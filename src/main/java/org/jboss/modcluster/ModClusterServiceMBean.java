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

import org.jboss.modcluster.mcmp.MCMPRequestType;


/**
 * @author Paul Ferraro
 *
 */
public interface ModClusterServiceMBean
{
   /**
    * Add a proxy to the list of those with which this handler communicates.
    * Communication does not begin until the next call to {@link #status()}.
    * 
    * @param host the hostname of the proxy; a string suitable for passing to 
    *             <code>InetAddress.getByHost(...)</code> 
    * @param port the port on which the proxy listens for MCMP requests
    */
   void addProxy(String host, int port);
   
   /**
    * Remove a proxy from the list of those with which this handler communicates.
    * Communication does not end until the next call to {@link #status()}.
    * 
    * @param host the hostname of the proxy; a string suitable for passing to 
    *             <code>InetAddress.getByHost(...)</code> 
    * @param port the port on which the proxy listens for MCMP requests
    */
   void removeProxy(String host, int port);
   
   /**
    * Retrieves the full proxy configuration.
    * 
    *         response: HTTP/1.1 200 OK
    *   response:
    *   node: [1:1] JVMRoute: node1 Domain: [bla] Host: 127.0.0.1 Port: 8009 Type: ajp
    *   host: 1 [] vhost: 1 node: 1
    *   context: 1 [/] vhost: 1 node: 1 status: 1
    *   context: 2 [/myapp] vhost: 1 node: 1 status: 1
    *   context: 3 [/host-manager] vhost: 1 node: 1 status: 1
    *   context: 4 [/docs] vhost: 1 node: 1 status: 1
    *   context: 5 [/manager] vhost: 1 node: 1 status: 1
    *
    * Sends a {@link MCMPRequestType#DUMP DUMP} request to all proxies,
    * concatentating their responses into a single string.
    * 
    * @return the configuration information from all the accessible proxies.
    */
   public Map<InetSocketAddress, String> getProxyConfiguration();
   
   /**
    * Retrieves the full proxy info message.
    *
    * Sends a {@link MCMPRequestType#INFO INFO} request to all proxies,
    * concatentating their responses into a single string.
    * 
    * @return the configuration information from all the accessible proxies.
    */
   public Map<InetSocketAddress, String> getProxyInfo();
   
   /**
    * Ping a node from httpd.
    *
    * @return PING_RSP String.
    */
   public Map<InetSocketAddress, String> ping(String jvmRoute);

   /**
    * Reset a DOWN connection to the proxy up to ERROR, where the configuration will
    * be refreshed.
    */
   public void reset();

   /**
    * Refresh configuration.
    */
   public void refresh();

   /**
    * Disable all webapps for all engines.
    */
   public boolean disable();

   /**
    * Enable all webapps for all engines.
    */
   public boolean enable();
   
   /**
    * Disables the webapp with the specified host and context path.
    * @param hostName host name of the target webapp
    * @param contextPath context path of the target webapp
    */
   public boolean disable(String hostName, String contextPath);
   
   /**
    * Enables the webapp with the specified host and context path.
    * @param hostName host name of the target webapp
    * @param contextPath context path of the target webapp
    */
   public boolean enable(String hostName, String contextPath);
}
