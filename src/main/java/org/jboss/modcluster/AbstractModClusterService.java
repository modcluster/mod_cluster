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

import org.apache.catalina.Engine;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.ServerFactory;
import org.apache.catalina.Service;
import org.jboss.modcluster.config.ModClusterConfig;
import org.jboss.modcluster.load.LoadBalanceFactorProviderFactory;
import org.jboss.modcluster.mcmp.MCMPHandler;
import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPUtils;
import org.jboss.modcluster.mcmp.impl.DefaultMCMPHandler;
import org.jboss.modcluster.mcmp.impl.ResetRequestSourceImpl;

/**
 * Abstract non-clustered mod_cluster lifecycle listener
 * @author Paul Ferraro
 */
public abstract class AbstractModClusterService extends ModClusterConfig
   implements LifecycleListener, LoadBalanceFactorProviderFactory, ModClusterServiceMBean
{
   private final MCMPHandler mcmpHandler;
   private final LifecycleListener lifecycleListener;
   
   public AbstractModClusterService()
   {
      this.mcmpHandler = new DefaultMCMPHandler(this, new ResetRequestSourceImpl(this, this));
      
      JBossWebEventHandler eventHandler = new DefaultJBossWebEventHandler(this, this, this, this.mcmpHandler, this);
      
      this.lifecycleListener = new JBossWebEventHandlerAdapter(eventHandler);
   }

   protected AbstractModClusterService(MCMPHandler mcmpHandler, LifecycleListener lifecycleListener)
   {
      this.mcmpHandler = mcmpHandler;
      this.lifecycleListener = lifecycleListener;
   }
   
   /**
    * @{inheritDoc}
    * @see org.apache.catalina.LifecycleListener#lifecycleEvent(org.apache.catalina.LifecycleEvent)
    */
   public void lifecycleEvent(LifecycleEvent event)
   {
      this.lifecycleListener.lifecycleEvent(event);
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#addProxy(java.lang.String, int)
    */
   public void addProxy(String host, int port)
   {
      this.mcmpHandler.addProxy(host, port);
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#removeProxy(java.lang.String, int)
    */
   public void removeProxy(String host, int port)
   {
      this.mcmpHandler.removeProxy(host, port);
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#getProxyConfiguration()
    */
   public String getProxyConfiguration()
   {
      return this.mcmpHandler.getProxyConfiguration();
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#getProxyInfo()
    */
   public String getProxyInfo()
   {
      return this.mcmpHandler.getProxyInfo();
   }

   /**
    * Reset a DOWN connection to the proxy up to ERROR, where the configuration will
    * be refreshed. To be used through JMX or similar.
    */
   public void reset()
   {
      this.mcmpHandler.reset();
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#refresh()
    */
   public void refresh()
   {
      // Set as error, and the periodic event will refresh the configuration
      this.mcmpHandler.markProxiesInError();
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#disable()
    */
   public boolean disable()
   {
      for (Service service: ServerFactory.getServer().findServices())
      {
         Engine engine = (Engine) service.getContainer();
         // Send DISABLE-APP * request
         MCMPRequest request = MCMPUtils.createDisableEngineRequest(engine);
         this.mcmpHandler.sendRequest(request);
      }
      
      return this.mcmpHandler.isProxyHealthOK();
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#enable()
    */
   public boolean enable()
   {
      for (Service service: ServerFactory.getServer().findServices())
      {
         Engine engine = (Engine) service.getContainer();
         // Send ENABLE-APP * request
         MCMPRequest request = MCMPUtils.createEnableEngineRequest(engine);
         this.mcmpHandler.sendRequest(request);
      }
      
      return this.mcmpHandler.isProxyHealthOK();
   }
}