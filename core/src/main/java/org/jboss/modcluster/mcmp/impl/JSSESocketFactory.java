/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.jboss.modcluster.mcmp.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.cert.CRL;
import java.security.cert.CertPathParameters;
import java.security.cert.CertStore;
import java.security.cert.CertStoreParameters;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.X509CertSelector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.net.SocketFactory;
import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;

import org.jboss.logging.Logger;
import org.jboss.modcluster.ModClusterMessages;
import org.jboss.modcluster.config.SSLConfiguration;

/*
 1. Make the JSSE's jars available, either as an installed
 extension (copy them into jre/lib/ext) or by adding
 them to the Tomcat classpath.
 2. keytool -genkey -alias tomcat -keyalg RSA
 Use "changeit" as password ( this is the default we use )
 */

/**
 * SSL server socket factory. It _requires_ a valid RSA key and JSSE.
 * 
 * @author Harish Prabandham
 * @author Costin Manolache
 * @author Stefan Freyr Stefansson
 * @author EKR -- renamed to JSSESocketFactory
 * @author Jan Luehe
 * @author Bill Barker
 * @author Radoslav Husar
 */
public class JSSESocketFactory extends SocketFactory {
    static Logger log = Logger.getLogger(JSSESocketFactory.class);

    private SSLSocketFactory socketFactory = null;
    private String[] enabledCiphers;
    private SSLConfiguration config = null;

    public JSSESocketFactory(SSLConfiguration config) {
        this.config = config;

        try {
            SSLContext context;
            if (config.getSslContext() != null) {
                // Use the context provided by integration code (e.g. WildFly Elytron)
                context = config.getSslContext();

                this.socketFactory = context.getSocketFactory();
                this.enabledCiphers = this.socketFactory.getSupportedCipherSuites();
            } else {
                // Create and init SSLContext
                context = SSLContext.getInstance(this.config.getSslProtocol());

                KeyManager[] keyManagers = this.getKeyManagers();
                TrustManager[] trustManagers = this.getTrustManagers();

                context.init(keyManagers, trustManagers, new SecureRandom());

                // create proxy
                this.socketFactory = context.getSocketFactory();

                String ciphers = this.config.getSslCiphers();

                this.enabledCiphers = (ciphers != null) ? getEnabled(ciphers, this.socketFactory.getSupportedCipherSuites())
                        : this.socketFactory.getDefaultCipherSuites();
            }
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Socket createSocket() throws IOException {
        Socket socket = this.socketFactory.createSocket();
        this.initSocket(socket);
        return socket;
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        Socket socket = this.socketFactory.createSocket(host, port);
        this.initSocket(socket);
        return socket;
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        Socket socket = this.socketFactory.createSocket(address, port, localAddress, localPort);
        this.initSocket(socket);
        return socket;
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException {
        Socket socket = this.socketFactory.createSocket(host, port, localAddress, localPort);
        this.initSocket(socket);
        return socket;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        Socket socket = this.socketFactory.createSocket(host, port);
        this.initSocket(socket);
        return socket;
    }

    private static String[] getEnabled(String requested, String[] supported) {
        if (requested == null)
            return null;

        Set<String> supportedSet = new HashSet<String>(Arrays.asList(supported));

        String[] tokens = requested.split(",");
        List<String> enabled = new ArrayList<String>(tokens.length);

        for (String token : tokens) {
            token = token.trim();

            if (token.length() > 0) {
                if (supportedSet.contains(token)) {
                    enabled.add(token);
                }
            }
        }

        return !enabled.isEmpty() ? enabled.toArray(new String[enabled.size()]) : null;
    }

    /**
     * Gets the SSL server's keystore.
     */
    private KeyStore getKeystore() throws IOException, KeyStoreException, NoSuchProviderException, NoSuchAlgorithmException,
            CertificateException {
        return this.getStore(this.config.getSslKeyStoreType(), this.config.getSslKeyStoreProvider(),
                this.config.getSslKeyStore(), this.config.getSslKeyStorePassword());
    }

    /**
     * Gets the SSL server's truststore.
     */
    protected KeyStore getTrustStore() throws IOException, KeyStoreException, NoSuchProviderException,
            NoSuchAlgorithmException, CertificateException {
        String trustStore = this.config.getSslTrustStore();

        if (trustStore == null)
            return null;

        String truststorePassword = this.config.getSslTrustStorePassword();
        if (truststorePassword == null) {
            truststorePassword = this.config.getSslKeyStorePassword();
        } else if (truststorePassword.equals("")) {
            truststorePassword = null;
        }
        String truststoreType = this.config.getSslTrustStoreType();
        if (truststoreType == null) {
            truststoreType = this.config.getSslKeyStoreType();
        }
        String truststoreProvider = this.config.getSslTrustStoreProvider();
        if (truststoreProvider == null) {
            truststoreProvider = this.config.getSslKeyStoreProvider();
        }

        return this.getStore(truststoreType, truststoreProvider, trustStore, truststorePassword);
    }

    /**
     * Gets the key- or truststore with the specified type, path, and password.
     */
    private KeyStore getStore(String type, String provider, String path, String password) throws IOException,
            KeyStoreException, NoSuchProviderException, NoSuchAlgorithmException, CertificateException {
        InputStream istream = null;
        try {
            KeyStore ks = (provider == null) ? KeyStore.getInstance(type) : KeyStore.getInstance(type, provider);
            if (!("PKCS11".equalsIgnoreCase(type) || "".equals(path))) {
                File keyStoreFile = new File(path);
                if (!keyStoreFile.isAbsolute()) {
                    keyStoreFile = new File(System.getProperty("catalina.base"), path);
                }
                istream = new FileInputStream(keyStoreFile);
            }

            if (password == null) {
                ks.load(istream, null);
            } else {
                ks.load(istream, password.toCharArray());
            }
            return ks;
        } finally {
            if (istream != null) {
                try {
                    istream.close();
                } catch (IOException e) {
                    log.warn(e.getLocalizedMessage(), e);
                }
            }
        }
    }

    /**
     * Gets the initialized key managers.
     * 
     * @throws GeneralSecurityException
     * @throws IOException
     */
    protected KeyManager[] getKeyManagers() throws GeneralSecurityException, IOException {
        KeyStore ks = this.getKeystore();
        String alias = this.config.getSslKeyAlias();

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(this.config.getSslCertificateEncodingAlgorithm());
        kmf.init(ks, this.config.getSslKeyStorePassword().toCharArray());

        KeyManager[] kms = kmf.getKeyManagers();
        if (alias != null) {
            if ("JKS".equals(this.config.getSslKeyStoreType())) {
                alias = alias.toLowerCase();
            }
            for (int i = 0; i < kms.length; i++) {
                kms[i] = new JSSEKeyManager((X509KeyManager) kms[i], alias);
            }
        }

        return kms;
    }

    /**
     * Gets the initialized trust managers.
     * 
     * @throws GeneralSecurityException
     * @throws IOException
     */
    protected TrustManager[] getTrustManagers() throws GeneralSecurityException, IOException {
        KeyStore trustStore = this.getTrustStore();

        if (trustStore == null)
            return null;

        String algorithm = this.config.getSslTrustAlgorithm();

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);

        if (this.config.getSslCrlFile() == null) {
            tmf.init(trustStore);
            return tmf.getTrustManagers();
        }

        CertPathParameters params = this.getParameters(algorithm, this.config.getSslCrlFile(), trustStore);
        ManagerFactoryParameters mfp = new CertPathTrustManagerParameters(params);
        tmf.init(mfp);
        return tmf.getTrustManagers();
    }

    /**
     * Return the initialization parameters for the TrustManager. Currently, only the default <code>PKIX</code> is supported.
     * 
     * @param algorithm The algorithm to get parameters for.
     * @param crlf The path to the CRL file.
     * @param trustStore The configured TrustStore.
     * @return The parameters including the CRLs and TrustStore.
     * @throws GeneralSecurityException
     * @throws IOException
     */
    protected CertPathParameters getParameters(String algorithm, String crlf, KeyStore trustStore) throws GeneralSecurityException, IOException {
        if (!"PKIX".equalsIgnoreCase(algorithm)) {
            throw ModClusterMessages.MESSAGES.crlNotSupported(algorithm);
        }

        PKIXBuilderParameters params = new PKIXBuilderParameters(trustStore, new X509CertSelector());
        Collection<? extends CRL> crls = this.getCRLs(crlf);
        CertStoreParameters csp = new CollectionCertStoreParameters(crls);
        CertStore store = CertStore.getInstance("Collection", csp);
        params.addCertStore(store);
        params.setRevocationEnabled(true);
        params.setMaxPathLength(this.config.getSslTrustMaxCertLength());

        return params;
    }

    /**
     * Load the collection of CRLs.
     * 
     * @throws FileNotFoundException
     * @throws GeneralSecurityException
     */
    protected Collection<? extends CRL> getCRLs(String crlf) throws FileNotFoundException, GeneralSecurityException {
        File crlFile = new File(crlf);
        if (!crlFile.isAbsolute()) {
            crlFile = new File(System.getProperty("catalina.base"), crlf);
        }
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        InputStream is = new FileInputStream(crlFile);
        try {
            return cf.generateCRLs(is);
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                log.warn(e.getLocalizedMessage(), e);
            }
        }
    }

    /**
     * Configures the given SSL server socket with the requested cipher suites, protocol versions, and need for client
     * authentication
     */
    private void initSocket(Socket ssocket) {
        SSLSocket socket = (SSLSocket) ssocket;

        if (this.enabledCiphers != null) {
            socket.setEnabledCipherSuites(this.enabledCiphers);
        }

        String[] protocols = getEnabled(this.config.getSslProtocol(), socket.getSupportedProtocols());

        if (protocols != null) {
            socket.setEnabledProtocols(protocols);
        }
    }
}
