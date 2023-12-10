/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.config.impl;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.jboss.modcluster.config.JvmRouteFactory;
import org.jboss.modcluster.config.MCMPHandlerConfiguration;
import org.jboss.modcluster.config.ProxyConfiguration;
import org.jboss.modcluster.config.SessionDrainingStrategy;

/**
 * @author Radoslav Husar
 * @since 1.3.6.Final
 */
public class MCMPHandlerConfigurationImpl implements MCMPHandlerConfiguration {

    private final Collection<ProxyConfiguration> proxyConfigurations;
    private final String proxyURL;
    private final int socketTimeout;
    private final SocketFactory socketFactory;
    private final Map<String, Set<String>> excludedContextsPerHost;
    private final Boolean advertise;
    private final boolean autoEnableContexts;
    private final long stopContextTimeout;
    private final TimeUnit stopContextTimeoutUnit;
    private final JvmRouteFactory jvmRouteFactory;
    private final SessionDrainingStrategy sessionDrainingStrategy;

    public MCMPHandlerConfigurationImpl(Collection<ProxyConfiguration> proxyConfigurations, String proxyURL, int socketTimeout, SocketFactory socketFactory, Map<String, Set<String>> excludedContextsPerHost, Boolean advertise, boolean autoEnableContexts, long stopContextTimeout, TimeUnit stopContextTimeoutUnit, JvmRouteFactory jvmRouteFactory, SessionDrainingStrategy sessionDrainingStrategy) {
        this.proxyConfigurations = proxyConfigurations;
        this.proxyURL = proxyURL;
        this.socketTimeout = socketTimeout;
        this.socketFactory = socketFactory;
        this.excludedContextsPerHost = excludedContextsPerHost;
        this.advertise = advertise;
        this.autoEnableContexts = autoEnableContexts;
        this.stopContextTimeout = stopContextTimeout;
        this.stopContextTimeoutUnit = stopContextTimeoutUnit;
        this.jvmRouteFactory = jvmRouteFactory;
        this.sessionDrainingStrategy = sessionDrainingStrategy;
    }

    @Override
    public Collection<ProxyConfiguration> getProxyConfigurations() {
        return proxyConfigurations;
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

    @Override
    public String getProxyURL() {
        return proxyURL;
    }

    @Override
    public int getSocketTimeout() {
        return socketTimeout;
    }

    @Override
    @Deprecated
    public boolean isSsl() {
        return (socketFactory instanceof SSLSocketFactory);
    }

    @Override
    public SocketFactory getSocketFactory() {
        return socketFactory;
    }

    @Override
    public Map<String, Set<String>> getExcludedContextsPerHost() {
        return excludedContextsPerHost;
    }

    @Override
    public Boolean getAdvertise() {
        return advertise;
    }

    @Override
    public boolean isAutoEnableContexts() {
        return autoEnableContexts;
    }

    @Override
    public long getStopContextTimeout() {
        return stopContextTimeout;
    }

    @Override
    public TimeUnit getStopContextTimeoutUnit() {
        return stopContextTimeoutUnit;
    }

    @Override
    public JvmRouteFactory getJvmRouteFactory() {
        return jvmRouteFactory;
    }

    @Override
    public SessionDrainingStrategy getSessionDrainingStrategy() {
        return sessionDrainingStrategy;
    }
}
