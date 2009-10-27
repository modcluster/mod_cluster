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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles communication via MCMP with the httpd side.
 * 
 * @author Brian Stansberry
 * @version $Revision$
 */
public interface MCMPHandler
{  
   /** Initialize the handler with the given list of proxies */
   void init(List<InetSocketAddress> initialProxies);
   
   /** Perform any shut down work. */
   void shutdown();
   
   /** 
    * Send a request to all healthy proxies.
    * 
    * @param request the request. Cannot be <code>null</code>
    **/
   Map<MCMPServerState, String> sendRequest(MCMPRequest request);
   
   /** 
    * Send a list of requests to all healthy proxies, with all requests
    * in the list sent to each proxy before moving on to the next.
    * 
    * @param requests the requests. Cannot be <code>null</code>
    */
   Map<MCMPServerState, List<String>> sendRequests(List<MCMPRequest> requests);
   
   /**
    * Add a proxy to the list of those with which this handler communicates.
    * Communication does not begin until the next call to {@link #status()}.
    * <p>
    * Same as {@link #addProxy(InetAddress, int, boolean) addProxy(address, port, false}.
    * </p>
    * 
    * @param socketAddress InetSocketAddress on which the proxy listens for MCMP requests
    */
   void addProxy(InetSocketAddress socketAddress);
   
   /**
    * Add a proxy to the list of those with which this handler communicates.
    * Communication does not begin until the next call to {@link #status()}.
    * 
    * @param socketAddress InetSocketAddress on which the proxy listens for MCMP requests
    * @param established <code>true</code> if the proxy should be considered 
    *                    {@link MCMPServer#isEstablished() established},
    *                    <code>false</code> otherwise.
    */
   void addProxy(InetSocketAddress socketAddress, boolean established);
   
   /**
    * Remove a proxy from the list of those with which this handler communicates.
    * Communication does not end until the next call to {@link #status()}.
    * 
    * @param socketAddress InetSocketAddress on which the proxy listens for MCMP requests
    */
   void removeProxy(InetSocketAddress socketAddress);
   
   
   /**
    * Get the state of all proxies
    * 
    * @return a set of status objects indicating the status of this handler's
    *         communication with all proxies.
    */
   Set<MCMPServerState> getProxyStates();
   
   /**
    * Reset any proxies whose status is {@link MCMPServerState#DOWN DOWN} up to 
    * {@link MCMPServerState#ERROR ERROR}, where the configuration will
    * be refreshed.
    */
   void reset();
   
   /** 
    * Reset any proxies whose status is {@link MCMPServerState#OK OK} down to 
    * {@link MCMPServerState#ERROR ERROR}, which will trigger a refresh of 
    * their configuration.
    */
   void markProxiesInError();   
   
   /**
    * Convenience method that checks whether the status of all proxies is
    * {@link MCMPServerState#OK OK}.
    * 
    * @return <code>true</code> if all proxies are {@link MCMPServerState#OK OK},
    *         <code>false</code> otherwise
    */
   boolean isProxyHealthOK();
   
   /**
    * Perform periodic processing. Update the list of proxies to reflect any
    * calls to <code>addProxy(...)</code> or <code>removeProxy(...)</code>.
    * Attempt to establish communication with any proxies whose state is
    * {@link MCMPServerState#ERROR ERROR}. If successful and a 
    * {@link ResetRequestSource} has been provided, update the proxy with the 
    * list of requests provided by the source.
    * @return true, if load balance factor calculation should be performed, false if it should be skipped
    */
   void status();

   /**
    * Return interface on which mod_cluster will communicate with proxies.
    * @return a network interface address
    */
   InetAddress getLocalAddress() throws IOException;
}
