/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.modcluster.config;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.jboss.modcluster.load.impl.StaticLoadBalanceFactorProvider;

/**
 * Java bean implementing the various configuration interfaces.
 * 
 * @author Brian Stansberry
 */
public class ModClusterConfig
   extends StaticLoadBalanceFactorProvider
   implements BalancerConfiguration, MCMPHandlerConfiguration, NodeConfiguration, SSLConfiguration
{
   // ----------------------------------------------- MCMPHandlerConfiguration

   /**
    * Receive advertisements from httpd proxies (default is to use advertisements
    * if the proxyList is not set).
    */
   private Boolean advertise;
   public Boolean getAdvertise() { return this.advertise; }
   public void setAdvertise(Boolean advertise) { this.advertise = advertise; }


   /**
    * Advertise group.
    */
   private String advertiseGroupAddress = null;
   public String getAdvertiseGroupAddress() { return this.advertiseGroupAddress; }
   public void setAdvertiseGroupAddress(String advertiseGroupAddress) { this.advertiseGroupAddress = advertiseGroupAddress; }


   /**
    * Advertise port.
    */
   private int advertisePort = -1;
   public int getAdvertisePort() { return this.advertisePort; }
   public void setAdvertisePort(int advertisePort) { this.advertisePort = advertisePort; }


   /**
    * Advertise security key.
    */
   private String advertiseSecurityKey = null;
   public String getAdvertiseSecurityKey() { return this.advertiseSecurityKey; }
   public void setAdvertiseSecurityKey(String advertiseSecurityKey) { this.advertiseSecurityKey = advertiseSecurityKey; }


   /**
    * Proxy list, format "address:port,address:port".
    */
   private String proxyList = null;
   public String getProxyList() { return this.proxyList; }
   public void setProxyList(String proxyList) { this.proxyList = proxyList; }


   /**
    * URL prefix.
    */
   private String proxyURL = null;
   public String getProxyURL() { return this.proxyURL; }
   public void setProxyURL(String proxyURL) { this.proxyURL = proxyURL; }


   /**
    * Connection timeout for communication with the proxy.
    */
   private int socketTimeout = 20000;
   public int getSocketTimeout() { return this.socketTimeout; }
   public void setSocketTimeout(int socketTimeout) { this.socketTimeout = socketTimeout; }

   /**
    * Should clustered service use a singleton master per domain.
    */
   private boolean masterPerDomain = false;
   public boolean isMasterPerDomain() { return this.masterPerDomain; }
   public void setMasterPerDomain(boolean masterPerDomain) { this.masterPerDomain = masterPerDomain; }
   
   
   // -----------------------------------------------------  SSLConfiguration

   /**
    * SSL client cert usage to connect to the proxy.
    */
   private boolean ssl = false;
   public boolean isSsl() { return this.ssl; }
   public void setSsl(boolean ssl) { this.ssl = ssl; }
   
   
   /**
    * SSL ciphers.
    */
   private String sslCiphers = null;
   public String getSslCiphers() { return this.sslCiphers; }
   public void setSslCiphers(String sslCiphers) { this.sslCiphers = sslCiphers; }
   
   
   /**
    * SSL protocol.
    */
   private String sslProtocol = "TLS";
   public String getSslProtocol() { return this.sslProtocol; }
   public void setSslProtocol(String sslProtocol) { this.sslProtocol = sslProtocol; }
   
   
   /**
    * Certificate encoding algorithm.
    */
   private String sslCertificateEncodingAlgorithm = KeyManagerFactory.getDefaultAlgorithm();
   public String getSslCertificateEncodingAlgorithm() { return this.sslCertificateEncodingAlgorithm; }
   public void setSslCertificateEncodingAlgorithm(String sslCertificateEncodingAlgorithm) { this.sslCertificateEncodingAlgorithm = sslCertificateEncodingAlgorithm; }
   
   
   /**
    * SSL keystore.
    */
   private String sslKeyStore = System.getProperty("user.home") + "/.keystore";
   public String getSslKeyStore() { return this.sslKeyStore; }
   public void setSslKeyStore(String sslKeyStore) { this.sslKeyStore = sslKeyStore; }
   
   
   /**
    * SSL keystore password.
    */
   private String sslKeyStorePass = "changeit";
   public String getSslKeyStorePass() { return this.sslKeyStorePass; }
   public void setSslKeyStorePass(String sslKeyStorePass) { this.sslKeyStorePass = sslKeyStorePass; }
   
   
   /**
    * Keystore type.
    */
   private String sslKeyStoreType = "JKS";
   public String getSslKeyStoreType() { return this.sslKeyStoreType; }
   public void setSslKeyStoreType(String sslKeyStoreType) { this.sslKeyStoreType = sslKeyStoreType; }
   
   
   /**
    * Keystore provider.
    */
   private String sslKeyStoreProvider = null;
   public String getSslKeyStoreProvider() { return this.sslKeyStoreProvider; }
   public void setSslKeyStoreProvider(String sslKeyStoreProvider) { this.sslKeyStoreProvider = sslKeyStoreProvider; }
   
   
   /**
    * Truststore algorithm.
    */
   private String sslTrustAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
   public String getSslTrustAlgorithm() { return this.sslTrustAlgorithm; }
   public void setSslTrustAlgorithm(String sslTrustAlgorithm) { this.sslTrustAlgorithm = sslTrustAlgorithm; }
   
   
   /**
    * Key alias.
    */
   private String sslKeyAlias = null;
   public String getSslKeyAlias() { return this.sslKeyAlias; }
   public void setSslKeyAlias(String sslKeyAlias) { this.sslKeyAlias = sslKeyAlias; }
   
   
   /**
    * Certificate revocation list.
    */
   private String sslCrlFile = null;
   public String getSslCrlFile() { return this.sslCrlFile; }
   public void setSslCrlFile(String sslCrlFile) { this.sslCrlFile = sslCrlFile; }
   
   
   /**
    * Trust max certificate length.
    */
   private int sslTrustMaxCertLength = 5;
   public int getSslTrustMaxCertLength() { return this.sslTrustMaxCertLength; }
   public void setSslTrustMaxCertLength(int sslTrustMaxCertLength) { this.sslTrustMaxCertLength = sslTrustMaxCertLength; }
   
   
   /**
    * Trust store file.
    */
   private String sslTrustStore = System.getProperty("javax.net.ssl.trustStore");
   public String getSslTrustStore() { return this.sslTrustStore; }
   public void setSslTrustStore(String sslTrustStore) { this.sslTrustStore = sslTrustStore; }
   
   
   /**
    * Trust store password.
    */
   private String sslTrustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword");
   public String getSslTrustStorePassword() { return this.sslTrustStorePassword; }
   public void setSslTrustStorePassword(String sslTrustStorePassword) { this.sslTrustStorePassword = sslTrustStorePassword; }
   
   
   /**
    * Trust store type.
    */
   private String sslTrustStoreType = System.getProperty("javax.net.ssl.trustStoreType");
   public String getSslTrustStoreType() { return this.sslTrustStoreType; }
   public void setSslTrustStoreType(String sslTrustStoreType) { this.sslTrustStoreType = sslTrustStoreType; }
   
   
   /**
    * Trust store provider.
    */
   private String sslTrustStoreProvider = System.getProperty("javax.net.ssl.trustStoreProvider");
   public String getSslTrustStoreProvider() { return this.sslTrustStoreProvider; }
   public void setSslTrustStoreProvider(String sslTrustStoreProvider) { this.sslTrustStoreProvider = sslTrustStoreProvider; }
   

   // -----------------------------------------------------  NodeConfiguration


   /**
    * Domain parameter, which allows tying a jvmRoute to a particular domain.
    */
   private String domain = null;
   public String getDomain() { return this.domain; }
   public void setDomain(String domain) { this.domain = domain; }


   /**
    * Allows controlling flushing of packets.
    */
   private boolean flushPackets = false;
   public boolean getFlushPackets() { return this.flushPackets; }
   public void setFlushPackets(boolean flushPackets) { this.flushPackets = flushPackets; }


   /**
    * Time to wait before flushing packets.
    */
   private int flushWait = -1;
   public int getFlushWait() { return this.flushWait; }
   public void setFlushWait(int flushWait) { this.flushWait = flushWait; }


   /**
    * Time to wait for a pong answer to a ping.
    */
   private int ping = -1;
   public int getPing() { return this.ping; }
   public void setPing(int ping) { this.ping = ping; }


   /**
    * Soft maximum inactive connection count.
    */
   private int smax = -1;
   public int getSmax() { return this.smax; }
   public void setSmax(int smax) { this.smax = smax; }


   /**
    * Maximum time on seconds for idle connections above smax.
    */
   private int ttl = -1;
   public int getTtl() { return this.ttl; }
   public void setTtl(int ttl) { this.ttl = ttl; }


   /**
    * Maximum time on seconds for idle connections the proxy will wait to connect to the node.
    */
   private int nodeTimeout = -1;
   public int getNodeTimeout() { return this.nodeTimeout; }
   public void setNodeTimeout(int nodeTimeout) { this.nodeTimeout = nodeTimeout; }


   /**
    * Name of the balancer.
    */
   private String balancer = null;
   public String getBalancer() { return this.balancer; }
   public void setBalancer(String balancer) { this.balancer = balancer; }

   // -------------------------------------------------  BalancerConfiguration
   
   /**
    * Enables sticky sessions.
    */
   private boolean stickySession = true;
   public boolean getStickySession() { return this.stickySession; }
   public void setStickySession(boolean stickySession) { this.stickySession = stickySession; }


   /**
    * Remove session when the request cannot be routed to the right node.
    */
   private boolean stickySessionRemove = false;
   public boolean getStickySessionRemove() { return this.stickySessionRemove; }
   public void setStickySessionRemove(boolean stickySessionRemove) { this.stickySessionRemove = stickySessionRemove; }


   /**
    * Return an error when the request cannot be routed to the right node.
    */
   private boolean stickySessionForce = true;
   public boolean getStickySessionForce() { return this.stickySessionForce; }
   public void setStickySessionForce(boolean stickySessionForce) { this.stickySessionForce = stickySessionForce; }


   /**
    * Timeout to wait for an available worker (default is no wait).
    */
   private int workerTimeout = -1;
   public int getWorkerTimeout() { return this.workerTimeout; }
   public void setWorkerTimeout(int workerTimeout) { this.workerTimeout = workerTimeout; }


   /**
    * Maximum number of attempts to send the request to the backend server.
    */
   private int maxAttempts = -1;
   public int getMaxAttempts() { return this.maxAttempts; }
   public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
}
