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
import org.jboss.modcluster.mcmp.AddressPort;
import org.jboss.modcluster.mcmp.MCMPHandler;
import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPRequestType;
import org.jboss.modcluster.mcmp.MCMPServerState;
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
   private final LoadBalanceFactorProvider provider = EasyMock.createStrictMock(LoadBalanceFactorProvider.class);
   private final AdvertiseListenerFactory listenerFactory = EasyMock.createStrictMock(AdvertiseListenerFactory.class);
   
   private JBossWebEventHandler handler;

   @Before
   public void construct() throws Exception
   {
      this.handler = new DefaultJBossWebEventHandler(this.nodeConfig, this.balancerConfig, this.mcmpConfig, this.mcmpHandler, this.provider, this.listenerFactory);
   }
   
   @Test
   public void init() throws IOException
   {
      InetAddress localAddress = InetAddress.getLocalHost();
      String localHostName = localAddress.getHostName();
      
      AdvertiseListener listener = EasyMock.createStrictMock(AdvertiseListener.class);
      
      // Test advertise = false
      EasyMock.expect(this.mcmpConfig.getProxyList()).andReturn(localHostName);
      
      this.mcmpHandler.init(Collections.singletonList(new AddressPort(localAddress, 8000)));
      
      EasyMock.expect(this.mcmpConfig.getAdvertise()).andReturn(Boolean.FALSE);
      
      EasyMock.replay(this.mcmpHandler, this.mcmpConfig, listener);
      
      this.handler.init();
      
      EasyMock.verify(this.mcmpHandler, this.mcmpConfig, listener);
      EasyMock.reset(this.mcmpHandler, this.mcmpConfig, listener);
      
      
      // Test advertise = true
      EasyMock.expect(this.mcmpConfig.getProxyList()).andReturn(localHostName);
      
      this.mcmpHandler.init(Collections.singletonList(new AddressPort(localAddress, 8000)));
      
      EasyMock.expect(this.mcmpConfig.getAdvertise()).andReturn(Boolean.TRUE);
      
      EasyMock.expect(this.listenerFactory.createListener(this.mcmpHandler, this.mcmpConfig)).andReturn(listener);
      
      listener.start();
      
      EasyMock.replay(this.mcmpHandler, this.listenerFactory, this.mcmpConfig, listener);
      
      this.handler.init();
      
      EasyMock.verify(this.mcmpHandler, this.listenerFactory, this.mcmpConfig, listener);
      EasyMock.reset(this.mcmpHandler, this.listenerFactory, this.mcmpConfig, listener);
      
      
      // Test advertise = null, proxies configured
      EasyMock.expect(this.mcmpConfig.getProxyList()).andReturn(localHostName);
      
      this.mcmpHandler.init(Collections.singletonList(new AddressPort(localAddress, 8000)));
      
      EasyMock.expect(this.mcmpConfig.getAdvertise()).andReturn(null);
      
      EasyMock.replay(this.mcmpHandler, this.mcmpConfig, listener);
      
      this.handler.init();
      
      EasyMock.verify(this.mcmpHandler, this.mcmpConfig, listener);
      EasyMock.reset(this.mcmpHandler, this.mcmpConfig, listener);
      
      
      // Test advertise = null, no proxies configured
      EasyMock.expect(this.mcmpConfig.getProxyList()).andReturn(null);
      
      List<AddressPort> emptyList = Collections.emptyList();
      
      this.mcmpHandler.init(emptyList);
      
      EasyMock.expect(this.mcmpConfig.getAdvertise()).andReturn(null);
      
      EasyMock.expect(this.listenerFactory.createListener(this.mcmpHandler, this.mcmpConfig)).andReturn(listener);
      
      listener.start();
      
      EasyMock.replay(this.mcmpHandler, this.listenerFactory, this.mcmpConfig, listener);
      
      this.handler.init();
      
      EasyMock.verify(this.mcmpHandler, this.listenerFactory, this.mcmpConfig, listener);
      EasyMock.reset(this.mcmpHandler, this.listenerFactory, this.mcmpConfig, listener);
   }
   
   @Test
   public void shutdown() throws IOException
   {
      // Test w/out advertise listener
      this.mcmpHandler.shutdown();
      
      EasyMock.replay(this.mcmpHandler);
      
      this.handler.shutdown();
      
      EasyMock.verify(this.mcmpHandler);
      EasyMock.reset(this.mcmpHandler);
      
      
      // Test w/advertise listener
      // First init() to create listener
      InetAddress localAddress = InetAddress.getLocalHost();
      String localHostName = localAddress.getHostName();
      
      AdvertiseListener listener = EasyMock.createStrictMock(AdvertiseListener.class);
      
      EasyMock.expect(this.mcmpConfig.getProxyList()).andReturn(localHostName);
      
      this.mcmpHandler.init(Collections.singletonList(new AddressPort(localAddress, 8000)));
      
      EasyMock.expect(this.mcmpConfig.getAdvertise()).andReturn(Boolean.TRUE);
      
      EasyMock.expect(this.listenerFactory.createListener(this.mcmpHandler, this.mcmpConfig)).andReturn(listener);
      
      listener.start();
      
      EasyMock.replay(this.mcmpHandler, this.listenerFactory, this.mcmpConfig, listener);
      
      this.handler.init();
      
      EasyMock.verify(this.mcmpHandler, this.listenerFactory, this.mcmpConfig, listener);
      EasyMock.reset(this.mcmpHandler, this.listenerFactory, this.mcmpConfig, listener);
      
      // Now test shutdown()
      listener.destroy();
      
      this.mcmpHandler.shutdown();
      
      EasyMock.replay(this.mcmpHandler, listener);
      
      this.handler.shutdown();
      
      EasyMock.verify(this.mcmpHandler, listener);
      EasyMock.reset(this.mcmpHandler, listener);
   }
   
   @Test
   public void addContext() throws IOException
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
      
      init();

      Host host = EasyMock.createStrictMock(Host.class);
      
      // Test context not started
      recordAddContext(context, host);
      
      EasyMock.replay(context, host);
      
      this.handler.addContext(context);
      
      EasyMock.verify(context, host);
      EasyMock.reset(context, host);
      
      // Test context started
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Capture<MCMPRequest> capturedRequest = new Capture<MCMPRequest>();
            
      // Expect log message
      EasyMock.expect(context.getPath()).andReturn("/context");
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("host");
      
      EasyMock.expect(context.isStarted()).andReturn(true);

      // Building request
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getParent()).andReturn(engine);
      EasyMock.expect(engine.getJvmRoute()).andReturn("host1");
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("host");
      EasyMock.expect(host.findAliases()).andReturn(new String[] { "alias1", "alias2" });
      EasyMock.expect(context.getPath()).andReturn("/context");
      
      this.mcmpHandler.sendRequest(EasyMock.capture(capturedRequest));
      
      EasyMock.replay(this.mcmpHandler, context, engine, host);
      
      this.handler.addContext(context);
      
      EasyMock.verify(this.mcmpHandler, context, engine, host);
      
      MCMPRequest request = capturedRequest.getValue();
      
      Assert.assertSame(MCMPRequestType.ENABLE_APP, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertEquals("host1", request.getJvmRoute());
      
      Map<String, String> parameters = request.getParameters();
      
      Assert.assertEquals(2, parameters.size());
      
      Assert.assertEquals("/context", parameters.get("Context"));
      Assert.assertEquals("host,alias1,alias2", parameters.get("Alias"));
      
      EasyMock.reset(this.mcmpHandler, context, engine, host);
   }
   
   private void recordAddContext(Context context, Container container)
   {
      // Expect log message
      EasyMock.expect(context.getPath()).andReturn("/context");
      EasyMock.expect(context.getParent()).andReturn(container);
      EasyMock.expect(container.getName()).andReturn("host");
      
      EasyMock.expect(context.isStarted()).andReturn(false);
   }
   
   @Test
   public void startContext() throws IOException
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
      EasyMock.reset(context);
      
      init();

      // Test initialized
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      Capture<MCMPRequest> capturedRequest = new Capture<MCMPRequest>();
            
      // Expect log message
      EasyMock.expect(context.getPath()).andReturn("/context");
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("host");
      
      // Building request
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getParent()).andReturn(engine);
      EasyMock.expect(engine.getJvmRoute()).andReturn("host1");
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("host");
      EasyMock.expect(host.findAliases()).andReturn(new String[] { "alias1", "alias2" });
      EasyMock.expect(context.getPath()).andReturn("/context");
      
      this.mcmpHandler.sendRequest(EasyMock.capture(capturedRequest));
      
      EasyMock.replay(this.mcmpHandler, context, engine, host);
      
      this.handler.startContext(context);
      
      EasyMock.verify(this.mcmpHandler, context, engine, host);
      
      MCMPRequest request = capturedRequest.getValue();
      
      Assert.assertSame(MCMPRequestType.ENABLE_APP, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertEquals("host1", request.getJvmRoute());
      
      Map<String, String> parameters = request.getParameters();
      
      Assert.assertEquals(2, parameters.size());
      
      Assert.assertEquals("/context", parameters.get("Context"));
      Assert.assertEquals("host,alias1,alias2", parameters.get("Alias"));
      
      EasyMock.reset(this.mcmpHandler, context, engine, host);
   }
   
   @Test
   public void stopContext() throws IOException
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
      EasyMock.reset(context);

      init();

      // Test initialized
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      Capture<MCMPRequest> capturedRequest = new Capture<MCMPRequest>();
      
      // Expect log message
      EasyMock.expect(context.getPath()).andReturn("/context");
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("host");
      
      // Building request
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getParent()).andReturn(engine);
      EasyMock.expect(engine.getJvmRoute()).andReturn("host1");
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("host");
      EasyMock.expect(host.findAliases()).andReturn(new String[] { "alias1", "alias2" });
      EasyMock.expect(context.getPath()).andReturn("/context");
      
      this.mcmpHandler.sendRequest(EasyMock.capture(capturedRequest));
      
      EasyMock.replay(this.mcmpHandler, context, engine, host);
      
      this.handler.stopContext(context);
      
      EasyMock.verify(this.mcmpHandler, context, engine, host);
      
      MCMPRequest request = capturedRequest.getValue();
      
      Assert.assertSame(MCMPRequestType.STOP_APP, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertEquals("host1", request.getJvmRoute());
      
      Map<String, String> parameters = request.getParameters();
      
      Assert.assertEquals(2, parameters.size());
      
      Assert.assertEquals("/context", parameters.get("Context"));
      Assert.assertEquals("host,alias1,alias2", parameters.get("Alias"));
      
      EasyMock.reset(this.mcmpHandler, context, engine, host);
   }
   
   @Test
   public void removeContext() throws IOException
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
      EasyMock.reset(context);
      
      init();

      // Test initialized - no jvm route
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      
      this.recordRemoveContext(context, host, engine);
      
      EasyMock.replay(context, host, engine);
      
      this.handler.removeContext(context);
      
      EasyMock.verify(context, host, engine);
      EasyMock.reset(context, host, engine);
      
      
      // Test initialized - jvm route exists
      Capture<MCMPRequest> capturedRequest = new Capture<MCMPRequest>();
            
      // Expect log message
      EasyMock.expect(context.getPath()).andReturn("/context");
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("host");
      
      // jvm route null check
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getParent()).andReturn(engine);
      EasyMock.expect(engine.getJvmRoute()).andReturn("host1");
      
      // Building request
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getParent()).andReturn(engine);
      EasyMock.expect(engine.getJvmRoute()).andReturn("host1");
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("host");
      EasyMock.expect(host.findAliases()).andReturn(new String[] { "alias1", "alias2" });
      EasyMock.expect(context.getPath()).andReturn("/context");
      
      this.mcmpHandler.sendRequest(EasyMock.capture(capturedRequest));
      
      EasyMock.replay(this.mcmpHandler, context, engine, host);
      
      this.handler.removeContext(context);
      
      EasyMock.verify(this.mcmpHandler, context, engine, host);
      
      MCMPRequest request = capturedRequest.getValue();
      
      Assert.assertSame(MCMPRequestType.REMOVE_APP, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertEquals("host1", request.getJvmRoute());
      
      Map<String, String> parameters = request.getParameters();
      
      Assert.assertEquals(2, parameters.size());
      
      Assert.assertEquals("/context", parameters.get("Context"));
      Assert.assertEquals("host,alias1,alias2", parameters.get("Alias"));
      
      EasyMock.reset(this.mcmpHandler, context, engine, host);
   }
   
   private void recordRemoveContext(Context context, Container container, Engine engine)
   {
      // Expect log message
      EasyMock.expect(context.getPath()).andReturn("/context");
      EasyMock.expect(context.getParent()).andReturn(container);
      EasyMock.expect(container.getName()).andReturn("parent-container");

      EasyMock.expect(context.getParent()).andReturn(container);
      EasyMock.expect(container.getParent()).andReturn(engine);
      EasyMock.expect(engine.getJvmRoute()).andReturn(null);
   }
   
   @Test
   public void status() throws IOException
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
      
      init();

      // Test initialized
      Capture<MCMPRequest> capturedRequest = new Capture<MCMPRequest>();
      
      // Expect log message
      EasyMock.expect(engine.getName()).andReturn("engine");
      
      this.mcmpHandler.status();
      
      EasyMock.expect(this.provider.getLoadBalanceFactor()).andReturn(10);
      EasyMock.expect(engine.getJvmRoute()).andReturn("host1");
      
      this.mcmpHandler.sendRequest(EasyMock.capture(capturedRequest));
      
      EasyMock.replay(this.mcmpHandler, this.provider, engine);
      
      this.handler.status(engine);
      
      EasyMock.verify(this.mcmpHandler, this.provider, engine);
      
      MCMPRequest request = capturedRequest.getValue();
      
      Assert.assertSame(MCMPRequestType.STATUS, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertEquals("host1", request.getJvmRoute());
      
      Map<String, String> parameters = request.getParameters();
      
      Assert.assertEquals(1, parameters.size());
      Assert.assertEquals("10", parameters.get("Load"));
      
      EasyMock.reset(this.mcmpHandler, this.provider, engine);
   }
   
   @Test
   public void startServer() throws Exception
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
      
      init();
      
      // Test initialized
      Service service = EasyMock.createStrictMock(Service.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      Context context = EasyMock.createStrictMock(Context.class);
      Capture<MCMPRequest> capturedRequest = new Capture<MCMPRequest>();
      Connector connector = new Connector("AJP/1.3");
      
      EasyMock.expect(server.findServices()).andReturn(new Service[] { service });
      EasyMock.expect(service.getContainer()).andReturn(engine);

      // Expect log message
      EasyMock.expect(engine.getName()).andReturn("engine");
      
      EasyMock.expect(engine.getService()).andReturn(service);
      EasyMock.expect(service.findConnectors()).andReturn(new Connector[] { connector });
      EasyMock.expect(engine.getJvmRoute()).andReturn(null);
      Set<MCMPServerState> states = Collections.emptySet();
      EasyMock.expect(this.mcmpHandler.getProxyStates()).andReturn(states);
      
      EasyMock.expect(engine.getJvmRoute()).andReturn("host1");
      EasyMock.expect(engine.getService()).andReturn(service);
      EasyMock.expect(service.findConnectors()).andReturn(new Connector[] { connector });
      
      EasyMock.expect(this.nodeConfig.getDomain()).andReturn("domain");
      EasyMock.expect(this.nodeConfig.getFlushPackets()).andReturn(Boolean.TRUE);
      EasyMock.expect(this.nodeConfig.getFlushWait()).andReturn(1);
      EasyMock.expect(this.nodeConfig.getPing()).andReturn(2);
      EasyMock.expect(this.nodeConfig.getSmax()).andReturn(3);
      EasyMock.expect(this.nodeConfig.getTtl()).andReturn(4);
      EasyMock.expect(this.nodeConfig.getNodeTimeout()).andReturn(5);
      EasyMock.expect(this.nodeConfig.getBalancer()).andReturn("S");
      
      EasyMock.expect(this.balancerConfig.getStickySession()).andReturn(Boolean.FALSE);
      EasyMock.expect(this.balancerConfig.getStickySessionRemove()).andReturn(Boolean.TRUE);
      EasyMock.expect(this.balancerConfig.getStickySessionForce()).andReturn(Boolean.FALSE);
      EasyMock.expect(this.balancerConfig.getWorkerTimeout()).andReturn(6);
      EasyMock.expect(this.balancerConfig.getMaxAttempts()).andReturn(7);
      
      this.mcmpHandler.sendRequest(EasyMock.capture(capturedRequest));

      EasyMock.expect(engine.findChildren()).andReturn(new Container[] { host });
      EasyMock.expect(host.findChildren()).andReturn(new Container[] { context });
      this.recordAddContext(context, host);
      
      EasyMock.replay(this.mcmpHandler, this.nodeConfig, this.balancerConfig, server, service, engine, host, context);
      
      this.handler.startServer(server);
      
      EasyMock.verify(this.mcmpHandler, this.nodeConfig, this.balancerConfig, server, service, engine, host, context);
      
      MCMPRequest request = capturedRequest.getValue();
      
      Assert.assertSame(MCMPRequestType.CONFIG, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertEquals("host1", request.getJvmRoute());
      
      Map<String, String> parameters = request.getParameters();
      
      Assert.assertEquals(16, parameters.size());
      Assert.assertEquals("127.0.0.1", parameters.get("Host"));
      Assert.assertEquals("0", parameters.get("Port"));
      Assert.assertEquals("ajp", parameters.get("Type"));
      Assert.assertEquals("domain", parameters.get("Domain"));
      Assert.assertEquals("On", parameters.get("flushpackets"));
      Assert.assertEquals("1", parameters.get("flushwait"));
      Assert.assertEquals("2", parameters.get("ping"));
      Assert.assertEquals("3", parameters.get("smax"));
      Assert.assertEquals("4", parameters.get("ttl"));
      Assert.assertEquals("5", parameters.get("Timeout"));
      Assert.assertEquals("S", parameters.get("Balancer"));
      Assert.assertEquals("No", parameters.get("StickySession"));
      Assert.assertEquals("Yes", parameters.get("StickySessionRemove"));
      Assert.assertEquals("No", parameters.get("StickySessionForce"));
      Assert.assertEquals("6", parameters.get("WaitWorker"));
      Assert.assertEquals("7", parameters.get("Maxattempts"));
      
      EasyMock.reset(this.mcmpHandler, this.nodeConfig, this.balancerConfig, server, service, engine, host, context);
   }
   
   @Test
   public void stopServer() throws IOException
   {
      Server server = EasyMock.createStrictMock(Server.class);
      
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
      
      init();
      
      // Test initialized
      Service service = EasyMock.createStrictMock(Service.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Container container = EasyMock.createStrictMock(Container.class);
      Context context = EasyMock.createStrictMock(Context.class);
      Capture<MCMPRequest> capturedRequest = new Capture<MCMPRequest>();
            
      EasyMock.expect(server.findServices()).andReturn(new Service[] { service });
      EasyMock.expect(service.getContainer()).andReturn(engine);

      // Expect log message
      EasyMock.expect(engine.getName()).andReturn("engine");
      
      EasyMock.expect(engine.getJvmRoute()).andReturn("host1").times(2);
      
      this.mcmpHandler.sendRequest(EasyMock.capture(capturedRequest));
      
      EasyMock.expect(engine.findChildren()).andReturn(new Container[] { container });
      EasyMock.expect(container.findChildren()).andReturn(new Container[] { context });
      this.recordRemoveContext(context, container, engine);
      
      EasyMock.replay(this.mcmpHandler, this.nodeConfig, this.balancerConfig, server, service, engine, container, context);
      
      this.handler.stopServer(server);
      
      EasyMock.verify(this.mcmpHandler, this.nodeConfig, this.balancerConfig, server, service, engine, container, context);

      MCMPRequest request = capturedRequest.getValue();
      
      Assert.assertSame(MCMPRequestType.REMOVE_APP, request.getRequestType());
      Assert.assertTrue(request.isWildcard());
      Assert.assertEquals("host1", request.getJvmRoute());
      Assert.assertTrue(request.getParameters().isEmpty());
      
      EasyMock.reset(this.mcmpHandler, this.nodeConfig, this.balancerConfig, server, service, engine, container, context);
   }
}