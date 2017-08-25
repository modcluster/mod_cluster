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

import java.util.Set;

import org.jboss.modcluster.config.BalancerConfiguration;
import org.jboss.modcluster.config.NodeConfiguration;
import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Engine;

/**
 * @author Paul Ferraro
 */
public interface MCMPRequestFactory {
    /**
     * Creates a CONFIG MCMP request for the specified engine.
     *
     * @param engine the servlet engine to be configured
     * @param nodeConfig the node configuration
     * @param balancerConfig the balancer configuration
     * @return an MCMP request
     */
    MCMPRequest createConfigRequest(Engine engine, NodeConfiguration nodeConfig, BalancerConfiguration balancerConfig);

    /**
     * Creates an ENABLE-APP MCMP request for the specified context.
     *
     * @param context a web application context
     * @return an MCMP request
     */
    MCMPRequest createEnableRequest(Context context);

    /**
     * Creates an DISABLE-APP MCMP request for the specified context.
     *
     * @param context a web application context
     * @return an MCMP request
     */
    MCMPRequest createDisableRequest(Context context);

    /**
     * Creates an STOP-APP MCMP request for the specified context.
     *
     * @param context a web application context
     * @return an MCMP request
     */
    MCMPRequest createStopRequest(Context context);

    /**
     * Creates an REMOVE-APP MCMP request for the specified context.
     *
     * @param context a web application context
     * @return an MCMP request
     */
    MCMPRequest createRemoveRequest(Context context);

    /**
     * Creates an STATUS MCMP request using the specified jvmRoute and load balance factor. Used to create a STATUS request for
     * a remote node.
     *
     * @param jvmRoute a configured jvm route
     * @param lbf a load factor
     * @return an MCMP request
     */
    MCMPRequest createStatusRequest(String jvmRoute, int lbf);

    /**
     * Creates an ENABLE-APP * MCMP request for the specified engine.
     *
     * @param engine a servlet engine
     * @return an MCMP request
     */
    MCMPRequest createEnableRequest(Engine engine);

    /**
     * Creates an DISABLE-APP * MCMP request for the specified engine.
     *
     * @param engine a servlet engine
     * @return an MCMP request
     */
    MCMPRequest createDisableRequest(Engine engine);

    /**
     * Creates an STOP-APP * MCMP request for the specified engine.
     *
     * @param engine a servlet engine
     * @return an MCMP request
     */
    MCMPRequest createStopRequest(Engine engine);

    /**
     * Creates an REMOVE-APP * MCMP request for the specified engine.
     *
     * @param engine a servlet engine
     * @return an MCMP request
     */
    MCMPRequest createRemoveRequest(Engine engine);

    /**
     * Creates an INFO MCMP request.
     *
     * @return an MCMP request
     */
    MCMPRequest createInfoRequest();

    /**
     * Creates an DUMP MCMP request.
     *
     * @return an MCMP request
     */
    MCMPRequest createDumpRequest();

    /**
     * Creates an PING MCMP request. This is used to ping a proxy,
     *
     * @return an MCMP request
     */
    MCMPRequest createPingRequest();

    /**
     * Creates an PING MCMP request for the node configured with the specified jvm route. This is used to ping a configured node
     * from a proxy.
     *
     * @param jvmRoute a jvm route of the target node
     * @return an MCMP request
     */
    MCMPRequest createPingRequest(String jvmRoute);

    /**
     * Creates an PING MCMP request for the node with a connector matching the specified protocol, host, and port. This is used
     * to ping an unconfigured node from a proxy.
     *
     * @param scheme specifies the protocol of the connector on the target node
     * @param host   specifies the host of the connector on the target node
     * @param port   specifies the port of the connector on the target node
     * @return an MCMP request
     */
    MCMPRequest createPingRequest(String scheme, String host, int port);

    /**
     * Create a REMOVE-APP request using the specified jvm route, aliases, and context path. Used to create REMOVE-APP requests
     * for remote nodes.
     *
     * @param jvmRoute a jvm route
     * @param aliases a set of host aliases
     * @param path the context path
     * @return an MCMP request
     */
    MCMPRequest createRemoveContextRequest(String jvmRoute, Set<String> aliases, String path);

    /**
     * Create a REMOVE-APP * request using the specified jvm route. Used to create REMOVE-APP * requests for remote nodes.
     *
     * @param jvmRoute a jvm route
     * @return an MCMP request
     */
    MCMPRequest createRemoveEngineRequest(String jvmRoute);
}
