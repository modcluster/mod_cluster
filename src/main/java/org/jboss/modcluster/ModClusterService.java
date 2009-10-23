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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;
import org.jboss.modcluster.advertise.AdvertiseListener;
import org.jboss.modcluster.advertise.AdvertiseListenerFactory;
import org.jboss.modcluster.advertise.impl.AdvertiseListenerFactoryImpl;
import org.jboss.modcluster.config.BalancerConfiguration;
import org.jboss.modcluster.config.MCMPHandlerConfiguration;
import org.jboss.modcluster.config.ModClusterConfig;
import org.jboss.modcluster.config.NodeConfiguration;
import org.jboss.modcluster.load.LoadBalanceFactorProvider;
import org.jboss.modcluster.load.LoadBalanceFactorProviderFactory;
import org.jboss.modcluster.load.SimpleLoadBalanceFactorProviderFactory;
import org.jboss.modcluster.mcmp.MCMPHandler;
import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPRequestFactory;
import org.jboss.modcluster.mcmp.MCMPResponseParser;
import org.jboss.modcluster.mcmp.MCMPServerState;
import org.jboss.modcluster.mcmp.ResetRequestSource;
import org.jboss.modcluster.mcmp.impl.DefaultMCMPHandler;
import org.jboss.modcluster.mcmp.impl.DefaultMCMPRequestFactory;
import org.jboss.modcluster.mcmp.impl.DefaultMCMPResponseParser;
import org.jboss.modcluster.mcmp.impl.ResetRequestSourceImpl;

public class ModClusterService implements ModClusterServiceMBean, ContainerEventHandler, LoadBalanceFactorProvider
{
   protected final Logger log = Logger.getLogger(this.getClass());
   
   private final NodeConfiguration nodeConfig;
   private final BalancerConfiguration balancerConfig;
   private final MCMPHandlerConfiguration mcmpConfig;
   private final MCMPHandler mcmpHandler;
   private final ResetRequestSource resetRequestSource;
   private final MCMPRequestFactory requestFactory;
   private final AdvertiseListenerFactory listenerFactory;
   private final LoadBalanceFactorProviderFactory loadBalanceFactorProviderFactory;
   
   private volatile Server server = null;
   
   private volatile LoadBalanceFactorProvider loadBalanceFactorProvider;
   private volatile AdvertiseListener advertiseListener;
   private volatile Map<String, Set<String>> excludedContextPaths;

   public ModClusterService(ModClusterConfig config, LoadBalanceFactorProvider loadBalanceFactorProvider)
   {
      this(config, new SimpleLoadBalanceFactorProviderFactory(loadBalanceFactorProvider));
   }

   public ModClusterService(ModClusterConfig config, LoadBalanceFactorProviderFactory loadBalanceFactorProviderFactory)
   {
      this(config, loadBalanceFactorProviderFactory, new DefaultMCMPRequestFactory());
   }

   private ModClusterService(ModClusterConfig config, LoadBalanceFactorProviderFactory loadBalanceFactorProviderFactory, MCMPRequestFactory requestFactory)
   {
      this(config, loadBalanceFactorProviderFactory, requestFactory, new DefaultMCMPResponseParser(), new ResetRequestSourceImpl(config, config, requestFactory));
   }

   private ModClusterService(ModClusterConfig config, LoadBalanceFactorProviderFactory loadBalanceFactorProviderFactory, MCMPRequestFactory requestFactory, MCMPResponseParser responseParser, ResetRequestSource resetRequestSource)
   {
      this(config, config, config, loadBalanceFactorProviderFactory, requestFactory, responseParser, resetRequestSource, new DefaultMCMPHandler(config, resetRequestSource, requestFactory, responseParser), new AdvertiseListenerFactoryImpl());
   }
   
   protected ModClusterService(NodeConfiguration nodeConfig, BalancerConfiguration balancerConfig, MCMPHandlerConfiguration mcmpConfig,
         LoadBalanceFactorProviderFactory loadBalanceFactorProviderFactory, MCMPRequestFactory requestFactory, MCMPResponseParser responseParser,
         ResetRequestSource resetRequestSource, MCMPHandler mcmpHandler, AdvertiseListenerFactory listenerFactory)
   {
      this.nodeConfig = nodeConfig;
      this.balancerConfig = balancerConfig;
      this.mcmpConfig = mcmpConfig;
      this.mcmpHandler = mcmpHandler;
      this.resetRequestSource = resetRequestSource;
      this.requestFactory = requestFactory;
      this.loadBalanceFactorProviderFactory = loadBalanceFactorProviderFactory;
      this.listenerFactory = listenerFactory;
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ContainerEventHandler#init(java.lang.Object)
    */
   public synchronized void init(Server server)
   {
      this.log.info(Strings.SERVER_INIT.getString());
      
      this.server = server;
      
      List<InetSocketAddress> initialProxies = Utils.parseProxies(this.mcmpConfig.getProxyList());
      
      this.mcmpHandler.init(initialProxies);
      
      this.excludedContextPaths = Utils.parseContexts(this.mcmpConfig.getExcludedContexts());

      this.resetRequestSource.init(server, this.excludedContextPaths);
      
      this.loadBalanceFactorProvider = this.loadBalanceFactorProviderFactory.createLoadBalanceFactorProvider();
      
      Boolean advertise = this.mcmpConfig.getAdvertise();
      
      if (Boolean.TRUE.equals(advertise) || (advertise == null && initialProxies.isEmpty()))
      {
         try
         {
            this.advertiseListener = this.listenerFactory.createListener(this.mcmpHandler, this.mcmpConfig);
            
            this.advertiseListener.start();
         }
         catch (IOException e)
         {
            // TODO What now?
            this.log.error(Strings.ERROR_ADVERTISE_START.getString(), e);
         }
      }
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ContainerEventHandler#shutdown()
    */
   public synchronized void shutdown()
   {
      this.log.debug(Strings.SHUTDOWN.getString());
      
      this.server = null;
      
      if (this.advertiseListener != null)
      {
         this.advertiseListener.destroy();
         
         this.advertiseListener = null;
      }
      
      this.mcmpHandler.shutdown();
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ContainerEventHandler#start(java.lang.Object)
    */
   public void start(Server server)
   {
      this.checkInit();

      this.log.debug(Strings.SERVER_START.getString());
      
      for (Engine engine: server.getEngines())
      {
         this.config(engine);
         
         for (Host host: engine.getHosts())
         {
            for (Context context: host.getContexts())
            {
               this.add(context);
            }
         }
      }
   }

   /**
    * Send commands to the front end server associated with the shutdown of the
    * node.
    */
   public void stop(Server server)
   {
      this.checkInit();

      this.log.debug(Strings.SERVER_STOP.getString());
      
      for (Engine engine: server.getEngines())
      {
         for (Host host: engine.getHosts())
         {
            for (Context context: host.getContexts())
            {
               this.remove(context);
            }
         }
         
         this.removeAll(engine);
      }
   }
   
   protected void config(Engine engine)
   {
      log.debug(Strings.ENGINE_CONFIG.getString(engine));

      try
      {
         this.establishJvmRoute(engine);
         
         MCMPRequest request = this.requestFactory.createConfigRequest(engine, this.nodeConfig, this.balancerConfig);
         
         this.mcmpHandler.sendRequest(request);
      }
      catch (Exception e)
      {
         mcmpHandler.markProxiesInError();
         
         this.log.info(Strings.ERROR_ADDRESS_JVMROUTE.getString(), e);
      }
   }
   
   /*
    * If needed, create automagical JVM route (address + port + engineName)
    */
   protected void establishJvmRoute(Engine engine) throws Exception
   {
      if (engine.getJvmRoute() == null)
      {
         Connector connector = engine.getProxyConnector();
         
         String jvmRoute = Utils.identifyHost(connector.getAddress()) + ":" + connector.getPort() + ":" + engine.getName();
         
         engine.setJvmRoute(jvmRoute);
         
         this.log.info(Strings.DETECT_JVMROUTE.getString(engine, jvmRoute));
      }
   }

   public void add(Context context)
   {
      this.checkInit();

      if (!this.exclude(context))
      {
         // Send ENABLE-APP if state is started
         if (context.isStarted())
         {
            this.log.debug(Strings.CONTEXT_ENABLE.getString(context, context.getHost()));

            MCMPRequest request = this.requestFactory.createEnableRequest(context);
            
            this.mcmpHandler.sendRequest(request);
         }
      }
   }

   public void start(Context context)
   {
      this.checkInit();

      if (!this.exclude(context))
      {
         this.log.debug(Strings.CONTEXT_START.getString(context, context.getHost()));
   
         // Send ENABLE-APP
         MCMPRequest request = this.requestFactory.createEnableRequest(context);
         
         this.mcmpHandler.sendRequest(request);
      }
   }

   public void stop(Context context)
   {
      this.checkInit();

      if (!this.exclude(context))
      {
         this.log.debug(Strings.CONTEXT_STOP.getString(context, context.getHost()));
   
         // Send STOP-APP
         MCMPRequest request = this.requestFactory.createStopRequest(context);
         
         this.mcmpHandler.sendRequest(request);
      }
   }

   public void remove(Context context)
   {
      this.checkInit();

      if (!this.exclude(context))
      {
         Host host = context.getHost();
         
         // JVMRoute can be null here if nothing was ever initialized
         if (host.getEngine().getJvmRoute() != null)
         {
            this.log.debug(Strings.CONTEXT_DISABLE.getString(context, host));
            
            MCMPRequest request = this.requestFactory.createRemoveRequest(context);
            
            this.mcmpHandler.sendRequest(request);
         }
      }
   }

   protected void removeAll(Engine engine)
   {
      // JVMRoute can be null here if nothing was ever initialized
      if (engine.getJvmRoute() != null)
      {
         this.log.debug(Strings.ENGINE_STOP.getString(engine));

         // Send REMOVE-APP * request
         MCMPRequest request = this.requestFactory.createRemoveRequest(engine);
         
         this.mcmpHandler.sendRequest(request);
      }
   }

   public void status(Engine engine)
   {
      this.checkInit();

      this.log.debug(Strings.ENGINE_STATUS.getString(engine));

      this.mcmpHandler.status();

      // Send STATUS request
      this.mcmpHandler.sendRequest(this.requestFactory.createStatusRequest(engine.getJvmRoute(), this.getLoadBalanceFactor()));
   }
   
   @Override
   public int getLoadBalanceFactor()
   {
      return this.loadBalanceFactorProvider.getLoadBalanceFactor();
   }

   protected void checkInit()
   {
      if (this.server == null)
      {
         throw new IllegalStateException(Strings.ERROR_UNINITIALIZED.getString());
      }
   }
   
   private boolean exclude(Context context)
   {
      Set<String> excludedPaths = this.excludedContextPaths.get(context.getHost().getName());
      
      return (excludedPaths != null) ? excludedPaths.contains(context.getPath()) : false;
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#addProxy(java.lang.String, int)
    */
   public void addProxy(String host, int port)
   {
      this.mcmpHandler.addProxy(this.createSocketAddress(host, port));
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#removeProxy(java.lang.String, int)
    */
   public void removeProxy(String host, int port)
   {
      this.mcmpHandler.removeProxy(this.createSocketAddress(host, port));
   }

   private InetSocketAddress createSocketAddress(String host, int port)
   {
      try
      {
         return new InetSocketAddress(InetAddress.getByName(host), port);
      }
      catch (UnknownHostException e)
      {
         throw new IllegalArgumentException(e);
      }
   }
   
   public Map<InetSocketAddress, String> getProxyConfiguration()
   {
      // Send DUMP * request
      return this.getProxyResults(this.requestFactory.createDumpRequest());
   }
   
   /**
    * Retrieves the full proxy info message.
    *
    * @return the proxy info confguration
    */
   public Map<InetSocketAddress, String> getProxyInfo()
   {
      // Send INFO * request
      return this.getProxyResults(this.requestFactory.createInfoRequest());
   }

   /**
    * Do a PING using each proxy
    *
    * @return the proxy PING_RSP strings.
    */
   public Map<InetSocketAddress, String> ping(String jvmRoute)
   {
      // Send PING * request
      return this.getProxyResults(this.requestFactory.createPingRequest(jvmRoute));
   }
   
   private Map<InetSocketAddress, String> getProxyResults(MCMPRequest request)
   {
      Map<MCMPServerState, String> responses = this.mcmpHandler.sendRequest(request);

      if (responses.isEmpty()) return Collections.emptyMap();

      Map<InetSocketAddress, String> results = new HashMap<InetSocketAddress, String>();
      
      for (Map.Entry<MCMPServerState, String> response: responses.entrySet())
      {
         MCMPServerState state = response.getKey();
         
         results.put(state.getSocketAddress(), response.getValue());
      }
      
      return results;
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
      if (this.server == null)
      {
         throw new IllegalStateException(Strings.ERROR_UNINITIALIZED.getString());
      }
      
      for (Engine engine: this.server.getEngines())
      {
         // Send DISABLE-APP * request
         this.mcmpHandler.sendRequest(this.requestFactory.createDisableRequest(engine));
      }
      
      return this.mcmpHandler.isProxyHealthOK();
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#enable()
    */
   public boolean enable()
   {
      if (this.server == null)
      {
         throw new IllegalStateException(Strings.ERROR_UNINITIALIZED.getString());
      }
      
      for (Engine engine: this.server.getEngines())
      {
         // Send ENABLE-APP * request
         this.mcmpHandler.sendRequest(this.requestFactory.createEnableRequest(engine));
      }
      
      return this.mcmpHandler.isProxyHealthOK();
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#disable(java.lang.String, java.lang.String)
    */
   public boolean disable(String host, String path)
   {
      if (this.server == null)
      {
         throw new IllegalStateException(Strings.ERROR_UNINITIALIZED.getString());
      }
      
      Context context = this.findContext(this.findHost(host), path);
      
      // Send DISABLE-APP /... request
      this.mcmpHandler.sendRequest(this.requestFactory.createDisableRequest(context));
      
      return this.mcmpHandler.isProxyHealthOK();
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#enable(java.lang.String, java.lang.String)
    */
   public boolean enable(String host, String path)
   {
      if (this.server == null)
      {
         throw new IllegalStateException(Strings.ERROR_UNINITIALIZED.getString());
      }
      
      Context context = this.findContext(this.findHost(host), path);
      
      // Send ENABLE-APP /... request
      this.mcmpHandler.sendRequest(this.requestFactory.createEnableRequest(context));
      
      return this.mcmpHandler.isProxyHealthOK();
   }

   private Host findHost(String name)
   {
      for (Engine engine: this.server.getEngines())
      {
         Host host = engine.findHost(name);
         
         if (host != null) return host;
      }
      
      throw new IllegalArgumentException();
   }
   
   private Context findContext(Host host, String path)
   {
      Context context = host.findContext(path);
      
      if (context == null)
      {
         throw new IllegalArgumentException();
      }
      
      return context;
   }
}
