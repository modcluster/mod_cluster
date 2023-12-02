/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.container;

/**
 * SPI for an engine, defined as collection of one or more hosts associated with a collection of {@link Connector}s. The only Connector
 * of significance is the one used to communicate with a proxy.
 *
 * @author Paul Ferraro
 */
public interface Engine {
    /**
     * The name of this engine.
     *
     * @return the engine name
     */
    String getName();

    /**
     * The server to which this engine is associated.
     *
     * @return a server
     */
    Server getServer();

    /**
     * The hosts associated with this engine.
     *
     * @return the engine's hosts
     */
    Iterable<Host> getHosts();

    /**
     * The connector which this engine uses to communicate with its proxies.
     *
     * @return the connector used by mod_cluster
     */
    Connector getProxyConnector();

    /**
     * Iteration of all connectors associated with this engine that can be used to communicate with its proxies.
     *
     * @return iteration of all connectors associated with this engine
     */
    Iterable<Connector> getConnectors();

    /**
     * The jvm route of this servlet engine. This uniquely identifies this node within the proxy.
     *
     * @return the servlet engine's jvm route
     */
    String getJvmRoute();

    /**
     * Set this jvm route for this servlet engine. Used to create a reasonable default value, if no explicit route is defined.
     *
     * @param jvmRoute a unique jvm route
     */
    void setJvmRoute(String jvmRoute);

    /**
     * Returns the host identified by the specified host name.
     *
     * @param name the host name
     * @return a servlet engine host
     */
    Host findHost(String name);

    /**
     * Returns the cookie name used for sessions.
     *
     * @return a cookie name
     */
    String getSessionCookieName();

    /**
     * Returns the url parameter name used for sessions.
     *
     * @return a parameter name
     */
    String getSessionParameterName();

    /**
     * Returns the default host of this engine.
     *
     * @return the default host
     */
    String getDefaultHost();
}
