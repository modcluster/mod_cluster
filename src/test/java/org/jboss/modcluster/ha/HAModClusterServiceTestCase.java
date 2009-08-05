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
package org.jboss.modcluster.ha;

import org.apache.catalina.Engine;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.easymock.EasyMock;
import org.jboss.ha.framework.interfaces.ClusterNode;
import org.jboss.ha.framework.interfaces.HAPartition;
import org.jboss.ha.framework.interfaces.HASingletonElectionPolicy;
import org.jboss.modcluster.ServerProvider;
import org.jboss.modcluster.config.BalancerConfiguration;
import org.jboss.modcluster.config.MCMPHandlerConfiguration;
import org.jboss.modcluster.config.NodeConfiguration;
import org.jboss.modcluster.config.ha.HAConfiguration;
import org.jboss.modcluster.load.LoadBalanceFactorProvider;
import org.jboss.modcluster.mcmp.MCMPHandler;
import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPRequestFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
@SuppressWarnings("boxing")
public class HAModClusterServiceTestCase
{
   private HAPartition partition = EasyMock.createStrictMock(HAPartition.class);
   private NodeConfiguration nodeConfig = EasyMock.createMock(NodeConfiguration.class);
   private BalancerConfiguration balancerConfig = EasyMock.createMock(BalancerConfiguration.class);
   private MCMPHandlerConfiguration mcmpConfig = EasyMock.createMock(MCMPHandlerConfiguration.class);
   private HAConfiguration haConfig = EasyMock.createMock(HAConfiguration.class);
   private MCMPHandler mcmpHandler = EasyMock.createStrictMock(MCMPHandler.class);
   private HASingletonAwareResetRequestSource resetRequestSource = EasyMock.createStrictMock(HASingletonAwareResetRequestSource.class);
   private ClusteredMCMPHandler clusteredMCMPHandler = EasyMock.createStrictMock(ClusteredMCMPHandler.class);
   private LoadBalanceFactorProvider lbfProvider = EasyMock.createStrictMock(LoadBalanceFactorProvider.class);
   private HASingletonElectionPolicy electionPolicy = EasyMock.createStrictMock(HASingletonElectionPolicy.class);
   private ClusterNode node = EasyMock.createStrictMock(ClusterNode.class);
   private LifecycleListener lifecycleListener = EasyMock.createStrictMock(LifecycleListener.class);
   private MCMPRequestFactory requestFactory = EasyMock.createStrictMock(MCMPRequestFactory.class);
   @SuppressWarnings("unchecked")
   private ServerProvider<Server> serverProvider = EasyMock.createStrictMock(ServerProvider.class);
   
   private static final boolean MASTER_PER_DOMAIN = true;
   private static final String SERVICE_HA_NAME = "myservice";
   private static final String DOMAIN = "domain";
   
   private HAModClusterService service;
   
   @Before
   public void setUp() throws Exception
   {
      EasyMock.expect(this.haConfig.isMasterPerDomain()).andReturn(MASTER_PER_DOMAIN);
      EasyMock.expect(this.nodeConfig.getDomain()).andReturn(DOMAIN);
      
      EasyMock.expect(this.partition.getClusterNode()).andReturn(this.node).times(2);
      
      EasyMock.replay(this.mcmpConfig, this.nodeConfig, this.haConfig, this.partition);
      
      this.service = new HAModClusterService(this.partition, this.nodeConfig, this.balancerConfig, this.mcmpConfig, this.haConfig, this.mcmpHandler, this.serverProvider, this.requestFactory, this.resetRequestSource, this.clusteredMCMPHandler, this.lifecycleListener, this.lbfProvider, this.electionPolicy);
      this.service.setServiceHAName(SERVICE_HA_NAME);
      
      EasyMock.verify(this.mcmpConfig, this.nodeConfig, this.haConfig, this.partition);
      EasyMock.reset(this.mcmpConfig, this.nodeConfig, this.haConfig, this.partition);
   }

   @Test
   public void getServer()
   {
      Server server = EasyMock.createMock(Server.class);
      
      EasyMock.expect(this.serverProvider.getServer()).andReturn(server);
      
      EasyMock.replay(this.serverProvider);
      
      Server result = this.service.getServer();
      
      EasyMock.verify(this.serverProvider);
      
      Assert.assertSame(server, result);
   }
   
   @Test
   public void addProxy()
   {
      String host = "127.0.0.1";
      int port = 0;
      
      this.clusteredMCMPHandler.addProxy(host, port);
      
      EasyMock.replay(this.clusteredMCMPHandler);
      
      this.service.addProxy(host, port);
      
      EasyMock.verify(this.clusteredMCMPHandler);
      EasyMock.reset(this.clusteredMCMPHandler);
   }
   
   @Test
   public void removeProxy()
   {
      String host = "127.0.0.1";
      int port = 0;
      
      this.clusteredMCMPHandler.removeProxy(host, port);
      
      EasyMock.replay(this.clusteredMCMPHandler);
      
      this.service.removeProxy(host, port);
      
      EasyMock.verify(this.clusteredMCMPHandler);
      EasyMock.reset(this.clusteredMCMPHandler);
   }
   
   @Test
   public void getProxyConfiguration()
   {
      EasyMock.expect(this.clusteredMCMPHandler.getProxyConfiguration()).andReturn("configuration");
      
      EasyMock.replay(this.clusteredMCMPHandler);
      
      String result = this.service.getProxyConfiguration();
      
      EasyMock.verify(this.clusteredMCMPHandler);
      
      Assert.assertEquals("configuration", result);
      
      EasyMock.reset(this.clusteredMCMPHandler);
   }
   
   @Test
   public void getProxyInfo()
   {
      EasyMock.expect(this.clusteredMCMPHandler.getProxyInfo()).andReturn("info");
      
      EasyMock.replay(this.clusteredMCMPHandler);
      
      String result = this.service.getProxyInfo();
      
      EasyMock.verify(this.clusteredMCMPHandler);
      
      Assert.assertEquals("info", result);
      
      EasyMock.reset(this.clusteredMCMPHandler);
   }
   
   @Test
   public void reset()
   {
      this.clusteredMCMPHandler.reset();
      
      EasyMock.replay(this.clusteredMCMPHandler);
      
      this.service.reset();
      
      EasyMock.verify(this.clusteredMCMPHandler);
      EasyMock.reset(this.clusteredMCMPHandler);
   }
   
   @Test
   public void refresh()
   {
      this.clusteredMCMPHandler.markProxiesInError();
      
      EasyMock.replay(this.clusteredMCMPHandler);
      
      this.service.refresh();
      
      EasyMock.verify(this.clusteredMCMPHandler);
      EasyMock.reset(this.clusteredMCMPHandler);
   }
   
   @Test
   public void enable()
   {
      Server server = EasyMock.createStrictMock(Server.class);
      Service service = EasyMock.createStrictMock(Service.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      MCMPRequest request = EasyMock.createMock(MCMPRequest.class);
      
      EasyMock.expect(this.serverProvider.getServer()).andReturn(server);
      EasyMock.expect(server.findServices()).andReturn(new Service[] { service });
      EasyMock.expect(service.getContainer()).andReturn(engine);

      EasyMock.expect(this.requestFactory.createEnableRequest(engine)).andReturn(request);
      
      this.clusteredMCMPHandler.sendRequest(request);
      
      EasyMock.expect(this.clusteredMCMPHandler.isProxyHealthOK()).andReturn(true);
      
      EasyMock.replay(this.serverProvider, this.requestFactory, this.clusteredMCMPHandler, server, service, engine);
      
      boolean result = this.service.enable();
      
      EasyMock.verify(this.serverProvider, this.requestFactory, this.clusteredMCMPHandler, server, service, engine);
      
      Assert.assertTrue(result);
   }
   
   @Test
   public void disable()
   {
      Server server = EasyMock.createStrictMock(Server.class);
      Service service = EasyMock.createStrictMock(Service.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      MCMPRequest request = EasyMock.createMock(MCMPRequest.class);
      
      EasyMock.expect(this.serverProvider.getServer()).andReturn(server);
      EasyMock.expect(server.findServices()).andReturn(new Service[] { service });
      EasyMock.expect(service.getContainer()).andReturn(engine);
      
      EasyMock.expect(this.requestFactory.createDisableRequest(engine)).andReturn(request);
      
      this.clusteredMCMPHandler.sendRequest(request);
      
      EasyMock.expect(this.clusteredMCMPHandler.isProxyHealthOK()).andReturn(true);
      
      EasyMock.replay(this.serverProvider, this.requestFactory, this.clusteredMCMPHandler, server, service, engine);
      
      boolean result = this.service.disable();
      
      EasyMock.verify(this.serverProvider, this.requestFactory, this.clusteredMCMPHandler, server, service, engine);
      
      Assert.assertTrue(result);
   }
   
   @Test
   public void lifecycleEvent()
   {
      LifecycleEvent event = new LifecycleEvent(EasyMock.createMock(Lifecycle.class), Lifecycle.INIT_EVENT);
      
      this.lifecycleListener.lifecycleEvent(event);
      
      EasyMock.replay(this.lifecycleListener);
      
      this.service.lifecycleEvent(event);
      
      EasyMock.verify(this.lifecycleListener);
      EasyMock.reset(this.lifecycleListener);
   }
   
   @Test
   public void createLoadBalanceFactorProvider()
   {
      LoadBalanceFactorProvider lbfProvider = this.service.createLoadBalanceFactorProvider();
      
      Assert.assertSame(this.lbfProvider, lbfProvider);
   }
   
/*   
   @Test
   public void init() throws UnknownHostException
   {
      InetAddress localAddress = InetAddress.getLocalHost();
      String localHostName = localAddress.getHostName();
      
      AdvertiseListener listener = EasyMock.createStrictMock(AdvertiseListener.class);
      
      // Test advertise = false
      EasyMock.expect(this.mcmpConfig.getProxyList()).andReturn(localHostName);
      
      this.clusteredMCMPHandler.init(Collections.singletonList(new AddressPort(localAddress, 8000)));
      
      EasyMock.expect(this.mcmpConfig.getAdvertise()).andReturn(Boolean.FALSE);
      
      EasyMock.replay(this.clusteredMCMPHandler, this.mcmpConfig, listener);
      
      this.listener.init();
      
      EasyMock.verify(this.clusteredMCMPHandler, this.mcmpConfig, listener);
      EasyMock.reset(this.clusteredMCMPHandler, this.mcmpConfig, listener);

      
      // Test advertise = null, proxies configured
      EasyMock.expect(this.mcmpConfig.getProxyList()).andReturn(localHostName);
      
      this.clusteredMCMPHandler.init(Collections.singletonList(new AddressPort(localAddress, 8000)));
      
      EasyMock.expect(this.mcmpConfig.getAdvertise()).andReturn(null);
      
      EasyMock.replay(this.clusteredMCMPHandler, this.mcmpConfig, listener);
      
      this.listener.init();
      
      EasyMock.verify(this.clusteredMCMPHandler, this.mcmpConfig, listener);
      EasyMock.reset(this.clusteredMCMPHandler, this.mcmpConfig, listener);
   }

   @Test
   public void startServer() throws Exception
   {
      Server server = EasyMock.createStrictMock(Server.class);
      
      this.resetRequestSource.setJbossWebServer(server);
      
      EasyMock.replay(this.resetRequestSource);
      
      // Test not initialized
      try
      {
         this.listener.startServer(server);
         
         Assert.fail();
      }
      catch (IllegalStateException e)
      {
         // Expected
      }

      EasyMock.verify(this.resetRequestSource);
      EasyMock.reset(this.resetRequestSource);
      
      init();
      
      // Test initialized
      Service service = EasyMock.createStrictMock(Service.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Container container = EasyMock.createStrictMock(Container.class);
      Context context = EasyMock.createStrictMock(Context.class);
      DistributedReplicantManager drm = EasyMock.createStrictMock(DistributedReplicantManager.class);
      Capture<MCMPRequest> capturedRequest = new Capture<MCMPRequest>();
      Connector connector = new Connector("AJP/1.3");
      
      this.resetRequestSource.setJbossWebServer(server);
      
      EasyMock.expect(server.findServices()).andReturn(new Service[] { service });

      EasyMock.expect(service.getContainer()).andReturn(engine);

      // Expect log message
      EasyMock.expect(engine.getName()).andReturn("engine");
      
      EasyMock.expect(engine.getService()).andReturn(service);
      EasyMock.expect(service.findConnectors()).andReturn(new Connector[] { connector });
      EasyMock.expect(engine.getJvmRoute()).andReturn(null);
      Set<MCMPServerState> states = Collections.emptySet();
      EasyMock.expect(this.mcmpHandler.getProxyStates()).andReturn(states);
      
      EasyMock.expect(engine.getJvmRoute()).andReturn("route");
      EasyMock.expect(this.partition.getDistributedReplicantManager()).andReturn(drm);
      drm.add("myservice:domain", this.listener.drmEntry);
      
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
      
      this.clusteredMCMPHandler.sendRequest(EasyMock.capture(capturedRequest));

      EasyMock.expect(engine.findChildren()).andReturn(new Container[] { container });
      EasyMock.expect(container.findChildren()).andReturn(new Container[] { context });
      this.recordAddContext(context, container);
      
      EasyMock.replay(this.partition, this.resetRequestSource, this.mcmpHandler, this.clusteredMCMPHandler, this.nodeConfig, this.balancerConfig, drm, server, service, engine, container, context);
      
      this.listener.startServer(server);
      
      EasyMock.verify(this.partition, this.resetRequestSource, this.mcmpHandler, this.clusteredMCMPHandler, this.nodeConfig, this.balancerConfig, drm, server, service, engine, container, context);
      
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
      
      Set<String> routes = this.listener.drmEntry.getJvmRoutes();
      Assert.assertEquals(1, routes.size());
      Assert.assertEquals("route", routes.iterator().next());
      
      EasyMock.reset(this.partition, this.resetRequestSource, this.mcmpHandler, this.clusteredMCMPHandler, this.nodeConfig, this.balancerConfig, drm, server, service, engine, container, context);
   }

   private void recordAddContext(Context context, Container container)
   {
      // Expect log message
      EasyMock.expect(context.getPath()).andReturn("/context");
      EasyMock.expect(context.getParent()).andReturn(container);
      EasyMock.expect(container.getName()).andReturn("parent-container");
      
      EasyMock.expect(context.isStarted()).andReturn(false);
   }
   
   @Test
   public void stopServer() throws Exception
   {
      Server server = EasyMock.createStrictMock(Server.class);
      
      // Test not initialized
      try
      {
         this.listener.stopServer(server);
         
         Assert.fail();
      }
      catch (IllegalStateException e)
      {
         // Expected
      }
      
      init();
      
      this.listener.drmEntry.addJvmRoute("route");
      
      // Test initialized
      Service service = EasyMock.createStrictMock(Service.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Container container = EasyMock.createStrictMock(Container.class);
      Context context = EasyMock.createStrictMock(Context.class);
      DistributedReplicantManager drm = EasyMock.createStrictMock(DistributedReplicantManager.class);
      Capture<MCMPRequest> capturedRequest = new Capture<MCMPRequest>();
      
      EasyMock.expect(server.findServices()).andReturn(new Service[] { service });
      EasyMock.expect(service.getContainer()).andReturn(engine);

      // Expect log message
      EasyMock.expect(engine.getName()).andReturn("engine");
      
      EasyMock.expect(engine.getJvmRoute()).andReturn("host1").times(2);
      
      this.clusteredMCMPHandler.sendRequest(EasyMock.capture(capturedRequest));
      
      EasyMock.expect(engine.getJvmRoute()).andReturn("route");
      EasyMock.expect(this.partition.getDistributedReplicantManager()).andReturn(drm);
      drm.add("myservice:domain", this.listener.drmEntry);
      
      EasyMock.expect(engine.findChildren()).andReturn(new Container[] { container });
      EasyMock.expect(container.findChildren()).andReturn(new Container[] { context });
      this.recordRemoveContext(context, container, engine);
      
      EasyMock.replay(this.partition, this.clusteredMCMPHandler, this.nodeConfig, this.balancerConfig, drm, server, service, engine, container, context);
      
      this.listener.stopServer(server);
      
      EasyMock.verify(this.partition, this.clusteredMCMPHandler, this.nodeConfig, this.balancerConfig, drm, server, service, engine, container, context);

      MCMPRequest request = capturedRequest.getValue();
      
      Assert.assertSame(MCMPRequestType.REMOVE_APP, request.getRequestType());
      Assert.assertTrue(request.isWildcard());
      Assert.assertEquals("host1", request.getJvmRoute());
      Assert.assertTrue(request.getParameters().isEmpty());
      
      Assert.assertTrue(this.listener.drmEntry.getJvmRoutes().isEmpty());
      
      EasyMock.reset(this.partition, this.clusteredMCMPHandler, this.nodeConfig, this.balancerConfig, drm, server, service, engine, container, context);
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
   public void addContext() throws UnknownHostException
   {
      Context context = EasyMock.createStrictMock(Context.class);
      
      EasyMock.replay(context);
      
      // Test not initialized
      try
      {
         this.listener.addContext(context);
         
         Assert.fail();
      }
      catch (IllegalStateException e)
      {
         // Expected
      }

      EasyMock.verify(context);
      EasyMock.reset(context);
      
      init();

      // Test context not started
      Host host = EasyMock.createStrictMock(Host.class);
      
      recordAddContext(context, host);
      
      EasyMock.replay(context, host);
      
      this.listener.addContext(context);
      
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
      
      this.clusteredMCMPHandler.sendRequest(EasyMock.capture(capturedRequest));
      
      EasyMock.replay(this.clusteredMCMPHandler, context, engine, host);
      
      this.listener.addContext(context);
      
      EasyMock.verify(this.clusteredMCMPHandler, context, engine, host);
      
      MCMPRequest request = capturedRequest.getValue();
      
      Assert.assertSame(MCMPRequestType.ENABLE_APP, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertEquals("host1", request.getJvmRoute());
      
      Map<String, String> parameters = request.getParameters();
      
      Assert.assertEquals(2, parameters.size());
      
      Assert.assertEquals("/context", parameters.get("Context"));
      Assert.assertEquals("host,alias1,alias2", parameters.get("Alias"));
      
      EasyMock.reset(this.clusteredMCMPHandler, context, engine, host);
   }
   
   @Test
   public void startContext() throws UnknownHostException
   {
      Context context = EasyMock.createStrictMock(Context.class);
      
      EasyMock.replay(context);
      
      // Test not initialized
      try
      {
         this.listener.startContext(context);
         
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
      
      this.clusteredMCMPHandler.sendRequest(EasyMock.capture(capturedRequest));
      
      EasyMock.replay(this.clusteredMCMPHandler, context, engine, host);
      
      this.listener.startContext(context);
      
      EasyMock.verify(this.clusteredMCMPHandler, context, engine, host);
      
      MCMPRequest request = capturedRequest.getValue();
      
      Assert.assertSame(MCMPRequestType.ENABLE_APP, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertEquals("host1", request.getJvmRoute());
      
      Map<String, String> parameters = request.getParameters();
      
      Assert.assertEquals(2, parameters.size());
      
      Assert.assertEquals("/context", parameters.get("Context"));
      Assert.assertEquals("host,alias1,alias2", parameters.get("Alias"));
      
      EasyMock.reset(this.clusteredMCMPHandler, context, engine, host);
   }
   
   @Test
   public void stopContext() throws UnknownHostException
   {
      Context context = EasyMock.createStrictMock(Context.class);
      
      EasyMock.replay(context);
      
      // Test not initialized
      try
      {
         this.listener.stopContext(context);
         
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
      
      this.clusteredMCMPHandler.sendRequest(EasyMock.capture(capturedRequest));
      
      EasyMock.replay(this.clusteredMCMPHandler, context, engine, host);
      
      this.listener.stopContext(context);
      
      EasyMock.verify(this.clusteredMCMPHandler, context, engine, host);
      
      MCMPRequest request = capturedRequest.getValue();
      
      Assert.assertSame(MCMPRequestType.STOP_APP, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertEquals("host1", request.getJvmRoute());
      
      Map<String, String> parameters = request.getParameters();
      
      Assert.assertEquals(2, parameters.size());
      
      Assert.assertEquals("/context", parameters.get("Context"));
      Assert.assertEquals("host,alias1,alias2", parameters.get("Alias"));
      
      EasyMock.reset(this.clusteredMCMPHandler, context, engine, host);
   }
   
   @Test
   public void removeContext() throws UnknownHostException
   {
      Context context = EasyMock.createStrictMock(Context.class);
      
      // Test not initialized
      try
      {
         this.listener.removeContext(context);
         
         Assert.fail();
      }
      catch (IllegalStateException e)
      {
         // Expected
      }
      
      init();

      // Test initialized - no jvm route
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      
      this.recordRemoveContext(context, host, engine);
      
      EasyMock.replay(context, host, engine);
      
      this.listener.removeContext(context);
      
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
      
      this.clusteredMCMPHandler.sendRequest(EasyMock.capture(capturedRequest));
      
      EasyMock.replay(this.clusteredMCMPHandler, context, engine, host);
      
      this.listener.removeContext(context);
      
      EasyMock.verify(this.clusteredMCMPHandler, context, engine, host);
      
      MCMPRequest request = capturedRequest.getValue();
      
      Assert.assertSame(MCMPRequestType.REMOVE_APP, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertEquals("host1", request.getJvmRoute());
      
      Map<String, String> parameters = request.getParameters();
      
      Assert.assertEquals(2, parameters.size());
      
      Assert.assertEquals("/context", parameters.get("Context"));
      Assert.assertEquals("host,alias1,alias2", parameters.get("Alias"));
      
      EasyMock.reset(this.clusteredMCMPHandler, context, engine, host);
   }

   @SuppressWarnings("unchecked")
   @Test
   public void status() throws Exception
   {
      Engine engine = EasyMock.createStrictMock(Engine.class);

      EasyMock.replay(engine);
      
      // Test not initialized
      try
      {
         this.listener.status(engine);
         
         Assert.fail();
      }
      catch (IllegalStateException e)
      {
         // Expected
      }

      EasyMock.verify(engine);
      EasyMock.reset(engine);
      
      init();
      
      // Test non-master status
      EasyMock.expect(engine.getName()).andReturn("engine");
      EasyMock.expect(this.lbfProvider.getLoadBalanceFactor()).andReturn(10);
      
      EasyMock.replay(this.lbfProvider, engine);
      
      this.listener.status(engine);
      
      EasyMock.verify(this.lbfProvider, engine);
      EasyMock.reset(this.lbfProvider, engine);
      
      
      // Make master
      String key = SERVICE_HA_NAME + ":" + DOMAIN;
      DistributedReplicantManager drm = EasyMock.createStrictMock(DistributedReplicantManager.class);
      
      this.listener.setElectionPolicy(null);
      
      EasyMock.expect(this.partition.getDistributedReplicantManager()).andReturn(drm);
      EasyMock.expect(drm.isMasterReplica(key)).andReturn(true);

      this.partition.callAsynchMethodOnCluster(EasyMock.eq("myservice"), EasyMock.eq("stopOldMaster"), EasyMock.aryEq(new Object[0]), EasyMock.aryEq(new Class[0]), EasyMock.eq(true));
      
      EasyMock.replay(this.partition, drm);
      
      this.listener.replicantsChanged(key, Collections.EMPTY_LIST, 1, false);
      
      EasyMock.verify(this.partition, drm);
      
      Assert.assertTrue(this.listener.isMasterNode());
      
      EasyMock.reset(this.partition, drm);
      
      // Create drm entries
      ClusterNode remoteNode1 = EasyMock.createMock(ClusterNode.class);
      MCMPServerState remoteState1 = EasyMock.createMock(MCMPServerState.class);
      
      EasyMock.expect(remoteState1.getState()).andReturn(MCMPServerState.State.OK);
      EasyMock.expect(remoteState1.isEstablished()).andReturn(true);
      
      EasyMock.replay(remoteState1);
      
      ModClusterServiceDRMEntry drmEntry1 = new ModClusterServiceDRMEntry(remoteNode1, Collections.singleton(remoteState1));
      drmEntry1.addJvmRoute("host1");
      
      EasyMock.verify(remoteState1);
      EasyMock.reset(remoteState1);
      
      
      MCMPServerState remoteState2 = EasyMock.createMock(MCMPServerState.class);
      ClusterNode remoteNode2 = EasyMock.createMock(ClusterNode.class);
      
      EasyMock.expect(remoteState2.getState()).andReturn(MCMPServerState.State.DOWN);
      EasyMock.expect(remoteState2.isEstablished()).andReturn(false);
      
      EasyMock.replay(remoteState2);
      
      ModClusterServiceDRMEntry drmEntry2 = new ModClusterServiceDRMEntry(remoteNode2, Collections.singleton(remoteState2));
      drmEntry2.addJvmRoute("host2");
      
      EasyMock.verify(remoteState2);
      EasyMock.reset(remoteState2);
      
      // Test master status
      MCMPServerState localState = EasyMock.createMock(MCMPServerState.class);
      ModClusterServiceDRMEntry drmEntry = new ModClusterServiceDRMEntry(this.node, null);
      InetAddress address1 = InetAddress.getByName("127.0.0.1");
      InetAddress address2 = InetAddress.getByName("127.0.1.1");
      MCMPServerDiscoveryEvent event1 = new MCMPServerDiscoveryEvent(remoteNode1, new AddressPort(address1, 1), true, 1);
      MCMPServerDiscoveryEvent event2 = new MCMPServerDiscoveryEvent(remoteNode2, new AddressPort(address2, 2), false, 2);

      Map<String, String> emptyMap = Collections.emptyMap();
      MCMPRequest request1 = new MCMPRequest(MCMPRequestType.ENABLE_APP, false, "route", emptyMap);
      MCMPRequest request2 = new MCMPRequest(MCMPRequestType.DISABLE_APP, false, "route", emptyMap);
      Capture<List<MCMPRequest>> capturedRequests = new Capture<List<MCMPRequest>>();
      Capture<ModClusterServiceDRMEntry> capturedEntry = new Capture<ModClusterServiceDRMEntry>();
      Capture<Object[]> capturedArgs = new Capture<Object[]>();
      
      ModClusterServiceStateGroupRpcResponse response1 = new ModClusterServiceStateGroupRpcResponse(remoteNode1, 10, new TreeSet<MCMPServerState>(), Collections.singletonList(event1), new ArrayList<MCMPRequest>());
      ModClusterServiceStateGroupRpcResponse response2 = new ModClusterServiceStateGroupRpcResponse(remoteNode2, 20, new TreeSet<MCMPServerState>(), Collections.singletonList(event2), new ArrayList<MCMPRequest>());
      
      EasyMock.expect(engine.getName()).andReturn("engine");
      EasyMock.expect(this.lbfProvider.getLoadBalanceFactor()).andReturn(10);
      
      this.mcmpHandler.status();

      EasyMock.expect(this.mcmpHandler.getProxyStates()).andReturn(new TreeSet<MCMPServerState>());
      
      EasyMock.expect(this.partition.getDistributedReplicantManager()).andReturn(drm);
      EasyMock.expect(drm.lookupReplicants(key)).andReturn(Collections.singletonList(drmEntry));
      EasyMock.expect(this.partition.getClusterNode()).andReturn(this.node);
      EasyMock.expect(this.partition.callMethodOnCluster(EasyMock.eq(SERVICE_HA_NAME), EasyMock.eq("getClusterCoordinatorState"), EasyMock.aryEq(new Object[] { new TreeSet<MCMPServerState>() }), EasyMock.aryEq(new Class[] { Set.class }), EasyMock.eq(true))).andReturn(new ArrayList<Object>(Arrays.asList(response1, response2)));
      
      // Process discovery events
      this.mcmpHandler.addProxy(address1, 1);
      this.mcmpHandler.removeProxy(address2, 2);
      
      // Start over - this time with no discovery events
      response1 = new ModClusterServiceStateGroupRpcResponse(remoteNode1, 10, Collections.singleton(remoteState1), new ArrayList<MCMPServerDiscoveryEvent>(), Collections.singletonList(request1));
      response2 = new ModClusterServiceStateGroupRpcResponse(remoteNode2, 20, Collections.singleton(remoteState2), new ArrayList<MCMPServerDiscoveryEvent>(), Collections.singletonList(request2));
      
      this.mcmpHandler.status();

      Set<MCMPServerState> states = new LinkedHashSet<MCMPServerState>(Arrays.asList(remoteState1, remoteState2));
      
      EasyMock.expect(this.mcmpHandler.getProxyStates()).andReturn(states);
      
      EasyMock.expect(drm.lookupReplicants(key)).andReturn(Arrays.asList(drmEntry1, drmEntry2));
      EasyMock.expect(this.partition.getClusterNode()).andReturn(this.node);
      EasyMock.expect(this.partition.callMethodOnCluster(EasyMock.eq(SERVICE_HA_NAME), EasyMock.eq("getClusterCoordinatorState"), EasyMock.aryEq(new Object[] { states }), EasyMock.aryEq(new Class[] { Set.class }), EasyMock.eq(true))).andReturn(new ArrayList<Object>(Arrays.asList(response1, response2)));
      
      EasyMock.expect(remoteState1.getState()).andReturn(MCMPServerState.State.OK);
      EasyMock.expect(remoteState1.isEstablished()).andReturn(true);
      EasyMock.expect(remoteState2.getState()).andReturn(MCMPServerState.State.DOWN);
      EasyMock.expect(remoteState2.isEstablished()).andReturn(false);
      
      this.mcmpHandler.sendRequests(Arrays.asList(request1, request2));
      this.mcmpHandler.sendRequests(EasyMock.capture(capturedRequests));
      
      EasyMock.expect(this.partition.getDistributedReplicantManager()).andReturn(drm);
      EasyMock.expect(drm.lookupLocalReplicant(key)).andReturn(drmEntry);
      
      EasyMock.expect(this.partition.getClusterNode()).andReturn(this.node);
      
      EasyMock.expect(remoteState1.getState()).andReturn(MCMPServerState.State.OK);
      EasyMock.expect(remoteState1.isEstablished()).andReturn(true);
      EasyMock.expect(remoteState2.getState()).andReturn(MCMPServerState.State.DOWN);
      EasyMock.expect(remoteState2.isEstablished()).andReturn(false);

      EasyMock.expect(this.partition.getDistributedReplicantManager()).andReturn(drm);
      
      drm.add(EasyMock.eq(key), EasyMock.capture(capturedEntry));
      
      EasyMock.expect(this.partition.callMethodOnCluster(EasyMock.eq("myservice"), EasyMock.eq("clusterStatusComplete"), EasyMock.capture(capturedArgs), EasyMock.aryEq(new Class[] { Map.class }), EasyMock.eq(true))).andReturn(null);
      
      EasyMock.replay(this.lbfProvider, this.mcmpHandler, this.clusteredMCMPHandler, this.partition, drm, localState, this.node, remoteState1, remoteState2, remoteNode1, remoteNode2, engine);
      
      this.listener.status(engine);
      
      EasyMock.verify(this.lbfProvider, this.mcmpHandler, this.clusteredMCMPHandler, this.partition, drm, localState, this.node, remoteState1, remoteState2, remoteNode1, remoteNode2, engine);
      
      List<MCMPRequest> requests = capturedRequests.getValue();
      
      Assert.assertEquals(2, requests.size());

      Assert.assertSame(MCMPRequestType.STATUS, requests.get(0).getRequestType());
      Assert.assertFalse(requests.get(0).isWildcard());
      Assert.assertEquals("host2", requests.get(0).getJvmRoute());
      Assert.assertEquals(1, requests.get(0).getParameters().size());
      Assert.assertEquals("20", requests.get(0).getParameters().get("Load"));

      Assert.assertSame(MCMPRequestType.STATUS, requests.get(1).getRequestType());
      Assert.assertFalse(requests.get(1).isWildcard());
      Assert.assertEquals("host1", requests.get(1).getJvmRoute());
      Assert.assertEquals(1, requests.get(1).getParameters().size());
      Assert.assertEquals("10", requests.get(1).getParameters().get("Load"));

      ModClusterServiceDRMEntry entry = capturedEntry.getValue();
      
      Assert.assertSame(this.node, entry.getPeer());
      Assert.assertEquals(states, entry.getMCMPServerStates());
      Assert.assertTrue(entry.getJvmRoutes().isEmpty());

      Object[] args = capturedArgs.getValue();
      Assert.assertEquals(1, args.length);
      Assert.assertTrue(args[0] instanceof Map);
      
      Map<ClusterNode, PeerMCMPDiscoveryStatus> map = (Map<ClusterNode, PeerMCMPDiscoveryStatus>) args[0];
      
      Assert.assertEquals(2, map.size());
      Assert.assertTrue(map.containsKey(remoteNode1));
      Assert.assertTrue(map.containsKey(remoteNode2));
      
      PeerMCMPDiscoveryStatus status1 = map.get(remoteNode1);
      
      Assert.assertSame(remoteNode1, status1.getPeer());
      Assert.assertEquals(Collections.singleton(remoteState1), status1.getMCMPServerStates());
      Assert.assertTrue(status1.getJvmRoutes().isEmpty());
      Assert.assertNull(status1.getLatestDiscoveryEvent());
      
      EasyMock.reset(this.lbfProvider, this.mcmpHandler, this.clusteredMCMPHandler, this.partition, drm, localState, this.node, remoteState1, remoteState2, remoteNode1, remoteNode2, engine);
      
      
      // Test master status, but off-frequency
      this.listener.setProcessStatusFrequency(2);
      
      EasyMock.expect(engine.getName()).andReturn("engine");
      EasyMock.expect(this.lbfProvider.getLoadBalanceFactor()).andReturn(10);
      
      EasyMock.replay(this.lbfProvider, engine);
      
      this.listener.status(engine);
      
      EasyMock.verify(this.lbfProvider, engine);
      EasyMock.reset(this.lbfProvider, engine);
   }
*/
}
