/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.config;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.net.SocketFactory;

import org.jboss.modcluster.mcmp.MCMPHandler;

/**
 * Configuration object for an {@link MCMPHandler}.
 *
 * @author Brian Stansberry
 * @author Radoslav Husar
 */
public interface MCMPHandlerConfiguration {

    /**
     * Gets list of proxies as a collection of {@link ProxyConfiguration}s.
     *
     * @return list of proxies as a collection of {@link ProxyConfiguration}s
     */
    Collection<ProxyConfiguration> getProxyConfigurations();

    /**
     * Gets list of proxies as a collection of {@link InetSocketAddress}es.
     *
     * @return list of proxies as a collection of {@link InetSocketAddress}es
     * @deprecated As of 1.3.1.Final use {@link #getProxyConfigurations()} to also specify local bind address.
     */
    @Deprecated
    Collection<InetSocketAddress> getProxies();

    /**
     * URL prefix.
     */
    String getProxyURL();

    /**
     * Connection timeout for communication with the proxy.
     */
    int getSocketTimeout();

    /**
     * SSL client cert usage to connect to the proxy.
     *
     * @deprecated Use {@link MCMPHandlerConfiguration#getSocketFactory()} instead.
     */
    @Deprecated
    boolean isSsl();

    /**
     * Configuration of the socket factory, supply SSL socket factory to use SSL to connect to the proxy.
     */
    SocketFactory getSocketFactory();

    /**
     * Returns a list of contexts that should never be enabled in mod_cluster. Contexts may be
     *
     * @return a comma delimited list of contexts of the form "[host:]context"
     */
    Map<String, Set<String>> getExcludedContextsPerHost();

    /**
     * Receive advertisements from httpd proxies (default is to use advertisements if the proxyList is not set).
     */
    Boolean getAdvertise();

    /**
     * Indicates whether or not to automatically enable contexts. If false, context will need to be enabled manually.
     *
     * @return true, if contexts should auto-enable, false otherwise.
     */
    boolean isAutoEnableContexts();

    /**
     * Returns the number of {@link #getStopContextTimeoutUnit()} to wait for pending requests to complete when stopping a
     * context.
     *
     * @return timeout in seconds.
     */
    long getStopContextTimeout();

    /**
     * Returns the unit of time to which {@link #getStopContextTimeout()} pertains.
     *
     * @return a unit of time
     */
    TimeUnit getStopContextTimeoutUnit();

    /**
     * Factory for generating jvm route
     */
    JvmRouteFactory getJvmRouteFactory();

    SessionDrainingStrategy getSessionDrainingStrategy();
}
