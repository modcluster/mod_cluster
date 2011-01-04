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
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionListener;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.jboss.modcluster.ModClusterService.EnablableRequestListener;
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
import org.jboss.modcluster.mcmp.MCMPResponseParser;
import org.jboss.modcluster.mcmp.MCMPServerState;
import org.jboss.modcluster.mcmp.ResetRequestSource;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
@SuppressWarnings("boxing")
public class ModClusterServiceTestCase
{
   private final NodeConfiguration nodeConfig = EasyMock.createStrictMock(NodeConfiguration.class);
   private final BalancerConfiguration balancerConfig = EasyMock.createStrictMock(BalancerConfiguration.class);
   private final MCMPHandlerConfiguration mcmpConfig = EasyMock.createStrictMock(MCMPHandlerConfiguration.class);
   private final MCMPHandler mcmpHandler = EasyMock.createStrictMock(MCMPHandler.class);
   private final ResetRequestSource source = EasyMock.createStrictMock(ResetRequestSource.class);
   private final MCMPRequestFactory requestFactory = EasyMock.createStrictMock(MCMPRequestFactory.class);
   private final MCMPResponseParser responseParser = EasyMock.createStrictMock(MCMPResponseParser.class);
   private final LoadBalanceFactorProviderFactory lbfProviderFactory = EasyMock.createStrictMock(LoadBalanceFactorProviderFactory.class);
   private final LoadBalanceFactorProvider lbfProvider = EasyMock.createStrictMock(LoadBalanceFactorProvider.class);
   private final AdvertiseListenerFactory advertiseListenerFactory = EasyMock.createStrictMock(AdvertiseListenerFactory.class);
   private final AdvertiseListener advertiseListener = EasyMock.createStrictMock(AdvertiseListener.class);
   
   private ModClusterService service = new ModClusterService(this.nodeConfig, this.balancerConfig, this.mcmpConfig, this.lbfProviderFactory, this.requestFactory, this.responseParser, this.source, this.mcmpHandler, this.advertiseListenerFactory);
   
   private void init(Server server)
   {
      this.init(server, EasyMock.createStrictMock(Engine.class), EasyMock.createStrictMock(Host.class), true);
   }
   
   private void init(Server server, Engine engine, Host host, boolean autoEnableContexts)
   {
      InetAddress localAddress = this.getLocalAddress();
      String localHostName = localAddress.getHostName();
      
      // Test advertise = null, proxies configured
      EasyMock.expect(this.mcmpConfig.getProxyList()).andReturn(localHostName);
      
      this.mcmpHandler.init(Collections.singletonList(new InetSocketAddress(localAddress, 8000)), this.service);
      
      EasyMock.expect(this.mcmpConfig.isAutoEnableContexts()).andReturn(autoEnableContexts);
      EasyMock.expect(this.mcmpConfig.getExcludedContexts()).andReturn("excluded");
      
      EasyMock.expect(server.getEngines()).andReturn(Collections.singleton(engine));
      EasyMock.expect(engine.getHosts()).andReturn(Collections.singleton(host));
      EasyMock.expect(host.getName()).andReturn("localhost");
      
      this.source.init(server, this.service);
      
      EasyMock.expect(this.lbfProviderFactory.createLoadBalanceFactorProvider()).andReturn(this.lbfProvider);
      
      EasyMock.expect(this.mcmpConfig.getAdvertise()).andReturn(null);
      
      EasyMock.replay(server, engine, host, this.mcmpHandler, this.mcmpConfig, this.lbfProviderFactory);
      
      this.service.init(server);

      EasyMock.verify(server, engine, host, this.mcmpHandler, this.mcmpConfig, this.lbfProviderFactory);
      
      Assert.assertEquals(autoEnableContexts, this.service.isAutoEnableContexts());

      Map<Host, Set<String>> contexts = this.service.getExcludedContexts();
      Assert.assertEquals(1, contexts.size());
      Set<String> paths = contexts.get(host);
      Assert.assertNotNull(paths);
      Assert.assertEquals(1, paths.size());
      Assert.assertTrue(paths.contains("/excluded"));
      
      EasyMock.reset(server, engine, host, this.mcmpHandler, this.mcmpConfig, this.lbfProviderFactory);
   }
   
   private void establishConnection(Server server)
   {
      this.establishConnection(server, EasyMock.createStrictMock(Engine.class), EasyMock.createStrictMock(Host.class), true);
   }
   
   private void establishConnection(Server server, Engine engine, Host host, boolean autoEnableContexts)
   {
      this.init(server, engine, host, autoEnableContexts);
      
      InetAddress localAddress = this.getLocalAddress();
      Connector connector = EasyMock.createStrictMock(Connector.class);
      JvmRouteFactory factory = EasyMock.createStrictMock(JvmRouteFactory.class);
      
      EasyMock.expect(server.getEngines()).andReturn(Collections.singleton(engine));

      EasyMock.expect(engine.getProxyConnector()).andReturn(connector);
      EasyMock.expect(connector.getAddress()).andReturn(this.getWildcardAddress());
      
      connector.setAddress(localAddress);
      
      EasyMock.expect(engine.getJvmRoute()).andReturn(null);
      EasyMock.expect(this.mcmpConfig.getJvmRouteFactory()).andReturn(factory);
      EasyMock.expect(factory.createJvmRoute(EasyMock.same(engine))).andReturn("jvm-route");
      
      engine.setJvmRoute("jvm-route");
      
      EasyMock.replay(server, engine, connector, this.mcmpConfig, factory);
      
      this.service.connectionEstablished(localAddress);
      
      EasyMock.verify(server, engine, connector, this.mcmpConfig, factory);
      EasyMock.reset(server, engine, connector, this.mcmpConfig, factory);
   }
   
   private InetAddress getLocalAddress()
   {
      try
      {
         return InetAddress.getByName("127.0.0.1");
      }
      catch (UnknownHostException e)
      {
         throw new IllegalStateException(e);
      }
   }
   
   private InetAddress getWildcardAddress()
   {
      try
      {
         InetAddress address = InetAddress.getByName("0.0.0.0");
         
         Assert.assertTrue(address.isAnyLocalAddress());
         
         return address;
      }
      catch (UnknownHostException e)
      {
         Assert.fail(e.getMessage());
         throw new IllegalStateException(e);
      }
   }
   
   @Test
   public void establishConnection()
   {
      Server server = EasyMock.createStrictMock(Server.class);
      
      this.establishConnection(server);
   }
   
   @Test
   public void getLoadBalanceFactor()
   {
      Server server = EasyMock.createStrictMock(Server.class);
      
      this.init(server);
      
      Integer expected = 10;
      
      EasyMock.expect(this.lbfProvider.getLoadBalanceFactor()).andReturn(expected);
      
      EasyMock.replay(this.lbfProvider);
      
      Integer result = this.service.getLoadBalanceFactor();
      
      EasyMock.verify(this.lbfProvider);
      
      Assert.assertSame(expected, result);
      
      EasyMock.reset(this.lbfProvider);
   }
   
   @Test
   public void addProxy()
   {
      Capture<InetSocketAddress> capturedSocketAddress = new Capture<InetSocketAddress>(); 
      
      this.mcmpHandler.addProxy(EasyMock.capture(capturedSocketAddress));
      
      EasyMock.replay(this.mcmpHandler);
      
      this.service.addProxy("localhost", 8080);
      
      EasyMock.verify(this.mcmpHandler);
      
      InetSocketAddress socketAddress = capturedSocketAddress.getValue();
      Assert.assertEquals("localhost", socketAddress.getHostName());
      Assert.assertEquals(8080, socketAddress.getPort());
      
      EasyMock.reset(this.mcmpHandler);
   }
   
   @Test
   public void removeProxy()
   {
      Capture<InetSocketAddress> capturedSocketAddress = new Capture<InetSocketAddress>(); 
      
      this.mcmpHandler.removeProxy(EasyMock.capture(capturedSocketAddress));
      
      EasyMock.replay(this.mcmpHandler);
      
      this.service.removeProxy("localhost", 8080);
      
      EasyMock.verify(this.mcmpHandler);
      
      InetSocketAddress socketAddress = capturedSocketAddress.getValue();
      Assert.assertEquals("localhost", socketAddress.getHostName());
      Assert.assertEquals(8080, socketAddress.getPort());
      
      EasyMock.reset(this.mcmpHandler);
   }
   
   @Test
   public void getProxyConfiguration()
   {
      MCMPRequest request = EasyMock.createStrictMock(MCMPRequest.class);

      // Test unestablished
      EasyMock.expect(this.requestFactory.createDumpRequest()).andReturn(request);
      
      EasyMock.replay(this.requestFactory, this.mcmpHandler);
      
      Map<InetSocketAddress, String> result = this.service.getProxyConfiguration();

      EasyMock.verify(this.requestFactory, this.mcmpHandler);
      
      Assert.assertNotNull(result);
      Assert.assertEquals(0, result.size());

      EasyMock.reset(this.requestFactory, this.mcmpHandler);
      
      
      Server server = EasyMock.createStrictMock(Server.class);
      
      this.establishConnection(server);
      
      // Test established
      InetSocketAddress address = InetSocketAddress.createUnresolved("localhost", 8080);
      String configuration = "config";
      MCMPServerState state = EasyMock.createStrictMock(MCMPServerState.class);
      
      EasyMock.expect(this.requestFactory.createDumpRequest()).andReturn(request);
      EasyMock.expect(this.mcmpHandler.sendRequest(request)).andReturn(Collections.singletonMap(state, configuration));
      EasyMock.expect(state.getSocketAddress()).andReturn(address);
      
      EasyMock.replay(this.mcmpHandler, this.requestFactory, state, request);
      
      result = this.service.getProxyConfiguration();
      
      EasyMock.verify(this.mcmpHandler, this.requestFactory, state, request);
      
      Assert.assertEquals(1, result.size());
      Assert.assertSame(configuration, result.get(address));
      
      EasyMock.reset(this.mcmpHandler, this.requestFactory, state, request);
   }
   
   @Test
   public void getProxyInfo()
   {
      // Test unestablished
      MCMPRequest request = EasyMock.createStrictMock(MCMPRequest.class);
      
      EasyMock.expect(this.requestFactory.createInfoRequest()).andReturn(request);

      EasyMock.replay(this.requestFactory, this.mcmpHandler);
      
      // Test established
      Map<InetSocketAddress, String> result = this.service.getProxyInfo();

      EasyMock.verify(this.requestFactory, this.mcmpHandler);
      
      Assert.assertNotNull(result);
      Assert.assertEquals(0, result.size());

      EasyMock.reset(this.requestFactory, this.mcmpHandler);

      
      // Test established
      Server server = EasyMock.createStrictMock(Server.class);
      
      this.establishConnection(server);
      
      InetSocketAddress address = InetSocketAddress.createUnresolved("localhost", 8080);
      String information = "info";
      MCMPServerState state = EasyMock.createStrictMock(MCMPServerState.class);
      
      EasyMock.expect(this.requestFactory.createInfoRequest()).andReturn(request);
      EasyMock.expect(this.mcmpHandler.sendRequest(request)).andReturn(Collections.singletonMap(state, information));
      EasyMock.expect(state.getSocketAddress()).andReturn(address);
      
      EasyMock.replay(this.mcmpHandler, this.requestFactory, state, request);
      
      result = this.service.getProxyInfo();
      
      EasyMock.verify(this.mcmpHandler, this.requestFactory, state, request);
      
      Assert.assertEquals(1, result.size());
      Assert.assertSame(information, result.get(address));
      
      EasyMock.reset(this.mcmpHandler, this.requestFactory, state, request);
   }
   
   @Test
   public void ping()
   {
      // Test unestablished
      // Test no parameter
      MCMPRequest request = EasyMock.createStrictMock(MCMPRequest.class);
      
      EasyMock.expect(this.requestFactory.createPingRequest()).andReturn(request);

      EasyMock.replay(this.requestFactory, this.mcmpHandler);
      
      Map<InetSocketAddress, String> result = this.service.ping();

      EasyMock.verify(this.requestFactory, this.mcmpHandler);
      
      Assert.assertNotNull(result);
      Assert.assertEquals(0, result.size());
      
      EasyMock.reset(this.requestFactory, this.mcmpHandler);


      // Test url scheme, host, port
      EasyMock.expect(this.requestFactory.createPingRequest("ajp", "127.0.0.1", 8009)).andReturn(request);

      EasyMock.replay(this.requestFactory, this.mcmpHandler);
      
      result = this.service.ping("ajp", "127.0.0.1", 8009);

      EasyMock.verify(this.requestFactory, this.mcmpHandler);
      
      Assert.assertNotNull(result);
      Assert.assertEquals(0, result.size());
      
      EasyMock.reset(this.requestFactory, this.mcmpHandler);

      
      // Test jvmRoute
      EasyMock.expect(this.requestFactory.createPingRequest("route")).andReturn(request);

      EasyMock.replay(this.requestFactory, this.mcmpHandler);
      
      result = this.service.ping("route");

      EasyMock.verify(this.requestFactory, this.mcmpHandler);
      
      Assert.assertNotNull(result);
      Assert.assertEquals(0, result.size());
      
      EasyMock.reset(this.requestFactory, this.mcmpHandler);
      
      // Test established, repeat above tests
      Server server = EasyMock.createStrictMock(Server.class);
      
      this.establishConnection(server);
      
      InetSocketAddress address = InetSocketAddress.createUnresolved("localhost", 8080);
      String pingResult = "OK";
      
      MCMPServerState state = EasyMock.createStrictMock(MCMPServerState.class);
      
      // Test no parameter
      EasyMock.expect(this.requestFactory.createPingRequest()).andReturn(request);
      EasyMock.expect(this.mcmpHandler.sendRequest(request)).andReturn(Collections.singletonMap(state, pingResult));
      EasyMock.expect(state.getSocketAddress()).andReturn(address);
      
      EasyMock.replay(this.mcmpHandler, this.requestFactory, state, request);
      
      result = this.service.ping();
      
      EasyMock.verify(this.mcmpHandler, this.requestFactory, state, request);
      
      Assert.assertEquals(1, result.size());
      Assert.assertSame(pingResult, result.get(address));
      
      EasyMock.reset(this.mcmpHandler, this.requestFactory, state, request);
      
      // Test url scheme, host and port
      
      EasyMock.expect(this.requestFactory.createPingRequest("ajp", "127.0.0.1", 8009)).andReturn(request);
      EasyMock.expect(this.mcmpHandler.sendRequest(request)).andReturn(Collections.singletonMap(state, pingResult));
      EasyMock.expect(state.getSocketAddress()).andReturn(address);
      
      EasyMock.replay(this.mcmpHandler, this.requestFactory, state, request);
      
      result = this.service.ping("ajp", "127.0.0.1", 8009);
      
      EasyMock.verify(this.mcmpHandler, this.requestFactory, state, request);
      
      Assert.assertEquals(1, result.size());
      Assert.assertSame(pingResult, result.get(address));
      
      EasyMock.reset(this.mcmpHandler, this.requestFactory, state, request);
      
      // Test non-url parameter
      EasyMock.expect(this.requestFactory.createPingRequest("route")).andReturn(request);
      EasyMock.expect(this.mcmpHandler.sendRequest(request)).andReturn(Collections.singletonMap(state, pingResult));
      EasyMock.expect(state.getSocketAddress()).andReturn(address);
      
      EasyMock.replay(this.mcmpHandler, this.requestFactory, state, request);
      
      result = this.service.ping("route");
      
      EasyMock.verify(this.mcmpHandler, this.requestFactory, state, request);
      
      Assert.assertEquals(1, result.size());
      Assert.assertSame(pingResult, result.get(address));
      
      EasyMock.reset(this.mcmpHandler, this.requestFactory, state, request);      
   }
   
   @Test
   public void reset()
   {
      EasyMock.replay(this.mcmpHandler);
      
      this.service.reset();
      
      EasyMock.verify(this.mcmpHandler);
      EasyMock.reset(this.mcmpHandler);
      
      Server server = EasyMock.createStrictMock(Server.class);
      
      this.establishConnection(server);
      
      this.mcmpHandler.reset();
      
      EasyMock.replay(this.mcmpHandler);
      
      this.service.reset();
      
      EasyMock.verify(this.mcmpHandler);
      EasyMock.reset(this.mcmpHandler);
   }
   
   @Test
   public void refresh()
   {
      EasyMock.replay(this.mcmpHandler);
      
      this.service.refresh();
      
      EasyMock.verify(this.mcmpHandler);
      EasyMock.reset(this.mcmpHandler);
      
      Server server = EasyMock.createStrictMock(Server.class);
      
      this.establishConnection(server);
      
      this.mcmpHandler.markProxiesInError();
      
      EasyMock.replay(this.mcmpHandler);
      
      this.service.refresh();
      
      EasyMock.verify(this.mcmpHandler);
      EasyMock.reset(this.mcmpHandler);
   }

   @Test
   public void enableNotEstablished()
   {
      Server server = EasyMock.createStrictMock(Server.class);
      
      this.init(server);
      
      EasyMock.replay(server);
      
      boolean result = this.service.enable();
      
      EasyMock.verify(server);
      
      Assert.assertFalse(result);
      
      EasyMock.reset(server);
   }
   
   @Test
   public void enable()
   {
      Server server = EasyMock.createStrictMock(Server.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      MCMPRequest request = EasyMock.createStrictMock(MCMPRequest.class);
      
      this.establishConnection(server, engine, host, false);

      Assert.assertFalse(this.service.isAutoEnableContexts());
      
      EasyMock.expect(server.getEngines()).andReturn(Collections.singleton(engine));

      EasyMock.expect(this.requestFactory.createEnableRequest(engine)).andReturn(request);
      
      EasyMock.expect(this.mcmpHandler.sendRequest(request)).andReturn(null);
      
      EasyMock.expect(this.mcmpHandler.isProxyHealthOK()).andReturn(true);
      
      EasyMock.replay(server, this.requestFactory, this.mcmpHandler, engine);
      
      boolean result = this.service.enable();
      
      EasyMock.verify(server, this.requestFactory, this.mcmpHandler, engine);
      
      Assert.assertTrue(result);
      Assert.assertTrue(this.service.isAutoEnableContexts());
      
      EasyMock.reset(server, this.requestFactory, this.mcmpHandler, engine);
   }
   
   @SuppressWarnings("boxing")
   @Test
   public void disable()
   {
      Server server = EasyMock.createStrictMock(Server.class);
      
      EasyMock.replay(server);
      
      boolean result = this.service.disable();
      
      EasyMock.verify(server);
      
      Assert.assertFalse(result);
      
      EasyMock.reset(server);
      
      this.establishConnection(server);
      
      Engine engine = EasyMock.createStrictMock(Engine.class);
      MCMPRequest request = EasyMock.createMock(MCMPRequest.class);
      
      EasyMock.expect(server.getEngines()).andReturn(Collections.singleton(engine));

      EasyMock.expect(this.requestFactory.createDisableRequest(engine)).andReturn(request);
      
      EasyMock.expect(this.mcmpHandler.sendRequest(request)).andReturn(null);
      
      EasyMock.expect(this.mcmpHandler.isProxyHealthOK()).andReturn(true);
      
      EasyMock.replay(server, this.requestFactory, this.mcmpHandler, engine);
      
      result = this.service.disable();
      
      EasyMock.verify(server, this.requestFactory, this.mcmpHandler, engine);
      
      Assert.assertTrue(result);
      
      EasyMock.reset(server, this.requestFactory, this.mcmpHandler, engine);
   }
   
   @Test
   public void enableContext()
   {
      String hostName = "host1";
      String path = "/";
      
      Server server = EasyMock.createStrictMock(Server.class);

      // Test unestablished
      EasyMock.replay(server);
      
      boolean result = this.service.enableContext(hostName, path);

      EasyMock.verify(server);
      
      Assert.assertFalse(result);
      
      EasyMock.reset(server);

      
      // Test established
      this.establishConnection(server);
      
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      Context context = EasyMock.createStrictMock(Context.class);
      MCMPRequest request = EasyMock.createMock(MCMPRequest.class);
      
      EasyMock.expect(server.getEngines()).andReturn(Collections.singleton(engine));
      EasyMock.expect(engine.findHost(hostName)).andReturn(host);
      EasyMock.expect(host.findContext(path)).andReturn(context);

      EasyMock.expect(this.requestFactory.createEnableRequest(context)).andReturn(request);
      
      EasyMock.expect(this.mcmpHandler.sendRequest(request)).andReturn(null);
      
      EasyMock.expect(this.mcmpHandler.isProxyHealthOK()).andReturn(true);
      
      EasyMock.replay(server, this.requestFactory, this.mcmpHandler, engine, host, context);
      
      result = this.service.enableContext(hostName, path);
      
      EasyMock.verify(server, this.requestFactory, this.mcmpHandler, engine, host, context);
      
      Assert.assertTrue(result);
      
      EasyMock.reset(server, this.requestFactory, this.mcmpHandler, engine, host, context);
   }
   
   @Test
   public void disableContext()
   {
      String hostName = "host1";
      String path = "/";
      
      Server server = EasyMock.createStrictMock(Server.class);

      // Test unestablished
      EasyMock.replay(server);
      
      boolean result = this.service.disableContext(hostName, path);

      EasyMock.verify(server);
      
      Assert.assertFalse(result);
      
      EasyMock.reset(server);

      
      // Test established
      this.establishConnection(server);
      
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      Context context = EasyMock.createStrictMock(Context.class);
      MCMPRequest request = EasyMock.createMock(MCMPRequest.class);
      
      EasyMock.expect(server.getEngines()).andReturn(Collections.singleton(engine));
      EasyMock.expect(engine.findHost(hostName)).andReturn(host);
      EasyMock.expect(host.findContext(path)).andReturn(context);

      EasyMock.expect(this.requestFactory.createDisableRequest(context)).andReturn(request);
      
      EasyMock.expect(this.mcmpHandler.sendRequest(request)).andReturn(null);
      
      EasyMock.expect(this.mcmpHandler.isProxyHealthOK()).andReturn(true);
      
      EasyMock.replay(server, this.requestFactory, this.mcmpHandler, engine, host, context);
      
      result = this.service.disableContext(hostName, path);
      
      EasyMock.verify(server, this.requestFactory, this.mcmpHandler, engine, host, context);
      
      Assert.assertTrue(result);
      
      EasyMock.reset(server, this.requestFactory, this.mcmpHandler, engine, host, context);
   }
   
   @Test
   public void initNoAdvertise()
   {
      Server server = EasyMock.createStrictMock(Server.class);
      InetAddress localAddress = this.getLocalAddress();
      String localHostName = localAddress.getHostName();
      
      // Test advertise = false
      EasyMock.expect(this.mcmpConfig.getProxyList()).andReturn(localHostName);
      
      this.mcmpHandler.init(Collections.singletonList(new InetSocketAddress(localAddress, 8000)), this.service);
      
      EasyMock.expect(this.mcmpConfig.isAutoEnableContexts()).andReturn(true);
      EasyMock.expect(this.mcmpConfig.getExcludedContexts()).andReturn(null);
      
      this.source.init(server, this.service);
      
      EasyMock.expect(this.lbfProviderFactory.createLoadBalanceFactorProvider()).andReturn(this.lbfProvider);
      
      EasyMock.expect(this.mcmpConfig.getAdvertise()).andReturn(Boolean.FALSE);
      
      EasyMock.replay(this.mcmpHandler, this.mcmpConfig, this.lbfProviderFactory, this.source);
      
      this.service.init(server);
      
      EasyMock.verify(this.mcmpHandler, this.mcmpConfig, this.lbfProviderFactory, this.source);
      EasyMock.reset(this.mcmpHandler, this.mcmpConfig, this.lbfProviderFactory, this.source);
   }
   
   @Test
   public void initAdvertise() throws IOException
   {
      Server server = EasyMock.createStrictMock(Server.class);
      InetAddress localAddress = InetAddress.getLocalHost();
      String localHostName = localAddress.getHostName();
      
      // Test advertise = true
      EasyMock.expect(this.mcmpConfig.getProxyList()).andReturn(localHostName);
      
      this.mcmpHandler.init(Collections.singletonList(new InetSocketAddress(localAddress, 8000)), this.service);
      
      EasyMock.expect(this.mcmpConfig.isAutoEnableContexts()).andReturn(true);
      EasyMock.expect(this.mcmpConfig.getExcludedContexts()).andReturn("");
      
      this.source.init(server, this.service);
      
      EasyMock.expect(this.lbfProviderFactory.createLoadBalanceFactorProvider()).andReturn(this.lbfProvider);
      
      EasyMock.expect(this.mcmpConfig.getAdvertise()).andReturn(Boolean.TRUE);
      
      EasyMock.expect(this.advertiseListenerFactory.createListener(this.mcmpHandler, this.mcmpConfig)).andReturn(this.advertiseListener);
      
      this.advertiseListener.start();
      
      EasyMock.replay(this.mcmpHandler, this.advertiseListenerFactory, this.mcmpConfig, this.lbfProviderFactory, this.advertiseListener, this.source);
      
      this.service.init(server);
      
      EasyMock.verify(this.mcmpHandler, this.advertiseListenerFactory, this.mcmpConfig, this.lbfProviderFactory, this.advertiseListener, this.source);
      EasyMock.reset(this.mcmpHandler, this.advertiseListenerFactory, this.mcmpConfig, this.lbfProviderFactory, this.advertiseListener, this.source);
   }
   
   @Test
   public void init()
   {
      Server server = EasyMock.createStrictMock(Server.class);
      
      this.init(server);
   }
   
   @Test
   public void initNoProxies() throws IOException
   {
      Server server = EasyMock.createStrictMock(Server.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host1 = EasyMock.createStrictMock(Host.class);
      Host host2 = EasyMock.createStrictMock(Host.class);
      Host localhost = EasyMock.createStrictMock(Host.class);
      
      // Test advertise = null, no proxies configured
      EasyMock.expect(this.mcmpConfig.getProxyList()).andReturn(null);
      
      this.mcmpHandler.init(Collections.<InetSocketAddress>emptyList(), this.service);
      
      EasyMock.expect(this.mcmpConfig.isAutoEnableContexts()).andReturn(true);
      EasyMock.expect(this.mcmpConfig.getExcludedContexts()).andReturn("host1:ignored,ROOT");

      EasyMock.expect(server.getEngines()).andReturn(Collections.singleton(engine));
      EasyMock.expect(engine.getHosts()).andReturn(Arrays.asList(host1, host2, localhost));
      EasyMock.expect(host1.getName()).andReturn("host1");
      EasyMock.expect(host2.getName()).andReturn("host2");
      EasyMock.expect(localhost.getName()).andReturn("localhost");
      
      this.source.init(server, this.service);
      
      EasyMock.expect(this.lbfProviderFactory.createLoadBalanceFactorProvider()).andReturn(this.lbfProvider);
      
      EasyMock.expect(this.mcmpConfig.getAdvertise()).andReturn(null);
      
      EasyMock.expect(this.advertiseListenerFactory.createListener(this.mcmpHandler, this.mcmpConfig)).andReturn(this.advertiseListener);
      
      this.advertiseListener.start();
      
      EasyMock.replay(server, engine, host1, host2, localhost, this.mcmpHandler, this.advertiseListenerFactory, this.mcmpConfig, this.lbfProviderFactory, this.advertiseListener, this.source);
      
      this.service.init(server);
      
      EasyMock.verify(server, engine, host1, host2, localhost, this.mcmpHandler, this.advertiseListenerFactory, this.mcmpConfig, this.lbfProviderFactory, this.advertiseListener, this.source);
      
      Map<Host, Set<String>> contexts = this.service.getExcludedContexts();
      Assert.assertEquals(2, contexts.size());
      Set<String> paths = contexts.get(host1);
      Assert.assertNotNull(paths);
      Assert.assertEquals(1, paths.size());
      Assert.assertTrue(paths.contains("/ignored"));
      paths = contexts.get(localhost);
      Assert.assertNotNull(paths);
      Assert.assertEquals(1, paths.size());
      Assert.assertTrue(paths.contains(""));
      
      EasyMock.reset(server, engine, host1, host2, localhost, this.mcmpHandler, this.advertiseListenerFactory, this.mcmpConfig, this.lbfProviderFactory, this.advertiseListener, this.source);
   }
   
   @Test
   public void shutdownNoAdvertise()
   {
      // Test w/out advertise listener
      this.mcmpHandler.shutdown();
      
      EasyMock.replay(this.mcmpHandler);
      
      this.service.shutdown();
      
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
      this.advertiseListener.destroy();
      
      this.mcmpHandler.shutdown();
      
      EasyMock.replay(this.mcmpHandler, this.advertiseListener);
      
      this.service.shutdown();
      
      EasyMock.verify(this.mcmpHandler, this.advertiseListener);
      EasyMock.reset(this.mcmpHandler, this.advertiseListener);
   }
   
   @Test
   public void addContextNotEstablished()
   {
      Context context = EasyMock.createStrictMock(Context.class);
      Host host = EasyMock.createStrictMock(Host.class);
      
      EasyMock.expect(context.getHost()).andReturn(host);
      
      EasyMock.replay(context, host);
      
      this.service.add(context);
      
      EasyMock.verify(context, host);
      EasyMock.reset(context, host);
   }

   @Test
   public void addContextExcluded() throws IOException
   {
      Server server = EasyMock.createStrictMock(Server.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      
      this.establishConnection(server, engine, host, true);

      Context context = EasyMock.createStrictMock(Context.class);
      
      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(context.getPath()).andReturn("/excluded");
      
      EasyMock.replay(server, engine, host, context);
      
      this.service.add(context);
      
      EasyMock.verify(server, engine, host, context);
      EasyMock.reset(server, engine, host, context);
   }

   @Test
   public void addContextNotStarted() throws IOException
   {
      Server server = EasyMock.createStrictMock(Server.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      
      this.establishConnection(server, engine, host, true);

      Context context = EasyMock.createStrictMock(Context.class);
      
      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(context.getPath()).andReturn("");
      
      EasyMock.expect(context.isStarted()).andReturn(false);
      
      EasyMock.replay(server, engine, host, context);
      
      this.service.add(context);
      
      EasyMock.verify(server, engine, host, context);
      EasyMock.reset(server, engine, host, context);
   }
   
   @Test
   public void addContextNoAutoEnable() throws IOException
   {
      Server server = EasyMock.createStrictMock(Server.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      MCMPRequest request = EasyMock.createStrictMock(MCMPRequest.class);
      
      this.establishConnection(server, engine, host, false);
      
      Context context = EasyMock.createStrictMock(Context.class);
      
      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(context.getPath()).andReturn("");
      EasyMock.expect(context.isStarted()).andReturn(true);
      EasyMock.expect(context.getHost()).andReturn(host);
      
      EasyMock.expect(this.requestFactory.createDisableRequest(context)).andReturn(request);
      
      EasyMock.expect(this.mcmpHandler.sendRequest(request)).andReturn(Collections.<MCMPServerState, String>emptyMap());
      
      EasyMock.replay(server, engine, host, context, this.mcmpHandler, this.requestFactory);
      
      this.service.add(context);
      
      EasyMock.verify(server, engine, host, context, this.mcmpHandler, this.requestFactory);
      EasyMock.reset(server, engine, host, context, this.mcmpHandler, this.requestFactory);
   }
   
   @Test
   public void addContext() throws IOException
   {
      Server server = EasyMock.createStrictMock(Server.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      
      this.establishConnection(server, engine, host, true);
      
      Context context = EasyMock.createStrictMock(Context.class);
      MCMPRequest request = EasyMock.createStrictMock(MCMPRequest.class);
      
      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(context.getPath()).andReturn("");
      EasyMock.expect(context.isStarted()).andReturn(true);
      EasyMock.expect(context.getHost()).andReturn(host);
      
      EasyMock.expect(this.requestFactory.createEnableRequest(context)).andReturn(request);
      
      EasyMock.expect(this.mcmpHandler.sendRequest(request)).andReturn(Collections.<MCMPServerState, String>emptyMap());
      
      EasyMock.replay(server, engine, host, context, this.mcmpHandler, this.requestFactory);
      
      this.service.add(context);
      
      EasyMock.verify(server, engine, host, context, this.mcmpHandler, this.requestFactory);
      EasyMock.reset(server, engine, host, context, this.mcmpHandler, this.requestFactory);
   }
   
   @Test
   public void startContextNotEstablished()
   {
      Context context = EasyMock.createStrictMock(Context.class);
      Host host = EasyMock.createStrictMock(Host.class);
      
      EasyMock.expect(context.getHost()).andReturn(host);
      
      context.addRequestListener(EasyMock.<ServletRequestListener>notNull());
      
      EasyMock.replay(context);
      
      this.service.start(context);
      
      EasyMock.verify(context);
   }

   @Test
   public void startContextExcluded() throws IOException
   {
      Server server = EasyMock.createStrictMock(Server.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);

      this.establishConnection(server, engine, host, true);
      
      Context context = EasyMock.createStrictMock(Context.class);
      
      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(context.getPath()).andReturn("/excluded");
      
      EasyMock.replay(server, engine, host, context);
      
      this.service.start(context);
      
      EasyMock.verify(server, engine, host, context);
      EasyMock.reset(server, engine, host, context);
   }
   
   @Test
   public void startContextNoAutoEnable() throws IOException
   {
      Server server = EasyMock.createStrictMock(Server.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      
      this.establishConnection(server, engine, host, false);

      Context context = EasyMock.createStrictMock(Context.class);
      MCMPRequest request = EasyMock.createStrictMock(MCMPRequest.class);
      
      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(context.getPath()).andReturn("");
      EasyMock.expect(context.getHost()).andReturn(host);
      
      EasyMock.expect(this.requestFactory.createDisableRequest(context)).andReturn(request);
      
      EasyMock.expect(this.mcmpHandler.sendRequest(request)).andReturn(Collections.<MCMPServerState, String>emptyMap());
      
      context.addRequestListener(EasyMock.<ServletRequestListener>notNull());
      
      EasyMock.replay(server, engine, host, context, this.mcmpHandler, this.requestFactory);
      
      this.service.start(context);
      
      EasyMock.verify(server, engine, host, context, this.mcmpHandler, this.requestFactory);
      EasyMock.reset(server, engine, host, context, this.mcmpHandler, this.requestFactory);
   }

   @Test
   public void startContext() throws IOException
   {
      Server server = EasyMock.createStrictMock(Server.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      Context context = EasyMock.createStrictMock(Context.class);

      this.startContext(server, engine, host, context);
   }
   
   private EnablableRequestListener startContext(Server server, Engine engine, Host host, Context context)
   {
      this.establishConnection(server, engine, host, true);
      
      MCMPRequest request = EasyMock.createStrictMock(MCMPRequest.class);
      Capture<EnablableRequestListener> capturedListener = new Capture<EnablableRequestListener>();
      
      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(context.getPath()).andReturn("");
      EasyMock.expect(context.getHost()).andReturn(host);
      
      EasyMock.expect(this.requestFactory.createEnableRequest(context)).andReturn(request);
      
      EasyMock.expect(this.mcmpHandler.sendRequest(request)).andReturn(Collections.<MCMPServerState, String>emptyMap());
      
      context.addRequestListener(EasyMock.capture(capturedListener));
      
      EasyMock.replay(server, engine, host, context, this.mcmpHandler, this.requestFactory);
      
      this.service.start(context);
      
      EasyMock.verify(server, engine, host, context, this.mcmpHandler, this.requestFactory);
      
      EasyMock.reset(server, engine, host, context, this.mcmpHandler, this.requestFactory);
      
      return capturedListener.getValue();
   }
   
   @Test
   public void stopContextNoInit()
   {
      Context context = EasyMock.createStrictMock(Context.class);
      
      EasyMock.replay(context);
      
      this.service.stop(context);

      EasyMock.verify(context);
   }

   @Test
   public void stopContextExcluded() throws IOException
   {
      Server server = EasyMock.createStrictMock(Server.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      
      this.establishConnection(server, engine, host, true);
      
      Context context = EasyMock.createStrictMock(Context.class);

      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(context.getPath()).andReturn("/excluded");
      
      EasyMock.replay(server, engine, host, context);
      
      this.service.stop(context);
      
      EasyMock.verify(server, engine, host, context);
      EasyMock.reset(server, engine, host, context);
   }

   @Test
   public void stopNonDistributableContext() throws IOException, InterruptedException
   {
      Server server = EasyMock.createStrictMock(Server.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      Context context = EasyMock.createStrictMock(Context.class);
      
      final EnablableRequestListener listener = this.startContext(server, engine, host, context);

      MCMPRequest disableRequest = EasyMock.createStrictMock(MCMPRequest.class);
      MCMPRequest stopRequest = EasyMock.createStrictMock(MCMPRequest.class);
      MCMPServerState state = EasyMock.createStrictMock(MCMPServerState.class);
      SessionDrainingStrategy strategy = EasyMock.createStrictMock(SessionDrainingStrategy.class);
      final Capture<HttpSessionListener> capturedAddListener = new Capture<HttpSessionListener>();
      Capture<HttpSessionListener> capturedRemoveListener = new Capture<HttpSessionListener>();
      
      // Test successful drain
      
      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(context.getPath()).andReturn("");
      EasyMock.expect(context.getHost()).andReturn(host);
      
      EasyMock.expect(this.requestFactory.createDisableRequest(context)).andReturn(disableRequest);
      EasyMock.expect(this.mcmpHandler.sendRequest(disableRequest)).andReturn(Collections.<MCMPServerState, String>emptyMap());
      
      EasyMock.expect(this.mcmpConfig.getStopContextTimeoutUnit()).andReturn(TimeUnit.SECONDS);
      EasyMock.expect(this.mcmpConfig.getStopContextTimeout()).andReturn(0L);
      EasyMock.expect(this.mcmpConfig.getSessionDrainingStrategy()).andReturn(strategy);
      EasyMock.expect(strategy.isEnabled(context)).andReturn(true);
      
      EasyMock.expect(context.getActiveSessionCount()).andReturn(2);
      
      context.addSessionListener(EasyMock.capture(capturedAddListener));
      
      EasyMock.expect(context.getActiveSessionCount()).andReturn(1);
      
      try
      {
         EasyMock.expect(context.getActiveSessionCount()).andReturn(0);

         context.removeSessionListener(EasyMock.capture(capturedRemoveListener));
         
         EasyMock.expect(this.requestFactory.createStopRequest(context)).andReturn(stopRequest);
         EasyMock.expect(this.mcmpHandler.sendRequest(stopRequest)).andReturn(Collections.singletonMap(state, "response"));
         EasyMock.expect(this.responseParser.parseStopAppResponse("response")).andReturn(2);
         
         EasyMock.expect(this.mcmpHandler.sendRequest(stopRequest)).andReturn(Collections.singletonMap(state, "response"));
         EasyMock.expect(this.responseParser.parseStopAppResponse("response")).andReturn(0);
         
         EasyMock.replay(this.requestFactory, this.mcmpHandler, this.mcmpConfig, this.responseParser, server, engine, host, context, disableRequest, stopRequest, strategy);
         
         Runnable task = new Runnable()
         {
            public void run()
            {
               while (!capturedAddListener.hasCaptured() && !Thread.currentThread().isInterrupted())
               {
                  Thread.yield();
               }

               capturedAddListener.getValue().sessionDestroyed(null);
               
               while (!listener.isEnabled() && !Thread.currentThread().isInterrupted())
               {
                  Thread.yield();
               }

               listener.requestDestroyed(null);
            }
         };
         
         Thread thread = new Thread(task);
         
         thread.start();
         
         this.service.stop(context);
         
         thread.interrupt();
         thread.join();
         
         EasyMock.verify(this.requestFactory, this.mcmpHandler, this.mcmpConfig, this.responseParser, server, engine, host, context, disableRequest, stopRequest, strategy);
         
         Assert.assertSame(capturedAddListener.getValue(), capturedRemoveListener.getValue());
         
         EasyMock.reset(this.requestFactory, this.mcmpHandler, this.mcmpConfig, this.responseParser, server, engine, host, context, disableRequest, stopRequest, strategy);

         // Test drain timeout
         EasyMock.expect(context.getHost()).andReturn(host);
         EasyMock.expect(context.getPath()).andReturn("");
         EasyMock.expect(context.getHost()).andReturn(host);
         
         EasyMock.expect(this.requestFactory.createDisableRequest(context)).andReturn(disableRequest);
         EasyMock.expect(this.mcmpHandler.sendRequest(disableRequest)).andReturn(Collections.<MCMPServerState, String>emptyMap());
         
         EasyMock.expect(this.mcmpConfig.getStopContextTimeoutUnit()).andReturn(TimeUnit.SECONDS);
         EasyMock.expect(this.mcmpConfig.getStopContextTimeout()).andReturn(1L);
         EasyMock.expect(this.mcmpConfig.getSessionDrainingStrategy()).andReturn(strategy);
         EasyMock.expect(strategy.isEnabled(context)).andReturn(true);
         
         EasyMock.expect(context.getActiveSessionCount()).andReturn(2);
         
         context.addSessionListener(EasyMock.capture(capturedAddListener));
         
         EasyMock.expect(context.getActiveSessionCount()).andReturn(1).times(2,5);
         
         context.removeSessionListener(EasyMock.capture(capturedRemoveListener));
         
         EasyMock.expect(this.requestFactory.createStopRequest(context)).andReturn(stopRequest);
         EasyMock.expect(this.mcmpHandler.sendRequest(stopRequest)).andReturn(Collections.<MCMPServerState, String>emptyMap());
         
         EasyMock.replay(this.requestFactory, this.mcmpHandler, this.mcmpConfig, this.responseParser, server, engine, host, context, disableRequest, stopRequest, strategy);
         
         this.service.stop(context);
         
         EasyMock.verify(this.requestFactory, this.mcmpHandler, this.mcmpConfig, this.responseParser, server, engine, host, context, disableRequest, stopRequest, strategy);
         
         Assert.assertSame(capturedAddListener.getValue(), capturedRemoveListener.getValue());
         
         EasyMock.reset(this.requestFactory, this.mcmpHandler, this.mcmpConfig, this.responseParser, server, engine, host, context, disableRequest, stopRequest, strategy);
      }
      catch (InterruptedException e)
      {
         Thread.currentThread().interrupt();
         throw e;
      }
   }

   @Test
   public void stopDistributableContext() throws IOException, InterruptedException
   {
      Server server = EasyMock.createStrictMock(Server.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      Context context = EasyMock.createStrictMock(Context.class);
      
      final EnablableRequestListener listener = this.startContext(server, engine, host, context);

      MCMPRequest disableRequest = EasyMock.createStrictMock(MCMPRequest.class);
      MCMPRequest stopRequest = EasyMock.createStrictMock(MCMPRequest.class);
      MCMPServerState state = EasyMock.createStrictMock(MCMPServerState.class);
      SessionDrainingStrategy strategy = EasyMock.createStrictMock(SessionDrainingStrategy.class);
      
      // Test successful drain
      
      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(context.getPath()).andReturn("");
      EasyMock.expect(context.getHost()).andReturn(host);
      
      EasyMock.expect(this.requestFactory.createDisableRequest(context)).andReturn(disableRequest);
      EasyMock.expect(this.mcmpHandler.sendRequest(disableRequest)).andReturn(Collections.<MCMPServerState, String>emptyMap());
      
      EasyMock.expect(this.mcmpConfig.getStopContextTimeoutUnit()).andReturn(TimeUnit.SECONDS);
      EasyMock.expect(this.mcmpConfig.getStopContextTimeout()).andReturn(0L);
      EasyMock.expect(this.mcmpConfig.getSessionDrainingStrategy()).andReturn(strategy);
      EasyMock.expect(strategy.isEnabled(context)).andReturn(false);
      
      EasyMock.expect(this.requestFactory.createStopRequest(context)).andReturn(stopRequest);
      EasyMock.expect(this.mcmpHandler.sendRequest(stopRequest)).andReturn(Collections.singletonMap(state, "response"));
      EasyMock.expect(this.responseParser.parseStopAppResponse("response")).andReturn(2);
      
      EasyMock.expect(this.mcmpHandler.sendRequest(stopRequest)).andReturn(Collections.singletonMap(state, "response"));
      EasyMock.expect(this.responseParser.parseStopAppResponse("response")).andReturn(0);
      
      EasyMock.replay(this.requestFactory, this.mcmpHandler, this.mcmpConfig, this.responseParser, server, engine, host, context, disableRequest, stopRequest, strategy);
      
      Runnable task = new Runnable()
      {
         public void run()
         {
            while (!listener.isEnabled() && !Thread.currentThread().isInterrupted())
            {
               Thread.yield();
            }

            listener.requestDestroyed(null);
         }
      };
      
      Thread thread = new Thread(task);
      
      thread.start();
      
      this.service.stop(context);
      
      thread.interrupt();
      
      try
      {
         thread.join();
         
         EasyMock.verify(this.requestFactory, this.mcmpHandler, this.mcmpConfig, this.responseParser, server, engine, host, context, disableRequest, stopRequest, strategy);
         
         Assert.assertFalse(listener.isEnabled());
         
         EasyMock.reset(this.requestFactory, this.mcmpHandler, this.mcmpConfig, this.responseParser, server, engine, host, context, disableRequest, stopRequest, strategy);

         // Test drain timeout
         EasyMock.expect(context.getHost()).andReturn(host);
         EasyMock.expect(context.getPath()).andReturn("");
         EasyMock.expect(context.getHost()).andReturn(host);
         
         EasyMock.expect(this.requestFactory.createDisableRequest(context)).andReturn(disableRequest);
         EasyMock.expect(this.mcmpHandler.sendRequest(disableRequest)).andReturn(Collections.<MCMPServerState, String>emptyMap());
         
         EasyMock.expect(this.mcmpConfig.getStopContextTimeoutUnit()).andReturn(TimeUnit.SECONDS);
         EasyMock.expect(this.mcmpConfig.getStopContextTimeout()).andReturn(1L);
         EasyMock.expect(this.mcmpConfig.getSessionDrainingStrategy()).andReturn(strategy);
         EasyMock.expect(strategy.isEnabled(context)).andReturn(false);
         
         EasyMock.expect(this.requestFactory.createStopRequest(context)).andReturn(stopRequest);
         // In fact on windows it is often called 3 times...
         EasyMock.expect(this.mcmpHandler.sendRequest(stopRequest)).andReturn(Collections.singletonMap(state, "response")).times(2,5);
         EasyMock.expect(this.responseParser.parseStopAppResponse("response")).andReturn(1).times(2,5);
         
         EasyMock.replay(this.requestFactory, this.mcmpHandler, this.mcmpConfig, this.responseParser, server, engine, host, context, disableRequest, stopRequest, strategy);
         
         this.service.stop(context);
         
         EasyMock.verify(this.requestFactory, this.mcmpHandler, this.mcmpConfig, this.responseParser, server, engine, host, context, disableRequest, stopRequest, strategy);

         Assert.assertFalse(listener.isEnabled());
         
         EasyMock.reset(this.requestFactory, this.mcmpHandler, this.mcmpConfig, this.responseParser, server, engine, host, context, disableRequest, stopRequest, strategy);
      }
      catch (InterruptedException e)
      {
         Thread.currentThread().interrupt();
         throw e;
      }
   }
   
   @Test
   public void removeContextNoInit()
   {
      Context context = EasyMock.createStrictMock(Context.class);
      Host host = EasyMock.createStrictMock(Host.class);
      
      EasyMock.expect(context.getHost()).andReturn(host);
      
      EasyMock.replay(context);
      
      this.service.remove(context);

      EasyMock.verify(context);
   }

   @Test
   public void removeContextIgnored() throws IOException
   {
      Server server = EasyMock.createStrictMock(Server.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      
      this.establishConnection(server, engine, host, true);

      Context context = EasyMock.createStrictMock(Context.class);

      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(context.getPath()).andReturn("/excluded");
      
      EasyMock.replay(server, engine, host, context);
      
      this.service.remove(context);
      
      EasyMock.verify(server, engine, host, context);
      EasyMock.reset(server, engine, host, context);
   }
      
   @Test
   public void removeContext() throws IOException
   {
      Server server = EasyMock.createStrictMock(Server.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      
      this.establishConnection(server, engine, host, true);

      Context context = EasyMock.createStrictMock(Context.class);
      MCMPRequest request = EasyMock.createStrictMock(MCMPRequest.class);
      
      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(context.getPath()).andReturn("");
      EasyMock.expect(context.getHost()).andReturn(host);
      
      EasyMock.expect(this.requestFactory.createRemoveRequest(context)).andReturn(request);
      
      EasyMock.expect(this.mcmpHandler.sendRequest(request)).andReturn(Collections.<MCMPServerState, String>emptyMap());
      
      EasyMock.replay(this.mcmpHandler, this.requestFactory, server, engine, host, context);
      
      this.service.remove(context);
      
      EasyMock.verify(this.mcmpHandler, this.requestFactory, server, engine, host, context);
      EasyMock.reset(this.mcmpHandler, this.requestFactory, server, engine, host, context);
   }
   
   @Test
   public void statusNotEstablished()
   {
      Engine engine = EasyMock.createStrictMock(Engine.class);
      
      this.mcmpHandler.status();

      EasyMock.replay(engine, this.mcmpHandler);
      
      this.service.status(engine);

      EasyMock.verify(engine, this.mcmpHandler);
      EasyMock.reset(engine, this.mcmpHandler);
   }
      
   @Test
   public void status() throws IOException
   {
      Server server = EasyMock.createStrictMock(Server.class);
      
      this.establishConnection(server);
      
      Engine engine = EasyMock.createStrictMock(Engine.class);
      MCMPRequest request = EasyMock.createStrictMock(MCMPRequest.class);
      
      this.mcmpHandler.status();

      EasyMock.expect(engine.getProxyConnector()).andReturn(null);
      // EasyMock.expect(this.lbfProvider.getLoadBalanceFactor()).andReturn(10);
      EasyMock.expect(engine.getJvmRoute()).andReturn("host1");
      
      EasyMock.expect(this.requestFactory.createStatusRequest("host1", -1)).andReturn(request);
      
      EasyMock.expect(this.mcmpHandler.sendRequest(request)).andReturn(Collections.<MCMPServerState, String>emptyMap());
      
      EasyMock.replay(this.mcmpHandler, this.requestFactory, this.lbfProvider, engine);
      
      this.service.status(engine);
      
      EasyMock.verify(this.mcmpHandler, this.requestFactory, this.lbfProvider, engine);
      EasyMock.reset(this.mcmpHandler, this.requestFactory, this.lbfProvider, engine);
   }
   
   @Test
   public void startServerNotInit()
   {
      Server server = EasyMock.createStrictMock(Server.class);
      
      EasyMock.replay(server);
      
      this.service.start(server);
      
      EasyMock.verify(server);
      EasyMock.reset(server);
   }
   
   @Test
   public void startServerNotEstablished()
   {
      Server server = EasyMock.createStrictMock(Server.class);
      
      this.init(server);
      
      EasyMock.replay(server);
      
      this.service.start(server);
      
      EasyMock.verify(server);
      EasyMock.reset(server);
   }
      
   @Test
   public void startServer() throws Exception
   {
      Server server = EasyMock.createStrictMock(Server.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      
      this.establishConnection(server, engine, host, true);

      Context context = EasyMock.createStrictMock(Context.class);
      Context excludedContext = EasyMock.createStrictMock(Context.class);
      MCMPRequest configRequest = EasyMock.createStrictMock(MCMPRequest.class);
      MCMPRequest enableRequest = EasyMock.createStrictMock(MCMPRequest.class);
      
      EasyMock.expect(server.getEngines()).andReturn(Collections.singleton(engine));
      EasyMock.expect(this.requestFactory.createConfigRequest(engine, this.nodeConfig, this.balancerConfig)).andReturn(configRequest);
      
      EasyMock.expect(this.mcmpHandler.sendRequest(configRequest)).andReturn(Collections.<MCMPServerState, String>emptyMap());

      EasyMock.expect(engine.getHosts()).andReturn(Collections.singleton(host));
      EasyMock.expect(host.getContexts()).andReturn(Arrays.asList(context, excludedContext));
      
      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(context.getPath()).andReturn("");
      EasyMock.expect(context.isStarted()).andReturn(true);
      EasyMock.expect(context.getHost()).andReturn(host);
      
      EasyMock.expect(this.requestFactory.createEnableRequest(context)).andReturn(enableRequest);
      
      EasyMock.expect(this.mcmpHandler.sendRequest(enableRequest)).andReturn(Collections.<MCMPServerState, String>emptyMap());
      
      EasyMock.expect(excludedContext.getHost()).andReturn(host);
      EasyMock.expect(excludedContext.getPath()).andReturn("/excluded");
      
      EasyMock.replay(this.mcmpHandler, this.requestFactory, server, engine, host, context, excludedContext);
      
      this.service.start(server);
      
      EasyMock.verify(this.mcmpHandler, this.requestFactory, server, engine, host, context, excludedContext);
      EasyMock.reset(this.mcmpHandler, this.requestFactory, server, engine, host, context, excludedContext);
   }
   
   @Test
   public void stopServerNoInit()
   {
      Server server = EasyMock.createStrictMock(Server.class);
      
      EasyMock.replay(server);
      
      this.service.stop(server);
      
      EasyMock.verify(server);
      EasyMock.reset(server);
   }

   @Test
   public void stopServer() throws IOException
   {
      Server server = EasyMock.createStrictMock(Server.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      Context context = EasyMock.createStrictMock(Context.class);
      
      EnablableRequestListener listener = this.startContext(server, engine, host, context);

      Context excludedContext = EasyMock.createStrictMock(Context.class);
      MCMPRequest disableRequest = EasyMock.createStrictMock(MCMPRequest.class);
      MCMPRequest stopRequest = EasyMock.createStrictMock(MCMPRequest.class);
      MCMPRequest removeRrequest = EasyMock.createStrictMock(MCMPRequest.class);
      MCMPRequest removeAllRequest = EasyMock.createStrictMock(MCMPRequest.class);
      MCMPServerState state = EasyMock.createStrictMock(MCMPServerState.class);
      SessionDrainingStrategy strategy = EasyMock.createStrictMock(SessionDrainingStrategy.class);
      
      EasyMock.expect(server.getEngines()).andReturn(Collections.singleton(engine));
      EasyMock.expect(engine.getHosts()).andReturn(Collections.singleton(host));
      EasyMock.expect(host.getContexts()).andReturn(Arrays.asList(context, excludedContext));

      EasyMock.expect(context.isStarted()).andReturn(true);
      
      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(context.getPath()).andReturn("");
      EasyMock.expect(context.getHost()).andReturn(host);

      EasyMock.expect(this.requestFactory.createDisableRequest(context)).andReturn(disableRequest);
      EasyMock.expect(this.mcmpHandler.sendRequest(disableRequest)).andReturn(Collections.singletonMap(state, "response1"));
      
      EasyMock.expect(this.mcmpConfig.getStopContextTimeoutUnit()).andReturn(TimeUnit.SECONDS);
      EasyMock.expect(this.mcmpConfig.getStopContextTimeout()).andReturn(1L);
      EasyMock.expect(this.mcmpConfig.getSessionDrainingStrategy()).andReturn(strategy);
      EasyMock.expect(strategy.isEnabled(context)).andReturn(false);
      
      // Called at least once...
      EasyMock.expect(this.requestFactory.createStopRequest(context)).andReturn(stopRequest).atLeastOnce();
      EasyMock.expect(this.mcmpHandler.sendRequest(stopRequest)).andReturn(Collections.singletonMap(state, "response2")).atLeastOnce();
      EasyMock.expect(this.responseParser.parseStopAppResponse("response2")).andReturn(1).atLeastOnce();
      // EasyMock.expect(this.mcmpHandler.sendRequest(stopRequest)).andReturn(Collections.singletonMap(state, "response3")).atLeastOnce();
      // EasyMock.expect(this.responseParser.parseStopAppResponse("response3")).andReturn(1).atLeastOnce();

      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(context.getPath()).andReturn("");
      EasyMock.expect(context.getHost()).andReturn(host);
      
      EasyMock.expect(this.requestFactory.createRemoveRequest(context)).andReturn(removeRrequest);
      
      EasyMock.expect(this.mcmpHandler.sendRequest(removeRrequest)).andReturn(Collections.singletonMap(state, "response4"));

      context.removeRequestListener(EasyMock.same(listener));
      
      EasyMock.expect(excludedContext.isStarted()).andReturn(true);
      EasyMock.expect(excludedContext.getHost()).andReturn(host);
      EasyMock.expect(excludedContext.getPath()).andReturn("/excluded");
      EasyMock.expect(excludedContext.getHost()).andReturn(host);
      EasyMock.expect(excludedContext.getPath()).andReturn("/excluded");
      
      EasyMock.expect(this.requestFactory.createRemoveRequest(engine)).andReturn(removeAllRequest);
      
      EasyMock.expect(this.mcmpHandler.sendRequest(removeAllRequest)).andReturn(Collections.<MCMPServerState, String>emptyMap());
      
      EasyMock.replay(this.mcmpHandler, this.requestFactory, this.mcmpConfig, this.responseParser, server, engine, host, context, excludedContext, strategy);
      
      this.service.stop(server);
      
      EasyMock.verify(this.mcmpHandler, this.requestFactory, this.mcmpConfig, this.responseParser, server, engine, host, context, excludedContext, strategy);
      EasyMock.reset(this.mcmpHandler, this.requestFactory, this.mcmpConfig, this.responseParser, server, engine, host, context, excludedContext, strategy);
   }
   
   @Test
   public void stopGracefully() throws Exception
   {
      Server server = EasyMock.createStrictMock(Server.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      
      this.establishConnection(server, engine, host, true);

      Context context = EasyMock.createStrictMock(Context.class);
      MCMPRequest disableRequest = EasyMock.createStrictMock(MCMPRequest.class);
      MCMPRequest stopRequest = EasyMock.createStrictMock(MCMPRequest.class);
      final Capture<HttpSessionListener> capturedAddListener = new Capture<HttpSessionListener>();
      Capture<HttpSessionListener> capturedRemoveListener = new Capture<HttpSessionListener>();
      
      // Test successful drain
      
      EasyMock.expect(server.getEngines()).andReturn(Collections.singleton(engine));
      EasyMock.expect(this.requestFactory.createDisableRequest(engine)).andReturn(disableRequest);
      EasyMock.expect(this.mcmpHandler.sendRequest(disableRequest)).andReturn(Collections.<MCMPServerState, String>emptyMap());
      
      EasyMock.expect(server.getEngines()).andReturn(Collections.singleton(engine));
      EasyMock.expect(engine.getHosts()).andReturn(Collections.singleton(host));
      EasyMock.expect(host.getContexts()).andReturn(Collections.singleton(context));
      
      EasyMock.expect(context.getActiveSessionCount()).andReturn(2);
      
      context.addSessionListener(EasyMock.capture(capturedAddListener));
      
      EasyMock.expect(context.getActiveSessionCount()).andReturn(1);
      
      try
      {
         EasyMock.expect(context.getActiveSessionCount()).andReturn(0);

         context.removeSessionListener(EasyMock.capture(capturedRemoveListener));
         
         EasyMock.expect(server.getEngines()).andReturn(Collections.singleton(engine));
         EasyMock.expect(this.requestFactory.createStopRequest(engine)).andReturn(stopRequest);
         EasyMock.expect(this.mcmpHandler.sendRequest(stopRequest)).andReturn(Collections.<MCMPServerState, String>emptyMap());
         
         EasyMock.replay(this.requestFactory, this.mcmpHandler, server, engine, host, context);
         
         Runnable task = new Runnable()
         {
            public void run()
            {
               while (!capturedAddListener.hasCaptured() && !Thread.currentThread().isInterrupted())
               {
                  Thread.yield();
               }

               capturedAddListener.getValue().sessionDestroyed(null);
            }
         };
         
         Thread thread = new Thread(task);
         
         thread.start();
         
         boolean result = this.service.stop(0, TimeUnit.SECONDS);
         
         thread.interrupt();
         thread.join();
         
         EasyMock.verify(this.requestFactory, this.mcmpHandler, server, engine, host, context);
         
         Assert.assertTrue(result);
         Assert.assertSame(capturedAddListener.getValue(), capturedRemoveListener.getValue());
         
         EasyMock.reset(this.requestFactory, this.mcmpHandler, server, engine, host, context);
         capturedAddListener.reset();
         capturedRemoveListener.reset();
         
         // Test timed out drain
         
         EasyMock.expect(server.getEngines()).andReturn(Collections.singleton(engine));
         EasyMock.expect(this.requestFactory.createDisableRequest(engine)).andReturn(disableRequest);
         EasyMock.expect(this.mcmpHandler.sendRequest(disableRequest)).andReturn(Collections.<MCMPServerState, String>emptyMap());
         
         EasyMock.expect(server.getEngines()).andReturn(Collections.singleton(engine));
         EasyMock.expect(engine.getHosts()).andReturn(Collections.singleton(host));
         EasyMock.expect(host.getContexts()).andReturn(Collections.singleton(context));
         
         EasyMock.expect(context.getActiveSessionCount()).andReturn(2);
         
         context.addSessionListener(EasyMock.capture(capturedAddListener));
         
         EasyMock.expect(context.getActiveSessionCount()).andReturn(1).times(1,5);
         
         context.removeSessionListener(EasyMock.capture(capturedRemoveListener));
         
         EasyMock.replay(this.requestFactory, this.mcmpHandler, server, engine, host, context);
         
         result = this.service.stop(1, TimeUnit.SECONDS);
         
         EasyMock.verify(this.requestFactory, this.mcmpHandler, server, engine, host, context);
         
         Assert.assertFalse(result);
         Assert.assertSame(capturedAddListener.getValue(), capturedRemoveListener.getValue());
         
         EasyMock.reset(this.requestFactory, this.mcmpHandler, server, engine, host, context);
      }
      catch (InterruptedException e)
      {
         Thread.currentThread().interrupt();
         throw e;
      }
   }
   
   @Test
   public void stopContextGracefully() throws Exception
   {
      String hostName = "host";
      String contextPath = "path";
      
      Server server = EasyMock.createStrictMock(Server.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      
      this.establishConnection(server, engine, host, true);

      Context context = EasyMock.createStrictMock(Context.class);
      MCMPRequest disableRequest = EasyMock.createStrictMock(MCMPRequest.class);
      MCMPRequest stopRequest = EasyMock.createStrictMock(MCMPRequest.class);
      final Capture<HttpSessionListener> capturedAddListener = new Capture<HttpSessionListener>();
      Capture<HttpSessionListener> capturedRemoveListener = new Capture<HttpSessionListener>();
      
      // Test successful drain
      
      EasyMock.expect(server.getEngines()).andReturn(Collections.singleton(engine));
      EasyMock.expect(engine.findHost(hostName)).andReturn(host);
      EasyMock.expect(host.findContext(contextPath)).andReturn(context);
      EasyMock.expect(this.requestFactory.createDisableRequest(context)).andReturn(disableRequest);
      EasyMock.expect(this.mcmpHandler.sendRequest(disableRequest)).andReturn(Collections.<MCMPServerState, String>emptyMap());
      
      EasyMock.expect(context.getActiveSessionCount()).andReturn(2);
      
      context.addSessionListener(EasyMock.capture(capturedAddListener));
      
      EasyMock.expect(context.getActiveSessionCount()).andReturn(1);
      
      try
      {
         EasyMock.expect(context.getActiveSessionCount()).andReturn(0);

         context.removeSessionListener(EasyMock.capture(capturedRemoveListener));
         
         EasyMock.expect(this.requestFactory.createStopRequest(context)).andReturn(stopRequest);
         EasyMock.expect(this.mcmpHandler.sendRequest(stopRequest)).andReturn(Collections.<MCMPServerState, String>emptyMap());
         
         EasyMock.replay(this.requestFactory, this.mcmpHandler, server, engine, host, context);
         
         Runnable task = new Runnable()
         {
            public void run()
            {
               while (!capturedAddListener.hasCaptured() && !Thread.currentThread().isInterrupted())
               {
                  Thread.yield();
               }

               capturedAddListener.getValue().sessionDestroyed(null);
            }
         };
         
         Thread thread = new Thread(task);
         
         thread.start();
         
         boolean result = this.service.stopContext(hostName, contextPath, 0, TimeUnit.SECONDS);
         
         thread.interrupt();
         thread.join();
         
         EasyMock.verify(this.requestFactory, this.mcmpHandler, server, engine, host, context);
         
         Assert.assertTrue(result);
         Assert.assertSame(capturedAddListener.getValue(), capturedRemoveListener.getValue());
         
         EasyMock.reset(this.requestFactory, this.mcmpHandler, server, engine, host, context);
         capturedAddListener.reset();
         capturedRemoveListener.reset();
         
         // Test timed out drain
         
         EasyMock.expect(server.getEngines()).andReturn(Collections.singleton(engine));
         EasyMock.expect(engine.findHost(hostName)).andReturn(host);
         EasyMock.expect(host.findContext(contextPath)).andReturn(context);
         EasyMock.expect(this.requestFactory.createDisableRequest(context)).andReturn(disableRequest);
         EasyMock.expect(this.mcmpHandler.sendRequest(disableRequest)).andReturn(Collections.<MCMPServerState, String>emptyMap());
         
         EasyMock.expect(context.getActiveSessionCount()).andReturn(2);
         
         context.addSessionListener(EasyMock.capture(capturedAddListener));
         
         EasyMock.expect(context.getActiveSessionCount()).andReturn(1).times(2,5);
         
         context.removeSessionListener(EasyMock.capture(capturedRemoveListener));
         
         EasyMock.replay(this.requestFactory, this.mcmpHandler, server, engine, host, context);
         
         result = this.service.stopContext(hostName, contextPath, 1, TimeUnit.SECONDS);
         
         EasyMock.verify(this.requestFactory, this.mcmpHandler, server, engine, host, context);
         
         Assert.assertFalse(result);
         Assert.assertSame(capturedAddListener.getValue(), capturedRemoveListener.getValue());
         
         EasyMock.reset(this.requestFactory, this.mcmpHandler, server, engine, host, context);
      }
      catch (InterruptedException e)
      {
         Thread.currentThread().interrupt();
         throw e;
      }
   }
}
