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
package org.jboss.modcluster;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jboss.modcluster.mcmp.MCMPHandler;
import org.jboss.modcluster.mcmp.MCMPRequestType;

/**
 * @author Paul Ferraro
 */
public interface ModClusterServiceMBean {
    /**
     * Add a proxy to the list of those with which this handler communicates. Communication does not begin until the next call
     * to {@link MCMPHandler#status()}.
     *
     * @param host the hostname of the proxy; a string suitable for passing to <code>InetAddress.getByHost(...)</code>
     * @param port the port on which the proxy listens for MCMP requests
     */
    void addProxy(String host, int port);

    /**
     * Remove a proxy from the list of those with which this handler communicates. Communication does not end until the next
     * call to {@link MCMPHandler#status()}.
     *
     * @param host the hostname of the proxy; a string suitable for passing to <code>InetAddress.getByHost(...)</code>
     * @param port the port on which the proxy listens for MCMP requests
     */
    void removeProxy(String host, int port);

    /**
     * Retrieves the full proxy configuration.
     *
     * response: node: [1:1] JVMRoute: node1 Domain: [bla] Host: 127.0.0.1 Port: 8009 Type: ajp host: 1 [] vhost: 1 node: 1
     * context: 1 [/] vhost: 1 node: 1 status: 1 context: 2 [/myapp] vhost: 1 node: 1 status: 1 context: 3 [/host-manager]
     * vhost: 1 node: 1 status: 1 context: 4 [/docs] vhost: 1 node: 1 status: 1 context: 5 [/manager] vhost: 1 node: 1 status: 1
     *
     * Sends a {@link MCMPRequestType#DUMP DUMP} request to all proxies, returning the responses grouped by proxy address.
     *
     * @return a map of DUMP_RSP responses, grouped by proxy
     */
    Map<InetSocketAddress, String> getProxyConfiguration();

    /**
     * Retrieves the full proxy info message.
     *
     * Sends an {@link MCMPRequestType#INFO INFO} request to all proxies, returning the responses grouped by proxy address.
     *
     * @return a map of INFO_RSP responses, grouped by proxy
     */
    Map<InetSocketAddress, String> getProxyInfo();

    /**
     * Ping httpd. determines whether each proxy is accessible and healthy. returning the PING_RSP grouped by proxy address.
     *
     * @return a map of PING_RSP responses, grouped by proxy
     */
    Map<InetSocketAddress, String> ping();

    /**
     * Ping a node from httpd. returning the PING_RSP grouped by proxy address. determines whether the node configured with the
     * specified jvm route is accessible from each proxy returning the PING_RSP grouped by proxy address.
     *
     * @param jvmRoute a jvm route.
     * @return a map of PING_RSP responses, grouped by proxy
     */
    Map<InetSocketAddress, String> ping(String jvmRoute);

    /**
     * Ping a node defined protocol, host and port from httpd. determines whether a node (not necessarily configured) with the
     * matching connector is accessible from each proxy
     *
     * @param scheme ajp, http or https
     * @param hostname name or IP of a the host
     * @param port
     * @return a map of PING_RSP responses, grouped by proxy
     */
    Map<InetSocketAddress, String> ping(String scheme, String hostname, int port);

    /**
     * Reset a DOWN connection to the proxy up to ERROR, where the configuration will be refreshed.
     */
    void reset();

    /**
     * Refresh configuration.
     */
    void refresh();

    /**
     * Disable all webapps for all engines.
     *
     * @return true, if all proxies are responding normally, false otherwise
     */
    boolean disable();

    /**
     * Enable all webapps for all engines.
     *
     * @return true, if all proxies are responding normally, false otherwise
     */
    boolean enable();

    /**
     * Attempts to gracefully stops all web applications, within the specified timeout.
     * <ol>
     * <li>Disables all contexts</li>
     * <li>Waits for all sessions to drain</li>
     * <li>Stops all contexts</li>
     * </ol>
     *
     * @param timeout number of units of time for which to wait for sessions to drain. Negative or zero timeout value will wait
     *        forever.
     * @param unit unit of time represented in timeout parameter
     * @return true, if all contexts stopped successfully, false if sessions fail to drain before specified timeout.
     */
    boolean stop(long timeout, TimeUnit unit);

    /**
     * Disables the webapp with the specified host and context path.
     *
     * @param hostName host name of the target webapp
     * @param contextPath context path of the target webapp
     * @return true, if all proxies are responding normally, false otherwise
     */
    boolean disableContext(String hostName, String contextPath);

    /**
     * Enables the webapp with the specified host and context path.
     *
     * @param hostName host name of the target webapp
     * @param contextPath context path of the target webapp
     * @return true, if all proxies are responding normally, false otherwise
     */
    boolean enableContext(String hostName, String contextPath);

    /**
     * Attempts to gracefully stops a single web application, within the specified timeout.
     * <ol>
     * <li>Disables the specified context</li>
     * <li>Waits for all sessions for the specified context to drain</li>
     * <li>Stops the specified context</li>
     * </ol>
     *
     * @param timeout number of units of time for which to wait for sessions to drain. Negative or zero timeout value will wait
     *        forever.
     * @param unit unit of time represented in timeout parameter
     * @return true, if the specified context was stopped successfully, false if sessions fail to drain before specified
     *         timeout.
     */
    boolean stopContext(String hostName, String contextPath, long timeout, TimeUnit unit);
}
