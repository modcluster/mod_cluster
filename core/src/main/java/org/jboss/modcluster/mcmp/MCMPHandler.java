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
package org.jboss.modcluster.mcmp;

import org.jboss.modcluster.config.ProxyConfiguration;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles communication via MCMP with the httpd side.
 *
 * @author Brian Stansberry
 */
public interface MCMPHandler {

    /**
     * Initialization method for MCMP handler.
     *
     * @param initialProxies     a collection of initial {@link org.jboss.modcluster.config.ProxyConfiguration}s
     * @param connectionListener connection listener
     */
    void init(Collection<ProxyConfiguration> initialProxies, MCMPConnectionListener connectionListener);

    /** Perform any shut down work. */
    void shutdown();

    /**
     * Send a request to all healthy proxies.
     *
     * @param request the request. Cannot be <code>null</code>
     **/
    Map<MCMPServerState, String> sendRequest(MCMPRequest request);

    /**
     * Send a list of requests to all healthy proxies, with all requests in the list sent to each proxy before moving on to the
     * next.
     *
     * @param requests the requests. Cannot be <code>null</code>
     */
    Map<MCMPServerState, List<String>> sendRequests(List<MCMPRequest> requests);

    /**
     * Add a proxy to the list of those with which this handler communicates. Communication does not begin until the next call
     * to {@link #status()}.
     * <p>
     * Same as {@link #addProxy(InetAddress, int, boolean) addProxy(address, port, false}.
     * </p>
     *
     * @param socketAddress InetSocketAddress on which the proxy listens for MCMP requests
     * @deprecated See {@link #addProxy(org.jboss.modcluster.config.ProxyConfiguration)}
     */
    @Deprecated
    void addProxy(InetSocketAddress socketAddress);

    /**
     * Add a proxy to the list of those with which this handler communicates. Communication does not begin until the next call
     * to {@link #status()}.
     * <p>
     * Same as {@link #addProxy(ProxyConfiguration, boolean) addProxy(proxyConfiguration, false)}.
     * </p>
     * @param proxyConfiguration {@link ProxyConfiguration} defining address on which the proxy listens for MCMP requests
     *                           and optional local address to bind connections to
     */
    void addProxy(ProxyConfiguration proxyConfiguration);

    /**
     * Add a proxy to the list of those with which this handler communicates. Communication does not begin until the next call
     * to {@link #status()}.
     *
     * @param socketAddress InetSocketAddress on which the proxy listens for MCMP requests
     * @param established <code>true</code> if the proxy should be considered {@link MCMPServer#isEstablished() established},
     *        <code>false</code> otherwise.
     * @deprecated See {@link #addProxy(org.jboss.modcluster.config.ProxyConfiguration, boolean)}
     */
    @Deprecated
    void addProxy(InetSocketAddress socketAddress, boolean established);

    /**
     * Add a proxy to the list of those with which this handler communicates. Communication does not begin until the next call
     * to {@link #status()}.
     *
     * @param proxyConfiguration {@link ProxyConfiguration} defining address on which the proxy listens for MCMP requests
     *                           and optional local address to bind connections to
     * @param established        {@code true} if the proxy should be considered {@link MCMPServer#isEstablished() established},
     *                           {@code false} otherwise.
     */
    void addProxy(ProxyConfiguration proxyConfiguration, boolean established);

    /**
     * Remove a proxy from the list of those with which this handler communicates. Communication does not end until the next
     * call to {@link #status()}.
     *
     * @param socketAddress InetSocketAddress on which the proxy listens for MCMP requests
     */
    void removeProxy(InetSocketAddress socketAddress);

    /**
     * Get the state of all proxies
     *
     * @return a set of status objects indicating the status of this handler's communication with all proxies.
     */
    Set<MCMPServerState> getProxyStates();

    /**
     * Reset any proxies whose status is {@link MCMPServerState#DOWN DOWN} up to {@link MCMPServerState#ERROR ERROR}, where the
     * configuration will be refreshed.
     */
    void reset();

    /**
     * Reset any proxies whose status is {@link MCMPServerState#OK OK} down to {@link MCMPServerState#ERROR ERROR}, which will
     * trigger a refresh of their configuration.
     */
    void markProxiesInError();

    /**
     * Convenience method that checks whether the status of all proxies is {@link MCMPServerState#OK OK}.
     *
     * @return <code>true</code> if all proxies are {@link MCMPServerState#OK OK}, <code>false</code> otherwise
     */
    boolean isProxyHealthOK();

    /**
     * Perform periodic processing. Update the list of proxies to reflect any calls to <code>addProxy(...)</code> or
     * <code>removeProxy(...)</code>. Attempt to establish communication with any proxies whose state is
     * {@link MCMPServerState#ERROR ERROR}. If successful and a {@link ResetRequestSource} has been provided, update the proxy
     * with the list of requests provided by the source.
     *
     * @return true, if load balance factor calculation should be performed, false if it should be skipped
     */
    void status();
}
