/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
        List<InetSocketAddress> proxies = new LinkedList<InetSocketAddress>();
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
