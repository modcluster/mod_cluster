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
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpSessionListener;

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
      InetAddress localAddress = this.getLocalAddress();
      String localHostName = localAddress.getHostName();
      
      // Test advertise = null, proxies configured
      EasyMock.expect(this.mcmpConfig.getProxyList()).andReturn(localHostName);
      
      this.mcmpHandler.init(Collections.singletonList(new InetSocketAddress(localAddress, 8000)));
      
      EasyMock.expect(this.mcmpConfig.getExcludedContexts()).andReturn("ignored");
      
      this.source.init(server, Collections.singletonMap("localhost", Collections.singleton("/ignored")));
      
      EasyMock.expect(this.lbfProviderFactory.createLoadBalanceFactorProvider()).andReturn(this.lbfProvider);
      
      EasyMock.expect(this.mcmpConfig.getAdvertise()).andReturn(null);
      
      EasyMock.replay(server, this.mcmpHandler, this.mcmpConfig, this.lbfProviderFactory);
      
      this.service.init(server);
      
      EasyMock.verify(server, this.mcmpHandler, this.mcmpConfig, this.lbfProviderFactory);
      EasyMock.reset(server, this.mcmpHandler, this.mcmpConfig, this.lbfProviderFactory);
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
      InetSocketAddress address = InetSocketAddress.createUnresolved("localhost", 8080);
      String configuration = "config";
      MCMPServerState state = EasyMock.createStrictMock(MCMPServerState.class);
      MCMPRequest request = EasyMock.createStrictMock(MCMPRequest.class);
      
      EasyMock.expect(this.requestFactory.createDumpRequest()).andReturn(request);
      EasyMock.expect(this.mcmpHandler.sendRequest(request)).andReturn(Collections.singletonMap(state, configuration));
      EasyMock.expect(state.getSocketAddress()).andReturn(address);
      
      EasyMock.replay(this.mcmpHandler, this.requestFactory, state, request);
      
      Map<InetSocketAddress, String> result = this.service.getProxyConfiguration();
      
      EasyMock.verify(this.mcmpHandler, this.requestFactory, state, request);
      
      Assert.assertEquals(1, result.size());
      Assert.assertSame(configuration, result.get(address));
      
      EasyMock.reset(this.mcmpHandler, this.requestFactory, state, request);
   }
   
   @Test
   public void getProxyInfo()
   {
      InetSocketAddress address = InetSocketAddress.createUnresolved("localhost", 8080);
      String information = "info";
      MCMPServerState state = EasyMock.createStrictMock(MCMPServerState.class);
      MCMPRequest request = EasyMock.createStrictMock(MCMPRequest.class);
      
      EasyMock.expect(this.requestFactory.createInfoRequest()).andReturn(request);
      EasyMock.expect(this.mcmpHandler.sendRequest(request)).andReturn(Collections.singletonMap(state, information));
      EasyMock.expect(state.getSocketAddress()).andReturn(address);
      
      EasyMock.replay(this.mcmpHandler, this.requestFactory, state, request);
      
      Map<InetSocketAddress, String> result = this.service.getProxyInfo();
      
      EasyMock.verify(this.mcmpHandler, this.requestFactory, state, request);
      
      Assert.assertEquals(1, result.size());
      Assert.assertSame(information, result.get(address));
      
      EasyMock.reset(this.mcmpHandler, this.requestFactory, state, request);
   }
   
   @Test
   public void ping()
   {
      InetSocketAddress address = InetSocketAddress.createUnresolved("localhost", 8080);
      String pingResult = "OK";
      
      MCMPServerState state = EasyMock.createStrictMock(MCMPServerState.class);
      MCMPRequest request = EasyMock.createStrictMock(MCMPRequest.class);
      
      // Test no parameter
      EasyMock.expect(this.requestFactory.createPingRequest()).andReturn(request);
      EasyMock.expect(this.mcmpHandler.sendRequest(request)).andReturn(Collections.singletonMap(state, pingResult));
      EasyMock.expect(state.getSocketAddress()).andReturn(address);
      
      EasyMock.replay(this.mcmpHandler, this.requestFactory, state, request);
      
      Map<InetSocketAddress, String> result = this.service.ping();
      
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
      this.mcmpHandler.reset();
      
      EasyMock.replay(this.mcmpHandler);
      
      this.service.reset();
      
      EasyMock.verify(this.mcmpHandler);
      EasyMock.reset(this.mcmpHandler);
   }
   
   @Test
   public void refresh()
   {
      this.mcmpHandler.markProxiesInError();
      
      EasyMock.replay(this.mcmpHandler);
      
      this.service.refresh();
      
      EasyMock.verify(this.mcmpHandler);
      EasyMock.reset(this.mcmpHandler);
   }
   
   @Test
   public void enable()
   {
      Server server = EasyMock.createStrictMock(Server.class);
      
      this.init(server);
      
      Engine engine = EasyMock.createStrictMock(Engine.class);
      MCMPRequest request = EasyMock.createMock(MCMPRequest.class);
      
      EasyMock.expect(server.getEngines()).andReturn(Collections.singleton(engine));

      EasyMock.expect(this.requestFactory.createEnableRequest(engine)).andReturn(request);
      
      EasyMock.expect(this.mcmpHandler.sendRequest(request)).andReturn(null);
      
      EasyMock.expect(this.mcmpHandler.isProxyHealthOK()).andReturn(true);
      
      EasyMock.replay(server, this.requestFactory, this.mcmpHandler, engine);
      
      boolean result = this.service.enable();
      
      EasyMock.verify(server, this.requestFactory, this.mcmpHandler, engine);
      
      Assert.assertTrue(result);
      
      EasyMock.reset(server, this.requestFactory, this.mcmpHandler, engine);
   }
   
   @SuppressWarnings("boxing")
   @Test
   public void disable()
   {
      Server server = EasyMock.createStrictMock(Server.class);
      
      this.init(server);
      
      Engine engine = EasyMock.createStrictMock(Engine.class);
      MCMPRequest request = EasyMock.createMock(MCMPRequest.class);
      
      EasyMock.expect(server.getEngines()).andReturn(Collections.singleton(engine));

      EasyMock.expect(this.requestFactory.createDisableRequest(engine)).andReturn(request);
      
      EasyMock.expect(this.mcmpHandler.sendRequest(request)).andReturn(null);
      
      EasyMock.expect(this.mcmpHandler.isProxyHealthOK()).andReturn(true);
      
      EasyMock.replay(server, this.requestFactory, this.mcmpHandler, engine);
      
      boolean result = this.service.disable();
      
      EasyMock.verify(server, this.requestFactory, this.mcmpHandler, engine);
      
      Assert.assertTrue(result);
      
      EasyMock.reset(server, this.requestFactory, this.mcmpHandler, engine);
   }
   
   @Test
   public void enableContext()
   {
      Server server = EasyMock.createStrictMock(Server.class);
      
      this.init(server);
      
      String hostName = "host1";
      String path = "/";
      
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
      
      boolean result = this.service.enableContext(hostName, path);
      
      EasyMock.verify(server, this.requestFactory, this.mcmpHandler, engine, host, context);
      
      Assert.assertTrue(result);
      
      EasyMock.reset(server, this.requestFactory, this.mcmpHandler, engine, host, context);
   }
   
   @Test
   public void disableContext()
   {
      Server server = EasyMock.createStrictMock(Server.class);
      
      this.init(server);
      
      String hostName = "host1";
      String path = "/";
      
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
      
      boolean result = this.service.disableContext(hostName, path);
      
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
      
      this.mcmpHandler.init(Collections.singletonList(new InetSocketAddress(localAddress, 8000)));
      
      EasyMock.expect(this.mcmpConfig.getExcludedContexts()).andReturn(null);
      
      this.source.init(server, Collections.<String, Set<String>>emptyMap());
      
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
      
      this.mcmpHandler.init(Collections.singletonList(new InetSocketAddress(localAddress, 8000)));
      
      EasyMock.expect(this.mcmpConfig.getExcludedContexts()).andReturn("");
      
      this.source.init(server, Collections.<String, Set<String>>emptyMap());
      
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
   
   private InetAddress getLocalAddress()
   {
      try
      {
         return InetAddress.getLocalHost();
      }
      catch (UnknownHostException e)
      {
         throw new IllegalStateException(e);
      }
   }
   
   @Test
   public void initNoProxies() throws IOException
   {
      Server server = EasyMock.createStrictMock(Server.class);
      
      // Test advertise = null, no proxies configured
      EasyMock.expect(this.mcmpConfig.getProxyList()).andReturn(null);
      Capture<Map<String, Set<String>>> capturedMap = new Capture<Map<String, Set<String>>>();
      
      this.mcmpHandler.init(Collections.<InetSocketAddress>emptyList());
      
      EasyMock.expect(this.mcmpConfig.getExcludedContexts()).andReturn("host1:ignored,ROOT");
      
      this.source.init(EasyMock.same(server), EasyMock.capture(capturedMap));
      
      EasyMock.expect(this.lbfProviderFactory.createLoadBalanceFactorProvider()).andReturn(this.lbfProvider);
      
      EasyMock.expect(this.mcmpConfig.getAdvertise()).andReturn(null);
      
      EasyMock.expect(this.advertiseListenerFactory.createListener(this.mcmpHandler, this.mcmpConfig)).andReturn(this.advertiseListener);
      
      this.advertiseListener.start();
      
      EasyMock.replay(server, this.mcmpHandler, this.advertiseListenerFactory, this.mcmpConfig, this.lbfProviderFactory, this.advertiseListener, this.source);
      
      this.service.init(server);
      
      EasyMock.verify(server, this.mcmpHandler, this.advertiseListenerFactory, this.mcmpConfig, this.lbfProviderFactory, this.advertiseListener, this.source);
      
      Map<String, Set<String>> contexts = capturedMap.getValue();
      
      Assert.assertEquals(2, contexts.size());
      Assert.assertEquals(Collections.singleton("/ignored"), contexts.get("host1"));
      Assert.assertEquals(Collections.singleton(""), contexts.get("localhost"));
      
      EasyMock.reset(server, this.mcmpHandler, this.advertiseListenerFactory, this.mcmpConfig, this.lbfProviderFactory, this.advertiseListener, this.source);
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
   public void addContextNotInit()
   {
      Context context = EasyMock.createStrictMock(Context.class);
      
      EasyMock.replay(context);
      
      // Test not initialized
      try
      {
         this.service.add(context);
         
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
      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("localhost");
      EasyMock.expect(context.getPath()).andReturn("/ignored");
      
      EasyMock.replay(context, host);
      
      this.service.add(context);
      
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
      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("host1");
      
      EasyMock.expect(context.isStarted()).andReturn(false);
      
      EasyMock.replay(context, host);
      
      this.service.add(context);
      
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
      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("localhost");
      EasyMock.expect(context.getPath()).andReturn("/context");
      
      EasyMock.expect(context.isStarted()).andReturn(true);

      EasyMock.expect(context.getHost()).andReturn(host);
      
      EasyMock.expect(this.requestFactory.createEnableRequest(context)).andReturn(request);
      
      EasyMock.expect(this.mcmpHandler.sendRequest(request)).andReturn(Collections.<MCMPServerState, String>emptyMap());
      
      EasyMock.replay(this.mcmpHandler, this.requestFactory, context, host);
      
      this.service.add(context);
      
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
         this.service.start(context);
         
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
      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("localhost");
      EasyMock.expect(context.getPath()).andReturn("/ignored");
      
      EasyMock.replay(context, host);
      
      this.service.start(context);
      
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
      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("localhost");
      EasyMock.expect(context.getPath()).andReturn("/context");

      EasyMock.expect(context.getHost()).andReturn(host);
      
      EasyMock.expect(this.requestFactory.createEnableRequest(context)).andReturn(request);
      
      EasyMock.expect(this.mcmpHandler.sendRequest(request)).andReturn(Collections.<MCMPServerState, String>emptyMap());
      
      EasyMock.replay(this.mcmpHandler, this.requestFactory, context, host);
      
      this.service.start(context);
      
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
         this.service.stop(context);
         
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
      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("localhost");
      EasyMock.expect(context.getPath()).andReturn("/ignored");
      
      EasyMock.replay(context, host);
      
      this.service.stop(context);
      
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
      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("localhost");
      EasyMock.expect(context.getPath()).andReturn("/context");

      EasyMock.expect(context.getHost()).andReturn(host);
      
      EasyMock.expect(this.requestFactory.createStopRequest(context)).andReturn(request);
      
      EasyMock.expect(this.mcmpHandler.sendRequest(request)).andReturn(Collections.<MCMPServerState, String>emptyMap());
      
      EasyMock.replay(this.mcmpHandler, this.requestFactory, context, host);
      
      this.service.stop(context);
      
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
         this.service.remove(context);
         
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
      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("localhost");
      EasyMock.expect(context.getPath()).andReturn("/ignored");
      
      EasyMock.replay(context, host);
      
      this.service.remove(context);
      
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
      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("localhost");
      EasyMock.expect(context.getPath()).andReturn("/context");

      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(host.getEngine()).andReturn(engine);
      EasyMock.expect(engine.getJvmRoute()).andReturn(null);
      
      EasyMock.replay(context, host, engine);
      
      this.service.remove(context);
      
      EasyMock.verify(context, host, engine);
   }
      
   @Test
   public void removeContext() throws IOException
   {
      this.init();

      Context context = EasyMock.createStrictMock(Context.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      MCMPRequest request = EasyMock.createStrictMock(MCMPRequest.class);
      
      // Exclusion check
      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("localhost");
      EasyMock.expect(context.getPath()).andReturn("/context");
      
      // jvm route null check
      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(host.getEngine()).andReturn(engine);
      EasyMock.expect(engine.getJvmRoute()).andReturn("host1");
      
      EasyMock.expect(this.requestFactory.createRemoveRequest(context)).andReturn(request);
      
      EasyMock.expect(this.mcmpHandler.sendRequest(request)).andReturn(Collections.<MCMPServerState, String>emptyMap());
      
      EasyMock.replay(this.mcmpHandler, this.requestFactory, context, engine, host);
      
      this.service.remove(context);
      
      EasyMock.verify(this.mcmpHandler, this.requestFactory, context, engine, host);
   }
   
   @Test
   public void statusNoInit()
   {
      Engine engine = EasyMock.createStrictMock(Engine.class);

      EasyMock.replay(engine);
      
      // Test not initialized
      try
      {
         this.service.status(engine);
         
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
      
      this.mcmpHandler.status();
      
      EasyMock.expect(this.lbfProvider.getLoadBalanceFactor()).andReturn(10);
      EasyMock.expect(engine.getJvmRoute()).andReturn("host1");
      
      EasyMock.expect(this.requestFactory.createStatusRequest("host1", 10)).andReturn(request);
      
      EasyMock.expect(this.mcmpHandler.sendRequest(request)).andReturn(Collections.<MCMPServerState, String>emptyMap());
      
      EasyMock.replay(this.mcmpHandler, this.requestFactory, this.lbfProvider, engine);
      
      this.service.status(engine);
      
      EasyMock.verify(this.mcmpHandler, this.requestFactory, this.lbfProvider, engine);
      EasyMock.reset(this.mcmpHandler, this.requestFactory, this.lbfProvider, engine);
   }
   
   @Test
   public void startServerNoInit()
   {
      Server server = EasyMock.createStrictMock(Server.class);
      
      EasyMock.replay(server);
      
      // Test not initialized
      try
      {
         this.service.start(server);
         
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
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      Context context = EasyMock.createStrictMock(Context.class);
      MCMPRequest request = EasyMock.createStrictMock(MCMPRequest.class);
      Connector connector = EasyMock.createStrictMock(Connector.class);
      InetAddress address = InetAddress.getByName("127.0.0.1");
      
      this.init(server);
      
      // Test initialized
      EasyMock.expect(server.getEngines()).andReturn(Collections.singleton(engine));

      EasyMock.expect(engine.getProxyConnector()).andReturn(connector);
      EasyMock.expect(connector.getAddress()).andReturn(InetAddress.getByName("0.0.0.0"));
      EasyMock.expect(this.mcmpHandler.getLocalAddress()).andReturn(address);
      
      connector.setAddress(address);
      
      EasyMock.expect(engine.getJvmRoute()).andReturn(null);
      EasyMock.expect(engine.getProxyConnector()).andReturn(connector);
      EasyMock.expect(connector.getAddress()).andReturn(address);
      EasyMock.expect(connector.getPort()).andReturn(6666);
      EasyMock.expect(engine.getName()).andReturn("engine");
      
      engine.setJvmRoute("127.0.0.1:6666:engine");
      
      EasyMock.expect(this.requestFactory.createConfigRequest(engine, this.nodeConfig, this.balancerConfig)).andReturn(request);
      
      EasyMock.expect(this.mcmpHandler.sendRequest(request)).andReturn(Collections.<MCMPServerState, String>emptyMap());

      EasyMock.expect(engine.getHosts()).andReturn(Collections.singleton(host));
      EasyMock.expect(host.getContexts()).andReturn(Collections.singleton(context));

      // Exclusion check
      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("localhost");
      EasyMock.expect(context.getPath()).andReturn("/context");
      
      EasyMock.expect(context.isStarted()).andReturn(false);
      
      EasyMock.replay(server, this.mcmpHandler, this.requestFactory, engine, host, context, connector);
      
      this.service.start(server);
      
      EasyMock.verify(server, this.mcmpHandler, this.requestFactory, engine, host, context, connector);
      EasyMock.reset(server, this.mcmpHandler, this.requestFactory, engine, host, context, connector);
   }
   
   @Test
   public void stopServerNoInit()
   {
      Server server = EasyMock.createStrictMock(Server.class);
      
      EasyMock.replay(server);
      
      // Test not initialized
      try
      {
         this.service.stop(server);
         
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
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      Context context = EasyMock.createStrictMock(Context.class);
      MCMPRequest contextRequest = EasyMock.createStrictMock(MCMPRequest.class);
      MCMPRequest engineRequest = EasyMock.createStrictMock(MCMPRequest.class);
      
      this.init(server);
      
      EasyMock.expect(server.getEngines()).andReturn(Collections.singleton(engine));

      EasyMock.expect(engine.getHosts()).andReturn(Collections.singleton(host));
      EasyMock.expect(host.getContexts()).andReturn(Collections.singleton(context));
      EasyMock.expect(context.isStarted()).andReturn(false);

      // Exclusion check
      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(host.getName()).andReturn("localhost");
      EasyMock.expect(context.getPath()).andReturn("/context");
      
      // jvm route null check
      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(host.getEngine()).andReturn(engine);
      EasyMock.expect(engine.getJvmRoute()).andReturn("host1");
      
      EasyMock.expect(this.requestFactory.createRemoveRequest(context)).andReturn(contextRequest);
      
      EasyMock.expect(this.mcmpHandler.sendRequest(contextRequest)).andReturn(Collections.<MCMPServerState, String>emptyMap());
      
      EasyMock.expect(engine.getJvmRoute()).andReturn("host1");
      
      EasyMock.expect(this.requestFactory.createRemoveRequest(engine)).andReturn(engineRequest);
      
      EasyMock.expect(this.mcmpHandler.sendRequest(engineRequest)).andReturn(Collections.<MCMPServerState, String>emptyMap());
      
      EasyMock.replay(server, this.mcmpHandler, this.requestFactory, engine, host, context);
      
      this.service.stop(server);
      
      EasyMock.verify(server, this.mcmpHandler, this.requestFactory, engine, host, context);
      EasyMock.reset(server, this.mcmpHandler, this.requestFactory, engine, host, context);
   }
   
   @Test
   public void gracefulStop() throws Exception
   {
      Server server = EasyMock.createStrictMock(Server.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      final Context context = EasyMock.createStrictMock(Context.class);
      MCMPRequest disableRequest = EasyMock.createStrictMock(MCMPRequest.class);
      MCMPRequest stopRequest = EasyMock.createStrictMock(MCMPRequest.class);
      final Capture<HttpSessionListener> capturedAddListener = new Capture<HttpSessionListener>();
      Capture<HttpSessionListener> capturedRemoveListener = new Capture<HttpSessionListener>();
      
      this.init(server);
      
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
         
         EasyMock.expect(context.getActiveSessionCount()).andReturn(1).times(2);
         
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
   public void gracefulStopContext() throws Exception
   {
      String hostName = "host";
      String contextPath = "path";
      
      Server server = EasyMock.createStrictMock(Server.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      final Context context = EasyMock.createStrictMock(Context.class);
      MCMPRequest disableRequest = EasyMock.createStrictMock(MCMPRequest.class);
      MCMPRequest stopRequest = EasyMock.createStrictMock(MCMPRequest.class);
      final Capture<HttpSessionListener> capturedAddListener = new Capture<HttpSessionListener>();
      Capture<HttpSessionListener> capturedRemoveListener = new Capture<HttpSessionListener>();
      
      this.init(server);
      
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
         
         EasyMock.expect(context.getActiveSessionCount()).andReturn(1).times(2);
         
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
