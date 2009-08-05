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

import java.io.IOException;
import java.util.List;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.util.StringManager;
import org.jboss.logging.Logger;
import org.jboss.modcluster.advertise.AdvertiseListener;
import org.jboss.modcluster.advertise.AdvertiseListenerFactory;
import org.jboss.modcluster.advertise.impl.AdvertiseListenerFactoryImpl;
import org.jboss.modcluster.config.BalancerConfiguration;
import org.jboss.modcluster.config.MCMPHandlerConfiguration;
import org.jboss.modcluster.config.NodeConfiguration;
import org.jboss.modcluster.load.LoadBalanceFactorProvider;
import org.jboss.modcluster.mcmp.AddressPort;
import org.jboss.modcluster.mcmp.MCMPHandler;
import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPUtils;

/**
 * Default implementation of {@link JBossWebEventHandler}.
 * 
 * @author Brian Stansberry
 */
public class DefaultJBossWebEventHandler implements JBossWebEventHandler
{
   static Logger log = Logger.getLogger(DefaultJBossWebEventHandler.class);

   // -----------------------------------------------------------------  Fields

   /**
    * The string manager for this package.
    */
   private final StringManager sm = StringManager.getManager(Constants.Package);
   private final NodeConfiguration nodeConfig;
   private final BalancerConfiguration balancerConfig;
   private final MCMPHandlerConfiguration mcmpConfig;
   private final MCMPHandler mcmpHandler;
   private final AdvertiseListenerFactory listenerFactory;
   private final LoadBalanceFactorProvider loadBalanceFactorProvider;
   
   private volatile boolean init;

   private AdvertiseListener advertiseListener;

   // -----------------------------------------------------------  Constructors

   public DefaultJBossWebEventHandler(NodeConfiguration nodeConfig, BalancerConfiguration balancerConfig,
         MCMPHandlerConfiguration mcmpConfig, MCMPHandler mcmpHandler, LoadBalanceFactorProvider loadBalanceFactorProvider)
   {
      this(nodeConfig, balancerConfig, mcmpConfig, mcmpHandler, loadBalanceFactorProvider, new AdvertiseListenerFactoryImpl());
   }

   protected DefaultJBossWebEventHandler(NodeConfiguration nodeConfig, BalancerConfiguration balancerConfig,
         MCMPHandlerConfiguration mcmpConfig, MCMPHandler mcmpHandler, LoadBalanceFactorProvider loadBalanceFactorProvider, 
         AdvertiseListenerFactory listenerFactory)
   {
      this.nodeConfig = nodeConfig;
      this.balancerConfig = balancerConfig;
      this.mcmpConfig = mcmpConfig;
      this.mcmpHandler = mcmpHandler;
      this.loadBalanceFactorProvider = loadBalanceFactorProvider;
      this.listenerFactory = listenerFactory;
   }

   // ---------------------------------------------------- JBossWebEventHandler

   public synchronized void init()
   {
      List<AddressPort> initialProxies = MCMPUtils.parseProxies(this.mcmpConfig.getProxyList());
      this.mcmpHandler.init(initialProxies);
      
      Boolean advertise = this.mcmpConfig.getAdvertise();
      
      if (Boolean.TRUE.equals(advertise) || (advertise == null && initialProxies.isEmpty()))
      {
         this.advertiseListener = this.listenerFactory.createListener(this.mcmpHandler, this.mcmpConfig);
         
         try
         {
            this.advertiseListener.start();
         }
         catch (IOException e)
         {
            // TODO What now?
            log.error(e.getMessage(), e);
         }
      }

      this.init = true;
   }

   public synchronized void shutdown()
   {
      this.init = false;
      
      if (this.advertiseListener != null)
      {
         try
         {
            this.advertiseListener.destroy();
         }
         catch (IOException e)
         {
            log.error(this.sm.getString("modcluster.error.stopListener"), e);
         }
         
         this.advertiseListener = null;
      }
      
      this.mcmpHandler.shutdown();
   }

   /**
    * Send commands to the front end server associated with the startup of the
    * node.
    */
   public void startServer(Server server)
   {
      this.checkInit();

      for (Service service: server.findServices())
      {
         Engine engine = (Engine) service.getContainer();

         this.config(engine);
         
         for (Container host: engine.findChildren())
         {
            for (Container context: host.findChildren())
            {
               this.addContext((Context) context);
            }
         }
      }
   }

   /**
    * Send commands to the front end server associated with the shutdown of the
    * node.
    */
   public void stopServer(Server server)
   {
      this.checkInit();

      for (Service service: server.findServices())
      {
         Engine engine = (Engine) service.getContainer();
         
         this.removeAll(engine);
         
         for (Container host: engine.findChildren())
         {
            for (Container context: host.findChildren())
            {
               this.removeContext((Context) context);
            }
         }
      }
   }
   
   protected void config(Engine engine)
   {
      this.config(engine, this.mcmpHandler);
   }
   
   protected void config(Engine engine, MCMPHandler mcmpHandler)
   {
      log.debug(this.sm.getString("modcluster.engine.config", engine.getName()));

      // If needed, create automagical JVM route (address + port + engineName)
      try
      {
         Utils.establishJvmRouteAndConnectorAddress(engine, mcmpHandler);
         
         this.jvmRouteEstablished(engine);
         
         MCMPRequest request = MCMPUtils.createConfigRequest(engine, this.nodeConfig, this.balancerConfig);
         this.mcmpHandler.sendRequest(request);
      }
      catch (Exception e)
      {
         mcmpHandler.markProxiesInError();
         
         log.info(this.sm.getString("modcluster.error.addressJvmRoute"), e);
      }
   }
   
   protected void jvmRouteEstablished(Engine engine)
   {
      // Do nothing
   }

   public void addContext(Context context)
   {
      this.checkInit();

      log.debug(this.sm.getString("modcluster.context.enable", context.getPath(), context.getParent().getName()));

      // Send ENABLE-APP if state is started
      if (Utils.isContextStarted(context))
      {
         MCMPRequest request = MCMPUtils.createEnableAppRequest(context);
         this.mcmpHandler.sendRequest(request);
      }
   }

   public void startContext(Context context)
   {
      this.checkInit();

      log.debug(this.sm.getString("modcluster.context.start", context.getPath(), context.getParent().getName()));

      // Send ENABLE-APP
      MCMPRequest request = MCMPUtils.createEnableAppRequest(context);
      this.mcmpHandler.sendRequest(request);
   }

   public void stopContext(Context context)
   {
      this.checkInit();

      log.debug(this.sm.getString("modcluster.context.stop", context.getPath(), context.getParent().getName()));

      // Send STOP-APP
      MCMPRequest request = MCMPUtils.createStopAppRequest(context);
      this.mcmpHandler.sendRequest(request);
   }

   public void removeContext(Context context)
   {
      this.checkInit();

      log.debug(this.sm.getString("modcluster.context.disable", context.getPath(), context.getParent().getName()));

      // JVMRoute can be null here if nothing was ever initialized
      if (Utils.getJvmRoute(context) != null)
      {
         MCMPRequest request = MCMPUtils.createRemoveAppRequest(context);
         this.mcmpHandler.sendRequest(request);
      }
   }

   protected void removeAll(Engine engine)
   {
      log.debug(this.sm.getString("modcluster.engine.stop", engine.getName()));

      // JVMRoute can be null here if nothing was ever initialized
      if (engine.getJvmRoute() != null)
      {
         // Send REMOVE-APP * request
         MCMPRequest request = MCMPUtils.createRemoveAllRequest(engine);
         this.mcmpHandler.sendRequest(request);
      }
   }

   public void status(Engine engine)
   {
      this.checkInit();

      log.debug(this.sm.getString("modcluster.engine.status", engine.getName()));

      this.mcmpHandler.status();

      // Send STATUS request
      int lbf = this.getLoadBalanceFactor();
      MCMPRequest request = MCMPUtils.createStatusRequest(engine, lbf);
      this.mcmpHandler.sendRequest(request);
   }

   protected int getLoadBalanceFactor()
   {
      return this.loadBalanceFactorProvider.getLoadBalanceFactor();
   }
   
   // ----------------------------------------------------------------  Private

   private void checkInit()
   {
      if (!this.init)
      {
         throw new IllegalStateException(this.sm.getString("modcluster.error.uninitialized"));
      }
   }
}
