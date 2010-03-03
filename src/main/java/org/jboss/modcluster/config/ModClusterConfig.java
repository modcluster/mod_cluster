/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Java bean implementing the various configuration interfaces.
 * 
 * @author Brian Stansberry
 */
public class ModClusterConfig
   implements BalancerConfiguration, MCMPHandlerConfiguration, NodeConfiguration, SSLConfiguration
{
   // ----------------------------------------------- MCMPHandlerConfiguration

   private Boolean advertise;
   public Boolean getAdvertise() { return this.advertise; }
   public void setAdvertise(Boolean advertise) { this.advertise = advertise; }

   private String advertiseGroupAddress = null;
   public String getAdvertiseGroupAddress() { return this.advertiseGroupAddress; }
   public void setAdvertiseGroupAddress(String advertiseGroupAddress) { this.advertiseGroupAddress = advertiseGroupAddress; }

   private int advertisePort = -1;
   public int getAdvertisePort() { return this.advertisePort; }
   public void setAdvertisePort(int advertisePort) { this.advertisePort = advertisePort; }

   private String advertiseInterface = null;
   public String getAdvertiseInterface() { return this.advertiseInterface; }
   public void setAdvertiseInterface(String advertiseInterface) { this.advertiseInterface = advertiseInterface; }
   
   private String advertiseSecurityKey = null;
   public String getAdvertiseSecurityKey() { return this.advertiseSecurityKey; }
   public void setAdvertiseSecurityKey(String advertiseSecurityKey) { this.advertiseSecurityKey = advertiseSecurityKey; }

   private ThreadFactory advertiseThreadFactory = Executors.defaultThreadFactory();
   public ThreadFactory getAdvertiseThreadFactory() { return this.advertiseThreadFactory; }
   public void setAdvertiseThreadFactory(ThreadFactory advertiseThreadFactory) { this.advertiseThreadFactory = advertiseThreadFactory; }
   
   private String proxyList = null;
   public String getProxyList() { return this.proxyList; }
   public void setProxyList(String proxyList) { this.proxyList = proxyList; }

   private String proxyURL = null;
   public String getProxyURL() { return this.proxyURL; }
   public void setProxyURL(String proxyURL) { this.proxyURL = proxyURL; }

   private int socketTimeout = 20000;
   public int getSocketTimeout() { return this.socketTimeout; }
   public void setSocketTimeout(int socketTimeout) { this.socketTimeout = socketTimeout; }

   private boolean ssl = false;
   public boolean isSsl() { return this.ssl; }
   public void setSsl(boolean ssl) { this.ssl = ssl; }

   private String excludedContexts = null;
   public String getExcludedContexts() { return this.excludedContexts; }
   public void setExcludedContexts(String excludedContexts) { this.excludedContexts = excludedContexts; }
   
   private boolean autoEnableContexts = true;
   public boolean isAutoEnableContexts() { return this.autoEnableContexts; }
   public void setAutoEnableContexts(boolean autoEnableContexts) { this.autoEnableContexts = autoEnableContexts; }
   
   // -----------------------------------------------------  SSLConfiguration
   
   private String sslCiphers = null;
   public String getSslCiphers() { return this.sslCiphers; }
   public void setSslCiphers(String sslCiphers) { this.sslCiphers = sslCiphers; }
   
   private String sslProtocol = "TLS";
   public String getSslProtocol() { return this.sslProtocol; }
   public void setSslProtocol(String sslProtocol) { this.sslProtocol = sslProtocol; }
   
   private String sslCertificateEncodingAlgorithm = KeyManagerFactory.getDefaultAlgorithm();
   public String getSslCertificateEncodingAlgorithm() { return this.sslCertificateEncodingAlgorithm; }
   public void setSslCertificateEncodingAlgorithm(String sslCertificateEncodingAlgorithm) { this.sslCertificateEncodingAlgorithm = sslCertificateEncodingAlgorithm; }
   
   private String sslKeyStore = System.getProperty("user.home") + "/.keystore";
   public String getSslKeyStore() { return this.sslKeyStore; }
   public void setSslKeyStore(String sslKeyStore) { this.sslKeyStore = sslKeyStore; }
   
   private String sslKeyStorePass = "changeit";
   public String getSslKeyStorePass() { return this.sslKeyStorePass; }
   public void setSslKeyStorePass(String sslKeyStorePass) { this.sslKeyStorePass = sslKeyStorePass; }
   
   private String sslKeyStoreType = "JKS";
   public String getSslKeyStoreType() { return this.sslKeyStoreType; }
   public void setSslKeyStoreType(String sslKeyStoreType) { this.sslKeyStoreType = sslKeyStoreType; }
   
   private String sslKeyStoreProvider = null;
   public String getSslKeyStoreProvider() { return this.sslKeyStoreProvider; }
   public void setSslKeyStoreProvider(String sslKeyStoreProvider) { this.sslKeyStoreProvider = sslKeyStoreProvider; }
   
   private String sslTrustAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
   public String getSslTrustAlgorithm() { return this.sslTrustAlgorithm; }
   public void setSslTrustAlgorithm(String sslTrustAlgorithm) { this.sslTrustAlgorithm = sslTrustAlgorithm; }
   
   private String sslKeyAlias = null;
   public String getSslKeyAlias() { return this.sslKeyAlias; }
   public void setSslKeyAlias(String sslKeyAlias) { this.sslKeyAlias = sslKeyAlias; }
   
   private String sslCrlFile = null;
   public String getSslCrlFile() { return this.sslCrlFile; }
   public void setSslCrlFile(String sslCrlFile) { this.sslCrlFile = sslCrlFile; }
   
   private int sslTrustMaxCertLength = 5;
   public int getSslTrustMaxCertLength() { return this.sslTrustMaxCertLength; }
   public void setSslTrustMaxCertLength(int sslTrustMaxCertLength) { this.sslTrustMaxCertLength = sslTrustMaxCertLength; }
   
   private String sslTrustStore = System.getProperty("javax.net.ssl.trustStore");
   public String getSslTrustStore() { return this.sslTrustStore; }
   public void setSslTrustStore(String sslTrustStore) { this.sslTrustStore = sslTrustStore; }
   
   private String sslTrustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword");
   public String getSslTrustStorePassword() { return this.sslTrustStorePassword; }
   public void setSslTrustStorePassword(String sslTrustStorePassword) { this.sslTrustStorePassword = sslTrustStorePassword; }
   
   private String sslTrustStoreType = System.getProperty("javax.net.ssl.trustStoreType");
   public String getSslTrustStoreType() { return this.sslTrustStoreType; }
   public void setSslTrustStoreType(String sslTrustStoreType) { this.sslTrustStoreType = sslTrustStoreType; }
   
   private String sslTrustStoreProvider = System.getProperty("javax.net.ssl.trustStoreProvider");
   public String getSslTrustStoreProvider() { return this.sslTrustStoreProvider; }
   public void setSslTrustStoreProvider(String sslTrustStoreProvider) { this.sslTrustStoreProvider = sslTrustStoreProvider; }
   

   // -----------------------------------------------------  NodeConfiguration

   private String domain = null;
   public String getDomain() { return this.domain; }
   public void setDomain(String domain) { this.domain = domain; }

   private boolean flushPackets = false;
   public boolean getFlushPackets() { return this.flushPackets; }
   public void setFlushPackets(boolean flushPackets) { this.flushPackets = flushPackets; }

   private int flushWait = -1;
   public int getFlushWait() { return this.flushWait; }
   public void setFlushWait(int flushWait) { this.flushWait = flushWait; }

   private int ping = -1;
   public int getPing() { return this.ping; }
   public void setPing(int ping) { this.ping = ping; }

   private int smax = -1;
   public int getSmax() { return this.smax; }
   public void setSmax(int smax) { this.smax = smax; }

   private int ttl = -1;
   public int getTtl() { return this.ttl; }
   public void setTtl(int ttl) { this.ttl = ttl; }

   private int nodeTimeout = -1;
   public int getNodeTimeout() { return this.nodeTimeout; }
   public void setNodeTimeout(int nodeTimeout) { this.nodeTimeout = nodeTimeout; }

   private String balancer = null;
   public String getBalancer() { return this.balancer; }
   public void setBalancer(String balancer) { this.balancer = balancer; }

   // -------------------------------------------------  BalancerConfiguration
   
   private boolean stickySession = true;
   public boolean getStickySession() { return this.stickySession; }
   public void setStickySession(boolean stickySession) { this.stickySession = stickySession; }

   private boolean stickySessionRemove = false;
   public boolean getStickySessionRemove() { return this.stickySessionRemove; }
   public void setStickySessionRemove(boolean stickySessionRemove) { this.stickySessionRemove = stickySessionRemove; }

   private boolean stickySessionForce = true;
   public boolean getStickySessionForce() { return this.stickySessionForce; }
   public void setStickySessionForce(boolean stickySessionForce) { this.stickySessionForce = stickySessionForce; }

   private int workerTimeout = -1;
   public int getWorkerTimeout() { return this.workerTimeout; }
   public void setWorkerTimeout(int workerTimeout) { this.workerTimeout = workerTimeout; }

   private int maxAttempts = -1;
   public int getMaxAttempts() { return this.maxAttempts; }
   public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
}
