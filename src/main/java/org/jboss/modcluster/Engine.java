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

/**
 * SPI for an engine, defined as collection of one or more hosts associated with a collection of Connectors.
 * The only Connector of significance is the one used to communicate with a proxy.
 * 
 * @author Paul Ferraro
 */
public interface Engine
{
   /**
    * The name of this engine.
    * @return the engine name
    */
   String getName();
   
   /**
    * The server to which this engine is associated.
    * @return a server.
    */
   Server getServer();
   
   /**
    * The hosts associated with this engine.
    * @return the engine's hosts.
    */
   Iterable<Host> getHosts();
   
   /**
    * The connector to which this engine uses to communicate with its proxies.
    * @return the connector used by mod_cluster
    */
   Connector getProxyConnector();
   
   /**
    * The jvm route of this servlet engine.
    * This uniquely identifies this node within the proxy.
    * @return the servlet engine's jvm route
    */
   String getJvmRoute();
   
   /**
    * Set this jvm route for this servlet engine.
    * Used to create a reasonable default value, if no explicit route is defined.
    * @param jvmRoute a unique jvm route.
    */
   void setJvmRoute(String jvmRoute);
   
   /**
    * Returns the host identified by the specified host name.
    * @param name the host name
    * @return a servlet engine host
    */
   Host findHost(String name);
   
   /**
    * Returns the cookie name used for sessions.
    * @return a cookie name
    */
   String getSessionCookieName();
   
   /**
    * Returns the url parameter name used for sessions.
    * @return a parameter name
    */
   String getSessionParameterName();
}
