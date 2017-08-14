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
package org.jboss.modcluster.config.impl;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import javax.net.SocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.jboss.modcluster.config.AdvertiseConfiguration;
import org.jboss.modcluster.config.BalancerConfiguration;
import org.jboss.modcluster.config.JvmRouteFactory;
import org.jboss.modcluster.config.MCMPHandlerConfiguration;
import org.jboss.modcluster.config.NodeConfiguration;
import org.jboss.modcluster.config.ProxyConfiguration;
import org.jboss.modcluster.config.SSLConfiguration;
import org.jboss.modcluster.config.SessionDrainingStrategy;
import org.jboss.modcluster.mcmp.impl.JSSESocketFactory;

/**
 * Java bean implementing the various configuration interfaces.
 *
 * @author Brian Stansberry
 * @author Radoslav Husar
 */
public class ModClusterConfig implements BalancerConfiguration, MCMPHandlerConfiguration, NodeConfiguration, SSLConfiguration, AdvertiseConfiguration {
    // ----------------------------------------------- MCMPHandlerConfiguration

    private Boolean advertise;

    @Override
    public Boolean getAdvertise() {
        return this.advertise;
    }

    public void setAdvertise(Boolean advertise) {
        this.advertise = advertise;
    }

    private InetSocketAddress advertiseSocketAddress = DEFAULT_SOCKET_ADDRESS;

    @Override
    public InetSocketAddress getAdvertiseSocketAddress() {
        return this.advertiseSocketAddress;
    }

    public void setAdvertiseSocketAddress(InetSocketAddress address) {
        this.advertiseSocketAddress = address;
    }

    private InetAddress advertiseInterface = null;

    @Override
    public InetAddress getAdvertiseInterface() {
        return this.advertiseInterface;
    }

    public void setAdvertiseInterface(InetAddress advertiseInterface) {
        this.advertiseInterface = advertiseInterface;
    }

    private String advertiseSecurityKey = null;

    @Override
    public String getAdvertiseSecurityKey() {
        return this.advertiseSecurityKey;
    }

    public void setAdvertiseSecurityKey(String advertiseSecurityKey) {
        this.advertiseSecurityKey = advertiseSecurityKey;
    }

    private ThreadFactory advertiseThreadFactory = Executors.defaultThreadFactory();

    @Override
    public ThreadFactory getAdvertiseThreadFactory() {
        return this.advertiseThreadFactory;
    }

    public void setAdvertiseThreadFactory(ThreadFactory advertiseThreadFactory) {
        this.advertiseThreadFactory = advertiseThreadFactory;
    }

    private Collection<ProxyConfiguration> proxyConfigurations = Collections.emptySet();

    @Override
    public Collection<ProxyConfiguration> getProxyConfigurations() {
        return this.proxyConfigurations;
    }

    public void setProxyConfigurations(Collection<ProxyConfiguration> proxyConfigurations) {
        this.proxyConfigurations = proxyConfigurations;
    }

    @Override
    @Deprecated
    public Collection<InetSocketAddress> getProxies() {
        List<InetSocketAddress> proxies = new LinkedList<>();
        for (ProxyConfiguration proxy : proxyConfigurations) {
            proxies.add(proxy.getRemoteAddress());
        }
        return proxies;
    }

    /**
     * @deprecated Since 1.3.1.Final use {@link ModClusterConfig#setProxyConfigurations(java.util.Collection)} instead.
     */
    @Deprecated
    public void setProxies(Collection<InetSocketAddress> proxies) {
        Collection<ProxyConfiguration> proxyConfigs = new HashSet<>();
        for (final InetSocketAddress destination : proxies) {
            proxyConfigs.add(new ProxyConfiguration() {
                @Override
                public InetSocketAddress getRemoteAddress() {
                    return destination;
                }

                @Override
                public InetSocketAddress getLocalAddress() {
                    return null;
                }
            });
        }
        this.proxyConfigurations = proxyConfigs;
    }

    private String proxyURL = null;

    @Override
    public String getProxyURL() {
        return this.proxyURL;
    }

    public void setProxyURL(String proxyURL) {
        this.proxyURL = proxyURL;
    }

    private int socketTimeout = 20000;

    @Override
    public int getSocketTimeout() {
        return this.socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    private boolean ssl = false;

    @Deprecated
    @Override
    public boolean isSsl() {
        return this.ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    @Override
    public SocketFactory getSocketFactory() {
        if (ssl) {
            return new JSSESocketFactory(this);
        } else {
            return SocketFactory.getDefault();
        }
    }

    private Map<String, Set<String>> excludedContextsPerHost = Collections.emptyMap();

    @Override
    public Map<String, Set<String>> getExcludedContextsPerHost() {
        return this.excludedContextsPerHost;
    }

    public void setExcludedContextsPerHost(Map<String, Set<String>> excludedContexts) {
        this.excludedContextsPerHost = excludedContexts;
    }

    private boolean autoEnableContexts = true;

    @Override
    public boolean isAutoEnableContexts() {
        return this.autoEnableContexts;
    }

    public void setAutoEnableContexts(boolean autoEnableContexts) {
        this.autoEnableContexts = autoEnableContexts;
    }

    private long stopContextTimeout = 10;

    @Override
    public long getStopContextTimeout() {
        return this.stopContextTimeout;
    }

    public void setStopContextTimeout(int stopContextTimeout) {
        this.stopContextTimeout = stopContextTimeout;
    }

    private TimeUnit stopContextTimeoutUnit = TimeUnit.SECONDS;

    @Override
    public TimeUnit getStopContextTimeoutUnit() {
        return this.stopContextTimeoutUnit;
    }

    public void setStopContextTimeoutUnit(TimeUnit stopContextTimeoutUnit) {
        this.stopContextTimeoutUnit = stopContextTimeoutUnit;
    }

    private JvmRouteFactory jvmRouteFactory = new SystemPropertyJvmRouteFactory(new UUIDJvmRouteFactory());

    @Override
    public JvmRouteFactory getJvmRouteFactory() {
        return this.jvmRouteFactory;
    }

    public void setJvmRouteFactory(JvmRouteFactory jvmRouteFactory) {
        this.jvmRouteFactory = jvmRouteFactory;
    }

    private SessionDrainingStrategy sessionDrainingStrategy = SessionDrainingStrategyEnum.DEFAULT;

    @Override
    public SessionDrainingStrategy getSessionDrainingStrategy() {
        return this.sessionDrainingStrategy;
    }

    public void setSessionDrainingStrategy(SessionDrainingStrategy sessionDrainingStrategy) {
        this.sessionDrainingStrategy = sessionDrainingStrategy;
    }

    // ----------------------------------------------------- SSLConfiguration

    private String sslCiphers = null;

    @Override
    public String getSslCiphers() {
        return this.sslCiphers;
    }

    public void setSslCiphers(String sslCiphers) {
        this.sslCiphers = sslCiphers;
    }

    private String sslProtocol = "TLS";

    @Override
    public String getSslProtocol() {
        return this.sslProtocol;
    }

    public void setSslProtocol(String sslProtocol) {
        this.sslProtocol = sslProtocol;
    }

    private String sslCertificateEncodingAlgorithm = KeyManagerFactory.getDefaultAlgorithm();

    @Override
    public String getSslCertificateEncodingAlgorithm() {
        return this.sslCertificateEncodingAlgorithm;
    }

    public void setSslCertificateEncodingAlgorithm(String sslCertificateEncodingAlgorithm) {
        this.sslCertificateEncodingAlgorithm = sslCertificateEncodingAlgorithm;
    }

    private String sslKeyStore = System.getProperty("user.home") + "/.keystore";

    @Override
    public String getSslKeyStore() {
        return this.sslKeyStore;
    }

    public void setSslKeyStore(String sslKeyStore) {
        this.sslKeyStore = sslKeyStore;
    }

    private String sslKeyStorePassword = "changeit";

    @Override
    public String getSslKeyStorePassword() {
        return this.sslKeyStorePassword;
    }

    public void setSslKeyStorePassword(String sslKeyStorePassword) {
        this.sslKeyStorePassword = sslKeyStorePassword;
    }

    private String sslKeyStoreType = "JKS";

    @Override
    public String getSslKeyStoreType() {
        return this.sslKeyStoreType;
    }

    public void setSslKeyStoreType(String sslKeyStoreType) {
        this.sslKeyStoreType = sslKeyStoreType;
    }

    private String sslKeyStoreProvider = null;

    @Override
    public String getSslKeyStoreProvider() {
        return this.sslKeyStoreProvider;
    }

    public void setSslKeyStoreProvider(String sslKeyStoreProvider) {
        this.sslKeyStoreProvider = sslKeyStoreProvider;
    }

    private String sslTrustAlgorithm = TrustManagerFactory.getDefaultAlgorithm();

    @Override
    public String getSslTrustAlgorithm() {
        return this.sslTrustAlgorithm;
    }

    public void setSslTrustAlgorithm(String sslTrustAlgorithm) {
        this.sslTrustAlgorithm = sslTrustAlgorithm;
    }

    private String sslKeyAlias = null;

    @Override
    public String getSslKeyAlias() {
        return this.sslKeyAlias;
    }

    public void setSslKeyAlias(String sslKeyAlias) {
        this.sslKeyAlias = sslKeyAlias;
    }

    private String sslCrlFile = null;

    @Override
    public String getSslCrlFile() {
        return this.sslCrlFile;
    }

    public void setSslCrlFile(String sslCrlFile) {
        this.sslCrlFile = sslCrlFile;
    }

    private int sslTrustMaxCertLength = 5;

    @Override
    public int getSslTrustMaxCertLength() {
        return this.sslTrustMaxCertLength;
    }

    public void setSslTrustMaxCertLength(int sslTrustMaxCertLength) {
        this.sslTrustMaxCertLength = sslTrustMaxCertLength;
    }

    private String sslTrustStore = System.getProperty("javax.net.ssl.trustStore");

    @Override
    public String getSslTrustStore() {
        return this.sslTrustStore;
    }

    public void setSslTrustStore(String sslTrustStore) {
        this.sslTrustStore = sslTrustStore;
    }

    private String sslTrustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword");

    @Override
    public String getSslTrustStorePassword() {
        return this.sslTrustStorePassword;
    }

    public void setSslTrustStorePassword(String sslTrustStorePassword) {
        this.sslTrustStorePassword = sslTrustStorePassword;
    }

    private String sslTrustStoreType = System.getProperty("javax.net.ssl.trustStoreType");

    @Override
    public String getSslTrustStoreType() {
        return this.sslTrustStoreType;
    }

    public void setSslTrustStoreType(String sslTrustStoreType) {
        this.sslTrustStoreType = sslTrustStoreType;
    }

    private String sslTrustStoreProvider = System.getProperty("javax.net.ssl.trustStoreProvider");

    @Override
    public String getSslTrustStoreProvider() {
        return this.sslTrustStoreProvider;
    }

    public void setSslTrustStoreProvider(String sslTrustStoreProvider) {
        this.sslTrustStoreProvider = sslTrustStoreProvider;
    }

    // ----------------------------------------------------- NodeConfiguration
    private String loadBalancingGroup = null;

    @Override
    public String getLoadBalancingGroup() {
        return this.loadBalancingGroup;
    }

    public void setLoadBalancingGroup(String loadBalancingGroup) {
        this.loadBalancingGroup = loadBalancingGroup;
    }

    private boolean flushPackets = false;

    @Override
    public boolean getFlushPackets() {
        return this.flushPackets;
    }

    public void setFlushPackets(boolean flushPackets) {
        this.flushPackets = flushPackets;
    }

    private int flushWait = -1;

    @Override
    public int getFlushWait() {
        return this.flushWait;
    }

    public void setFlushWait(int flushWait) {
        this.flushWait = flushWait;
    }

    private int ping = -1;

    @Override
    public int getPing() {
        return this.ping;
    }

    public void setPing(int ping) {
        this.ping = ping;
    }

    private int smax = -1;

    @Override
    public int getSmax() {
        return this.smax;
    }

    public void setSmax(int smax) {
        this.smax = smax;
    }

    private int ttl = -1;

    @Override
    public int getTtl() {
        return this.ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    private int nodeTimeout = -1;

    @Override
    public int getNodeTimeout() {
        return this.nodeTimeout;
    }

    public void setNodeTimeout(int nodeTimeout) {
        this.nodeTimeout = nodeTimeout;
    }

    private String balancer = null;

    @Override
    public String getBalancer() {
        return this.balancer;
    }

    public void setBalancer(String balancer) {
        this.balancer = balancer;
    }

    // ------------------------------------------------- BalancerConfiguration

    private boolean stickySession = true;

    @Override
    public boolean getStickySession() {
        return this.stickySession;
    }

    public void setStickySession(boolean stickySession) {
        this.stickySession = stickySession;
    }

    private boolean stickySessionRemove = false;

    @Override
    public boolean getStickySessionRemove() {
        return this.stickySessionRemove;
    }

    public void setStickySessionRemove(boolean stickySessionRemove) {
        this.stickySessionRemove = stickySessionRemove;
    }

    private boolean stickySessionForce = false;

    @Override
    public boolean getStickySessionForce() {
        return this.stickySessionForce;
    }

    public void setStickySessionForce(boolean stickySessionForce) {
        this.stickySessionForce = stickySessionForce;
    }

    private int workerTimeout = -1;

    @Override
    public int getWorkerTimeout() {
        return this.workerTimeout;
    }

    public void setWorkerTimeout(int workerTimeout) {
        this.workerTimeout = workerTimeout;
    }

    private int maxAttempts = -1;

    @Override
    public int getMaxAttempts() {
        return this.maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }
}
