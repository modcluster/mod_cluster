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

import java.util.concurrent.TimeUnit;

import org.jboss.modcluster.JvmRouteFactory;

/**
 * Configuration object for an {@link MCMPHandler}.
 * 
 * @author Brian Stansberry
 *
 */
public interface MCMPHandlerConfiguration extends SSLConfiguration, AdvertiseConfiguration
{   
   /**
    * Proxy list, format "address:port,address:port".
    */
   String getProxyList();
   
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
    * Returns a list of contexts that should never be enabled in mod_cluster.
    * Contexts may be 
    * @return a comma delimited list of contexts of the form "[host:]context"
    */
   String getExcludedContexts();
   
   /**
    * Receive advertisements from httpd proxies (default is to use advertisements
    * if the proxyList is not set).
    */
   Boolean getAdvertise();
   
   /**
    * Indicates whether or not to automatically enable contexts.
    * If false, context will need to be enabled manually.
    * @return true, if contexts should auto-enable, false otherwise.
    */
   boolean isAutoEnableContexts();

   /**
    * Returns the number of seconds to wait for pending requests to complete when stopping a context.
    * @return timeout in seconds.
    */
   int getStopContextTimeout();
   
   TimeUnit getStopContextTimeoutUnit();
   
   JvmRouteFactory getJvmRouteFactory();
}
