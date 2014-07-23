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

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jboss.modcluster.mcmp.MCMPHandler;

/**
 * Configuration object for an {@link MCMPHandler}.
 *
 * @author Brian Stansberry
 */
public interface MCMPHandlerConfiguration extends SSLConfiguration, AdvertiseConfiguration {

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
     * @deprecated as of 1.3.1 use {@link #getProxyConfigurations()} to also specify local addresses to bind to
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
     */
    boolean isSsl();

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
