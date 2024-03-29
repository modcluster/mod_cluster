/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.mcmp.impl;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509KeyManager;

/**
 * X509KeyManager which allows selection of a specific keypair and certificate chain (identified by their keystore alias name)
 * to be used by the server to authenticate itself to SSL clients.
 *
 * @author Jan Luehe
 */
public final class JSSEKeyManager implements X509KeyManager {
    private X509KeyManager delegate;

    private String serverKeyAlias;

    /**
     * Constructor.
     *
     * @param mgr The X509KeyManager used as a delegate
     * @param serverKeyAlias The alias name of the server's keypair and supporting certificate chain
     */
    public JSSEKeyManager(X509KeyManager mgr, String serverKeyAlias) {
        this.delegate = mgr;
        this.serverKeyAlias = serverKeyAlias;
    }

    /**
     * Choose an alias to authenticate the client side of a secure socket, given the public key type and the list of certificate
     * issuer authorities recognized by the peer (if any).
     *
     * @param keyType The key algorithm type name(s), ordered with the most-preferred key type first
     * @param issuers The list of acceptable CA issuer subject names, or null if it does not matter which issuers are used
     * @param socket The socket to be used for this connection. This parameter can be null, in which case this method will
     *        return the most generic alias to use
     *
     * @return The alias name for the desired key, or null if there are no matches
     */
    @Override
    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
        return this.delegate.chooseClientAlias(keyType, issuers, socket);
    }

    /**
     * Returns this key manager's server key alias that was provided in the constructor.
     *
     * @param keyType The key algorithm type name (ignored)
     * @param issuers The list of acceptable CA issuer subject names, or null if it does not matter which issuers are used
     *        (ignored)
     * @param socket The socket to be used for this connection. This parameter can be null, in which case this method will
     *        return the most generic alias to use (ignored)
     *
     * @return Alias name for the desired key
     */
    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        return this.serverKeyAlias;
    }

    /**
     * Returns the certificate chain associated with the given alias.
     *
     * @param alias The alias name
     *
     * @return Certificate chain (ordered with the user's certificate first and the root certificate authority last), or null if
     *         the alias can't be found
     */
    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        return this.delegate.getCertificateChain(alias);
    }

    /**
     * Get the matching aliases for authenticating the client side of a secure socket, given the public key type and the list of
     * certificate issuer authorities recognized by the peer (if any).
     *
     * @param keyType The key algorithm type name
     * @param issuers The list of acceptable CA issuer subject names, or null if it does not matter which issuers are used
     *
     * @return Array of the matching alias names, or null if there were no matches
     */
    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return this.delegate.getClientAliases(keyType, issuers);
    }

    /**
     * Get the matching aliases for authenticating the server side of a secure socket, given the public key type and the list of
     * certificate issuer authorities recognized by the peer (if any).
     *
     * @param keyType The key algorithm type name
     * @param issuers The list of acceptable CA issuer subject names, or null if it does not matter which issuers are used
     *
     * @return Array of the matching alias names, or null if there were no matches
     */
    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return this.delegate.getServerAliases(keyType, issuers);
    }

    /**
     * Returns the key associated with the given alias.
     *
     * @param alias The alias name
     *
     * @return The requested key, or null if the alias can't be found
     */
    @Override
    public PrivateKey getPrivateKey(String alias) {
        return this.delegate.getPrivateKey(alias);
    }
}
