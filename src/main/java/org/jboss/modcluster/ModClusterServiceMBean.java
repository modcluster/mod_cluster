/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

import org.jboss.ha.framework.interfaces.HASingletonMBean;
import org.jboss.modcluster.mcmp.MCMPRequestType;
import org.jboss.modcluster.mcmp.MCMPServerState;

/**
 * StandardMBean interface for {@link ModClusterService}.
 * 
 * @author Brian Stansberry
 */
public interface ModClusterServiceMBean extends HASingletonMBean
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
    * Reset any proxies whose status is {@link MCMPServerState#DOWN DOWN} up to 
    * {@link MCMPServerState#ERROR ERROR}, where the configuration will
    * be refreshed.
    */
   void reset();
   
   /** 
    * FIXME. This is the same as markProxiesInError().
    * 
    * Reset any proxies whose status is {@link MCMPServerState#OK OK} down to 
    * {@link MCMPServerState#ERROR ERROR}, which will trigger a refresh of 
    * their configuration. To be used through JMX or similar.
    */
   void refresh();
   
   /**
    * Sends a {@link MCMPRequestType#DUMP DUMP} request to all proxies,
    * concatentating their responses into a single string.
    * 
    * TODO wouldn't a List<String> be better? Let the caller concatenate if
    * so desired.
    * 
    * @return the configuration information from all the accessible proxies.
    */
   String getProxyConfiguration();
}
