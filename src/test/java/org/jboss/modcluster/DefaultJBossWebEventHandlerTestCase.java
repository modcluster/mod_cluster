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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.jboss.modcluster.advertise.AdvertiseListener;
import org.jboss.modcluster.advertise.AdvertiseListenerFactory;
import org.jboss.modcluster.config.BalancerConfiguration;
import org.jboss.modcluster.config.MCMPHandlerConfiguration;
import org.jboss.modcluster.config.NodeConfiguration;
import org.jboss.modcluster.load.LoadBalanceFactorProvider;
import org.jboss.modcluster.load.LoadBalanceFactorProviderFactory;
import org.jboss.modcluster.mcmp.MCMPHandler;
import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPRequestFactory;
import org.jboss.modcluster.mcmp.MCMPServerState;
import org.jboss.modcluster.mcmp.ResetRequestSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
@SuppressWarnings("boxing")
public class DefaultJBossWebEventHandlerTestCase
{
   private final NodeConfiguration nodeConfig = EasyMock.createStrictMock(NodeConfiguration.class);
   private final BalancerConfiguration balancerConfig = EasyMock.createStrictMock(BalancerConfiguration.class);
   private final MCMPHandlerConfiguration mcmpConfig = EasyMock.createStrictMock(MCMPHandlerConfiguration.class);
   private final MCMPHandler mcmpHandler = EasyMock.createStrictMock(MCMPHandler.class);
   private final ResetRequestSource source = EasyMock.createStrictMock(ResetRequestSource.class);
   private final MCMPRequestFactory requestFactory = EasyMock.createStrictMock(MCMPRequestFactory.class);
   private final LoadBalanceFactorProviderFactory lbfProviderFactory = EasyMock.createStrictMock(LoadBalanceFactorProviderFactory.class);
   private final LoadBalanceFactorProvider lbfProvider = EasyMock.createStrictMock(LoadBalanceFactorProvider.class);
   private final AdvertiseListenerFactory listenerFactory = EasyMock.createStrictMock(AdvertiseListenerFactory.class);
   private AdvertiseListener listener = EasyMock.createStrictMock(AdvertiseListener.class);
   
   private ContainerEventHandler<Server, Engine, Context> handler;

   @Before
   public void construct() throws Exception
   {
      this.handler = new CatalinaEventHandler(this.nodeConfig, this.balancerConfig, this.mcmpConfig, this.mcmpHandler, this.source, this.requestFactory, this.lbfProviderFactory, this.listenerFactory);
   }
   
   @Test
   public void initNoAdvertise() throws IOException
   {
      Server server = EasyMock.createStrictMock(Server.class);
      InetAddress localAddress = InetAddress.getLocalHost();
      String localHostName = localAddress.getHostName();
      Map<String, Set<String>> emptyContextMap = Collections.emptyMap();
      
      // Test advertise = false
      EasyMock.expect(this.mcmpConfig.getProxyList()).andReturn(localHostName);
      
      this.mcmpHandler.init(Collections.singletonList(new InetSocketAddress(localAddress, 8000)));
      
      EasyMock.expect(this.mcmpConfig.getExcludedContexts()).andReturn(null);
      
      this.source.init(emptyContextMap);
      
      EasyMock.expect(this.lbfProviderFactory.createLoadBalanceFactorProvider()).andReturn(this.lbfProvider);
      
      EasyMock.expect(this.mcmpConfig.getAdvertise()).andReturn(Boolean.FALSE);
      
      EasyMock.replay(this.mcmpHandler, this.mcmpConfig, this.lbfProviderFactory, this.source);
      
      this.handler.init(server);
      
      EasyMock.verify(this.mcmpHandler, this.mcmpConfig, this.lbfProviderFactory, this.source);
      EasyMock.reset(this.mcmpHandler, this.mcmpConfig, this.lbfProviderFactory, this.source);
   }
   
   @Test
   public void initAdvertise() throws IOException
   {
      Server server = EasyMock.createStrictMock(Server.class);
      InetAddress localAddress = InetAddress.getLocalHost();
      String localHostName = localAddress.getHostName();
      Map<String, Set<String>> emptyContextMap = Collections.emptyMap();
      
      // Test advertise = true
      EasyMock.expect(this.mcmpConfig.getProxyList()).andReturn(localHostName);
      
      this.mcmpHandler.init(Collections.singletonList(new InetSocketAddress(localAddress, 8000)));
      
      EasyMock.expect(this.mcmpConfig.getExcludedContexts()).andReturn("");
      
      this.source.init(emptyContextMap);
      
      EasyMock.expect(this.lbfProviderFactory.createLoadBalanceFactorProvider()).andReturn(this.lbfProvider);
      
      EasyMock.expect(this.mcmpConfig.getAdvertise()).andReturn(Boolean.TRUE);
      
      EasyMock.expect(this.listenerFactory.createListener(this.mcmpHandler, this.mcmpConfig)).andReturn(this.listener);
      
      this.listener.start();
      
      EasyMock.replay(this.mcmpHandler, this.listenerFactory, this.mcmpConfig, this.lbfProviderFactory, this.listener, this.source);
      
      this.handler.init(server);
      
      EasyMock.verify(this.mcmpHandler, this.listenerFactory, this.mcmpConfig, this.lbfProviderFactory, this.listener, this.source);
      EasyMock.reset(this.mcmpHandler, this.listenerFactory, this.mcmpConfig, this.lbfProviderFactory, this.listener, this.source);
   }
   
   @Test
   public void init() throws IOException
   {
      Server server = EasyMock.createStrictMock(Server.class);
      InetAddress localAddress = InetAddress.getLocalHost();
      String localHostName = localAddress.getHostName();
      
      // Test advertise = null, proxies configured
      EasyMock.expect(this.mcmpConfig.getProxyList()).andReturn(localHostName);
      
      this.mcmpHandler.init(Collections.singletonList(new InetSocketAddress(localAddress, 8000)));
      
      EasyMock.expect(this.mcmpConfig.getExcludedContexts()).andReturn("ignored");
      
      this.source.init(Collections.singletonMap("localhost", Collections.singleton("/ignored")));
      
      EasyMock.expect(this.lbfProviderFactory.createLoadBalanceFactorProvider()).andReturn(this.lbfProvider);
      
      EasyMock.expect(this.mcmpConfig.getAdvertise()).andReturn(null);
      
      EasyMock.replay(this.mcmpHandler, this.mcmpConfig, this.lbfProviderFactory);
      
      this.handler.init(server);
      
      EasyMock.verify(this.mcmpHandler, this.mcmpConfig, this.lbfProviderFactory);
      EasyMock.reset(this.mcmpHandler, this.mcmpConfig, this.lbfProviderFactory);
   }
   
   @Test
   public void initNoProxies() throws IOException
   {
      Server server = EasyMock.createStrictMock(Server.class);
      // Test advertise = null, no proxies configured
      EasyMock.expect(this.mcmpConfig.getProxyList()).andReturn(null);
      Capture<Map<String, Set<String>>> capturedMap = new Capture<Map<String, Set<String>>>();
      
      List<InetSocketAddress> emptyList = Collections.emptyList();
      
      this.mcmpHandler.init(emptyList);
      
      EasyMock.expect(this.mcmpConfig.getExcludedContexts()).andReturn("host1:ignored,ROOT");
      
      this.source.init(EasyMock.capture(capturedMap));
      
      EasyMock.expect(this.lbfProviderFactory.createLoadBalanceFactorProvider()).andReturn(this.lbfProvider);
      
      EasyMock.expect(this.mcmpConfig.getAdvertise()).andReturn(null);
      
      EasyMock.expect(this.listenerFactory.createListener(this.mcmpHandler, this.mcmpConfig)).andReturn(this.listener);
      
      this.listener.start();
      
      EasyMock.replay(this.mcmpHandler, this.listenerFactory, this.mcmpConfig, this.lbfProviderFactory, this.listener, this.source);
      
      this.handler.init(server);
      
      EasyMock.verify(this.mcmpHandler, this.listenerFactory, this.mcmpConfig, this.lbfProviderFactory, this.listener, this.source);
      
      Map<String, Set<String>> contexts = capturedMap.getValue();
      
      Assert.assertEquals(2, contexts.size());
      Assert.assertEquals(Collections.singleton("/ignored"), contexts.get("host1"));
      Assert.assertEquals(Collections.singleton(""), contexts.get("localhost"));
      
      EasyMock.reset(this.mcmpHandler, this.listenerFactory, this.mcmpConfig, this.lbfProviderFactory, this.listener, this.source);
   }
   
   @Test
   public void shutdownNoAdvertise()
   {
      // Test w/out advertise listener
      this.mcmpHandler.shutdown();
      
      EasyMock.replay(this.mcmpHandler);
      
      this.handler.shutdown();
      
      EasyMock.verify(this.mcmpHandler);
      EasyMock.reset(this.mcmpHandler);
   }
   
   @Test
   public void shutdownAdvertise() throws IOException
   {
      // Test w/advertise listener
      // First init() to create listener
      this.initAdvertise();
      
      // Now test shutdown()
      this.listener.destroy();
      
      this.mcmpHandler.shutdown();
      
      EasyMock.replay(this.mcmpHandler, this.listener);
      
      this.handler.shutdown();
      
      EasyMock.verify(this.mcmpHandler, this.listener);
      EasyMock.reset(this.mcmpHandler, this.listener);
   }
   
   @Test
   public void addContextNotInit()
   {
      Context context = EasyMock.createStrictMock(Context.class);
      
      EasyMock.replay(context);
      
      // Test not initialized
      try
      {
         this.handler.addContext(context);
         
         Assert.fail();
      }
      catch (IllegalStateException e)
      {
         // Expected
      }
      
      EasyMock.verify(context);
      EasyMock.reset(context);
   }

   @Test
   public void addContextIgnored() throws IOException
   {
      Context context = EasyMock.createStrictMock(Context.class);
      Host host = EasyMock.createStrictMock(Host.class);
      
      init();
      
      // Exclusion check
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("localhost");
      EasyMock.expect(context.getPath()).andReturn("/ignored");
      
      EasyMock.replay(context, host);
      
      this.handler.addContext(context);
      
      EasyMock.verify(context, host);
      EasyMock.reset(context, host);
   }

   @Test
   public void addContextNotStarted() throws IOException
   {
      Context context = EasyMock.createStrictMock(Context.class);
      Host host = EasyMock.createStrictMock(Host.class);
      
      init();
      
      // Exclusion check
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("host1");
      
      EasyMock.expect(context.isStarted()).andReturn(false);
      
      EasyMock.replay(context, host);
      
      this.handler.addContext(context);
      
      EasyMock.verify(context, host);
      EasyMock.reset(context, host);
   }
   
   @Test
   public void addContext() throws IOException
   {
      Context context = EasyMock.createStrictMock(Context.class);
      Host host = EasyMock.createStrictMock(Host.class);
      MCMPRequest request = EasyMock.createStrictMock(MCMPRequest.class);
      
      init();

      // Exclusion check
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("localhost");
      EasyMock.expect(context.getPath()).andReturn("/context");
      
      EasyMock.expect(context.isStarted()).andReturn(true);

      // Expect log message
      EasyMock.expect(context.getPath()).andReturn("/context");
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("host");
      
      EasyMock.expect(this.requestFactory.createEnableRequest(context)).andReturn(request);
      
      this.mcmpHandler.sendRequest(request);
      
      EasyMock.replay(this.mcmpHandler, this.requestFactory, context, host);
      
      this.handler.addContext(context);
      
      EasyMock.verify(this.mcmpHandler, context, host);
   }
   
   @Test
   public void startContextNoInit()
   {
      Context context = EasyMock.createStrictMock(Context.class);
      
      EasyMock.replay(context);
      
      // Test not initialized
      try
      {
         this.handler.startContext(context);
         
         Assert.fail();
      }
      catch (IllegalStateException e)
      {
         // Expected
      }
      
      EasyMock.verify(context);
   }

   @Test
   public void startContextIgnored() throws IOException
   {
      Context context = EasyMock.createStrictMock(Context.class);
      Host host = EasyMock.createStrictMock(Host.class);
      
      init();
      
      // Exclusion check
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("localhost");
      EasyMock.expect(context.getPath()).andReturn("/ignored");
      
      EasyMock.replay(context, host);
      
      this.handler.startContext(context);
      
      EasyMock.verify(context, host);
   }

   @Test
   public void startContext() throws IOException
   {
      Context context = EasyMock.createStrictMock(Context.class);
      Host host = EasyMock.createStrictMock(Host.class);
      MCMPRequest request = EasyMock.createStrictMock(MCMPRequest.class);
      
      init();

      // Exclusion check
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("localhost");
      EasyMock.expect(context.getPath()).andReturn("/context");

      // Expect log message
      EasyMock.expect(context.getPath()).andReturn("/context");
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("host");
      
      EasyMock.expect(this.requestFactory.createEnableRequest(context)).andReturn(request);
      
      this.mcmpHandler.sendRequest(request);
      
      EasyMock.replay(this.mcmpHandler, this.requestFactory, context, host);
      
      this.handler.startContext(context);
      
      EasyMock.verify(this.mcmpHandler, this.requestFactory, context, host);
   }
   
   @Test
   public void stopContextNoInit()
   {
      Context context = EasyMock.createStrictMock(Context.class);
      
      EasyMock.replay(context);
      
      // Test not initialized
      try
      {
         this.handler.stopContext(context);
         
         Assert.fail();
      }
      catch (IllegalStateException e)
      {
         // Expected
      }

      EasyMock.verify(context);
   }

   @Test
   public void stopContextIgnored() throws IOException
   {
      Context context = EasyMock.createStrictMock(Context.class);
      Host host = EasyMock.createStrictMock(Host.class);
      
      init();
      
      // Exclusion check
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("localhost");
      EasyMock.expect(context.getPath()).andReturn("/ignored");
      
      EasyMock.replay(context, host);
      
      this.handler.stopContext(context);
      
      EasyMock.verify(context, host);
   }
   
   @Test
   public void stopContext() throws IOException
   {
      Context context = EasyMock.createStrictMock(Context.class);
      MCMPRequest request = EasyMock.createStrictMock(MCMPRequest.class);
      Host host = EasyMock.createStrictMock(Host.class);
      
      init();

      // Exclusion check
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("localhost");
      EasyMock.expect(context.getPath()).andReturn("/context");

      // Expect log message
      EasyMock.expect(context.getPath()).andReturn("/context");
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("localhost");
      
      EasyMock.expect(this.requestFactory.createStopRequest(context)).andReturn(request);
      
      this.mcmpHandler.sendRequest(request);
      
      EasyMock.replay(this.mcmpHandler, this.requestFactory, context, host);
      
      this.handler.stopContext(context);
      
      EasyMock.verify(this.mcmpHandler, this.requestFactory, context, host);
   }
   
   @Test
   public void removeContextNoInit()
   {
      Context context = EasyMock.createStrictMock(Context.class);
      
      EasyMock.replay(context);
      
      // Test not initialized
      try
      {
         this.handler.removeContext(context);
         
         Assert.fail();
      }
      catch (IllegalStateException e)
      {
         // Expected
      }

      EasyMock.verify(context);
   }

   @Test
   public void removeContextIgnored() throws IOException
   {
      Context context = EasyMock.createStrictMock(Context.class);
      Host host = EasyMock.createStrictMock(Host.class);
      
      init();
      
      // Exclusion check
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("localhost");
      EasyMock.expect(context.getPath()).andReturn("/ignored");
      
      EasyMock.replay(context, host);
      
      this.handler.removeContext(context);
      
      EasyMock.verify(context, host);
   }

   @Test
   public void removeContextNoJvmRoute() throws IOException
   {
      Context context = EasyMock.createStrictMock(Context.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      
      init();

      // Exclusion check
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("localhost");
      EasyMock.expect(context.getPath()).andReturn("/context");

      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getParent()).andReturn(engine);
      EasyMock.expect(engine.getJvmRoute()).andReturn(null);
      
      EasyMock.replay(context, host, engine);
      
      this.handler.removeContext(context);
      
      EasyMock.verify(context, host, engine);
   }
      
   @Test
   public void removeContext() throws IOException
   {
      Context context = EasyMock.createStrictMock(Context.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      MCMPRequest request = EasyMock.createStrictMock(MCMPRequest.class);
      
      init();

      // Exclusion check
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("localhost");
      EasyMock.expect(context.getPath()).andReturn("/context");
      
      // jvm route null check
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getParent()).andReturn(engine);
      EasyMock.expect(engine.getJvmRoute()).andReturn("host1");
      
      // Expect log message
      EasyMock.expect(context.getPath()).andReturn("/context");
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("host");
      
      EasyMock.expect(this.requestFactory.createRemoveRequest(context)).andReturn(request);
      
      this.mcmpHandler.sendRequest(request);
      
      EasyMock.replay(this.mcmpHandler, this.requestFactory, context, engine, host);
      
      this.handler.removeContext(context);
      
      EasyMock.verify(this.mcmpHandler, context, engine, host);
   }
   
   @Test
   public void statusNoInit()
   {
      Engine engine = EasyMock.createStrictMock(Engine.class);

      EasyMock.replay(engine);
      
      // Test not initialized
      try
      {
         this.handler.status(engine);
         
         Assert.fail();
      }
      catch (IllegalStateException e)
      {
         // Expected
      }

      EasyMock.verify(engine);
      EasyMock.reset(engine);
   }
      
   @Test
   public void status() throws IOException
   {
      Engine engine = EasyMock.createStrictMock(Engine.class);
      MCMPRequest request = EasyMock.createStrictMock(MCMPRequest.class);
      
      init();

      // Expect log message
      EasyMock.expect(engine.getName()).andReturn("engine");
      
      this.mcmpHandler.status();
      
      EasyMock.expect(this.lbfProvider.getLoadBalanceFactor()).andReturn(10);
      EasyMock.expect(engine.getJvmRoute()).andReturn("host1");
      
      EasyMock.expect(this.requestFactory.createStatusRequest("host1", 10)).andReturn(request);
      
      this.mcmpHandler.sendRequest(request);
      
      EasyMock.replay(this.mcmpHandler, this.requestFactory, this.lbfProvider, engine);
      
      this.handler.status(engine);
      
      EasyMock.verify(this.mcmpHandler, this.requestFactory, this.lbfProvider, engine);
   }
   
   @Test
   public void startServerNoInit()
   {
      Server server = EasyMock.createStrictMock(Server.class);
      
      EasyMock.replay(server);
      
      // Test not initialized
      try
      {
         this.handler.startServer(server);
         
         Assert.fail();
      }
      catch (IllegalStateException e)
      {
         // Expected
      }
      
      EasyMock.verify(server);
      EasyMock.reset(server);
   }
      
   @Test
   public void startServer() throws Exception
   {
      Server server = EasyMock.createStrictMock(Server.class);
      Service service = EasyMock.createStrictMock(Service.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      Context context = EasyMock.createStrictMock(Context.class);
      MCMPRequest request = EasyMock.createStrictMock(MCMPRequest.class);
      Connector connector = new Connector("AJP/1.3");
      
      init();
      
      // Test initialized
      EasyMock.expect(server.findServices()).andReturn(new Service[] { service });
      EasyMock.expect(service.getContainer()).andReturn(engine);

      // Expect log message
      EasyMock.expect(engine.getName()).andReturn("engine");
      
      EasyMock.expect(engine.getService()).andReturn(service);
      EasyMock.expect(service.findConnectors()).andReturn(new Connector[] { connector });
      EasyMock.expect(engine.getJvmRoute()).andReturn(null);
      Set<MCMPServerState> states = Collections.emptySet();
      EasyMock.expect(this.mcmpHandler.getProxyStates()).andReturn(states);
      
      EasyMock.expect(this.requestFactory.createConfigRequest(engine, this.nodeConfig, this.balancerConfig)).andReturn(request);
      
      this.mcmpHandler.sendRequest(request);

      EasyMock.expect(engine.findChildren()).andReturn(new Container[] { host });
      EasyMock.expect(host.findChildren()).andReturn(new Container[] { context });

      // Exclusion check
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("localhost");
      EasyMock.expect(context.getPath()).andReturn("/context");
      
      EasyMock.expect(context.isStarted()).andReturn(false);
      
      EasyMock.replay(this.mcmpHandler, this.requestFactory, server, service, engine, host, context);
      
      this.handler.startServer(server);
      
      EasyMock.verify(this.mcmpHandler, this.requestFactory, server, service, engine, host, context);
   }
   
   @Test
   public void stopServerNoInit()
   {
      Server server = EasyMock.createStrictMock(Server.class);

      EasyMock.replay(server);
      
      // Test not initialized
      try
      {
         this.handler.stopServer(server);
         
         Assert.fail();
      }
      catch (IllegalStateException e)
      {
         // Expected
      }
      
      EasyMock.verify(server);
      EasyMock.reset(server);
   }
      
   @Test
   public void stopServer() throws IOException
   {
      Server server = EasyMock.createStrictMock(Server.class);
      Service service = EasyMock.createStrictMock(Service.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      Context context = EasyMock.createStrictMock(Context.class);
      MCMPRequest contextRequest = EasyMock.createStrictMock(MCMPRequest.class);
      MCMPRequest engineRequest = EasyMock.createStrictMock(MCMPRequest.class);
      
      init();
      
      EasyMock.expect(server.findServices()).andReturn(new Service[] { service });
      EasyMock.expect(service.getContainer()).andReturn(engine);

      EasyMock.expect(engine.findChildren()).andReturn(new Container[] { host });
      EasyMock.expect(host.findChildren()).andReturn(new Container[] { context });

      // Exclusion check
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("localhost");
      EasyMock.expect(context.getPath()).andReturn("/context");
      
      // jvm route null check
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getParent()).andReturn(engine);
      EasyMock.expect(engine.getJvmRoute()).andReturn("host1");
      
      // Expect log message
      EasyMock.expect(context.getPath()).andReturn("/context");
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("host");
      
      EasyMock.expect(this.requestFactory.createRemoveRequest(context)).andReturn(contextRequest);
      
      this.mcmpHandler.sendRequest(contextRequest);
      
      EasyMock.expect(engine.getJvmRoute()).andReturn("host1");
      
      // Expect log message
      EasyMock.expect(engine.getName()).andReturn("engine");
      
      EasyMock.expect(this.requestFactory.createRemoveRequest(engine)).andReturn(engineRequest);
      
      this.mcmpHandler.sendRequest(engineRequest);
      
      EasyMock.replay(this.mcmpHandler, this.requestFactory, server, service, engine, host, context);
      
      this.handler.stopServer(server);
      
      EasyMock.verify(this.mcmpHandler, this.requestFactory, server, service, engine, host, context);
   }
}