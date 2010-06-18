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
package org.jboss.modcluster.mcmp;

import java.net.InetAddress;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.TreeSet;

import org.easymock.EasyMock;
import org.jboss.modcluster.Connector;
import org.jboss.modcluster.Context;
import org.jboss.modcluster.Engine;
import org.jboss.modcluster.Host;
import org.jboss.modcluster.config.BalancerConfiguration;
import org.jboss.modcluster.config.NodeConfiguration;
import org.jboss.modcluster.mcmp.impl.DefaultMCMPRequestFactory;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
public class DefaultMCMPRequestFactoryTestCase
{
   private MCMPRequestFactory factory = new DefaultMCMPRequestFactory();
   
   @Test
   public void createEnableRequestContext()
   {
      Context context = EasyMock.createStrictMock(Context.class);
      Host host = EasyMock.createStrictMock(Host.class);      
      Engine engine = EasyMock.createStrictMock(Engine.class);
      
      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(host.getEngine()).andReturn(engine);
      EasyMock.expect(engine.getJvmRoute()).andReturn("host1");
      EasyMock.expect(host.getAliases()).andReturn(new TreeSet<String>(Arrays.asList("alias1", "alias2")));
      EasyMock.expect(context.getPath()).andReturn("/context");
      
      EasyMock.replay(context, host, engine);
      
      MCMPRequest request = this.factory.createEnableRequest(context);
      
      EasyMock.verify(context, host, engine);
      
      Assert.assertSame(MCMPRequestType.ENABLE_APP, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertEquals("host1", request.getJvmRoute());
      
      Map<String, String> parameters = request.getParameters();
      
      Assert.assertEquals(2, parameters.size());
      
      Assert.assertEquals("/context", parameters.get("Context"));
      Assert.assertEquals("alias1,alias2", parameters.get("Alias"));
   }
   
   @Test
   public void createDisableRequestContext()
   {
      Context context = EasyMock.createStrictMock(Context.class);
      Host host = EasyMock.createStrictMock(Host.class);      
      Engine engine = EasyMock.createStrictMock(Engine.class);
      
      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(host.getEngine()).andReturn(engine);
      EasyMock.expect(engine.getJvmRoute()).andReturn("host1");
      EasyMock.expect(host.getAliases()).andReturn(new TreeSet<String>(Arrays.asList("alias1", "alias2")));
      EasyMock.expect(context.getPath()).andReturn("/context");
      
      EasyMock.replay(context, host, engine);
      
      MCMPRequest request = this.factory.createDisableRequest(context);
      
      EasyMock.verify(context, host, engine);
      
      Assert.assertSame(MCMPRequestType.DISABLE_APP, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertEquals("host1", request.getJvmRoute());
      
      Map<String, String> parameters = request.getParameters();
      
      Assert.assertEquals(2, parameters.size());
      
      Assert.assertEquals("/context", parameters.get("Context"));
      Assert.assertEquals("alias1,alias2", parameters.get("Alias"));
   }
   
   @Test
   public void createStopRequest()
   {
      Context context = EasyMock.createStrictMock(Context.class);
      Host host = EasyMock.createStrictMock(Host.class);      
      Engine engine = EasyMock.createStrictMock(Engine.class);
      
      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(host.getEngine()).andReturn(engine);
      EasyMock.expect(engine.getJvmRoute()).andReturn("host1");
      EasyMock.expect(host.getAliases()).andReturn(new TreeSet<String>(Arrays.asList("alias1", "alias2")));
      EasyMock.expect(context.getPath()).andReturn("/context");
      
      EasyMock.replay(context, host, engine);
      
      MCMPRequest request = this.factory.createStopRequest(context);
      
      EasyMock.verify(context, host, engine);
      
      Assert.assertSame(MCMPRequestType.STOP_APP, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertEquals("host1", request.getJvmRoute());
      
      Map<String, String> parameters = request.getParameters();
      
      Assert.assertEquals(2, parameters.size());
      
      Assert.assertEquals("/context", parameters.get("Context"));
      Assert.assertEquals("alias1,alias2", parameters.get("Alias"));
   }
   
   @Test
   public void createRemoveRequestContext()
   {
      Context context = EasyMock.createStrictMock(Context.class);
      Host host = EasyMock.createStrictMock(Host.class);      
      Engine engine = EasyMock.createStrictMock(Engine.class);
      
      EasyMock.expect(context.getHost()).andReturn(host);
      EasyMock.expect(host.getEngine()).andReturn(engine);
      EasyMock.expect(engine.getJvmRoute()).andReturn("host1");
      EasyMock.expect(host.getAliases()).andReturn(new TreeSet<String>(Arrays.asList("alias1", "alias2")));
      EasyMock.expect(context.getPath()).andReturn("/context");
      
      EasyMock.replay(context, host, engine);
      
      MCMPRequest request = this.factory.createRemoveRequest(context);
      
      EasyMock.verify(context, host, engine);
      
      Assert.assertSame(MCMPRequestType.REMOVE_APP, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertEquals("host1", request.getJvmRoute());
      
      Map<String, String> parameters = request.getParameters();
      
      Assert.assertEquals(2, parameters.size());
      
      Assert.assertEquals("/context", parameters.get("Context"));
      Assert.assertEquals("alias1,alias2", parameters.get("Alias"));
   }
   
   @Test
   public void createStatusRequest()
   {
      MCMPRequest request = this.factory.createStatusRequest("route", 10);
      
      Assert.assertSame(MCMPRequestType.STATUS, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertEquals("route", request.getJvmRoute());
      
      Map<String, String> parameters = request.getParameters();
      
      Assert.assertEquals(1, parameters.size());
      Assert.assertEquals("10", parameters.get("Load"));
   }
   
   @Test
   public void createConfigRequest() throws Exception
   {
      Engine engine = EasyMock.createStrictMock(Engine.class);
      NodeConfiguration nodeConfig = EasyMock.createStrictMock(NodeConfiguration.class);
      BalancerConfiguration balancerConfig = EasyMock.createStrictMock(BalancerConfiguration.class);
      Connector connector = EasyMock.createStrictMock(Connector.class);
      
      EasyMock.expect(engine.getProxyConnector()).andReturn(connector);
      EasyMock.expect(connector.isReverse()).andReturn(true);
      EasyMock.expect(connector.getAddress()).andReturn(InetAddress.getLocalHost());
      EasyMock.expect(connector.getPort()).andReturn(100);
      EasyMock.expect(connector.getType()).andReturn(Connector.Type.AJP);
      
      EasyMock.expect(nodeConfig.getDomain()).andReturn("domain");
      EasyMock.expect(nodeConfig.getFlushPackets()).andReturn(Boolean.TRUE);
      EasyMock.expect(nodeConfig.getFlushWait()).andReturn(1);
      EasyMock.expect(nodeConfig.getPing()).andReturn(2);
      EasyMock.expect(nodeConfig.getSmax()).andReturn(3);
      EasyMock.expect(nodeConfig.getTtl()).andReturn(4);
      EasyMock.expect(nodeConfig.getNodeTimeout()).andReturn(5);
      EasyMock.expect(nodeConfig.getBalancer()).andReturn("S");
      
      EasyMock.expect(engine.getSessionCookieName()).andReturn(DefaultMCMPRequestFactory.DEFAULT_SESSION_COOKIE_NAME);
      EasyMock.expect(engine.getSessionParameterName()).andReturn(DefaultMCMPRequestFactory.DEFAULT_SESSION_PARAMETER_NAME);
      
      EasyMock.expect(balancerConfig.getStickySession()).andReturn(Boolean.FALSE);
      EasyMock.expect(balancerConfig.getStickySessionRemove()).andReturn(Boolean.TRUE);
      EasyMock.expect(balancerConfig.getStickySessionForce()).andReturn(Boolean.FALSE);
      EasyMock.expect(balancerConfig.getWorkerTimeout()).andReturn(6);
      EasyMock.expect(balancerConfig.getMaxAttempts()).andReturn(7);
      
      EasyMock.expect(engine.getJvmRoute()).andReturn("host1");
      
      EasyMock.replay(engine, connector, nodeConfig, balancerConfig);
      
      MCMPRequest request = this.factory.createConfigRequest(engine, nodeConfig, balancerConfig);
      
      EasyMock.verify(engine, connector, nodeConfig, balancerConfig);
      
      Assert.assertSame(MCMPRequestType.CONFIG, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertEquals("host1", request.getJvmRoute());
      
      Map<String, String> parameters = request.getParameters();
      
      Assert.assertEquals(17, parameters.size());
      Assert.assertEquals("true", parameters.get("Reversed"));
      Assert.assertEquals(InetAddress.getLocalHost().getHostName(), parameters.get("Host"));
      Assert.assertEquals("100", parameters.get("Port"));
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
   }
   
   @Test
   public void createInfoRequest()
   {
      MCMPRequest request = this.factory.createInfoRequest();
      
      Assert.assertSame(MCMPRequestType.INFO, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertNull(request.getJvmRoute());
      Assert.assertTrue(request.getParameters().isEmpty());
   }
   
   @Test
   public void createDumpRequest()
   {
      MCMPRequest request = this.factory.createDumpRequest();
      
      Assert.assertSame(MCMPRequestType.DUMP, request.getRequestType());
      Assert.assertTrue(request.isWildcard());
      Assert.assertNull(request.getJvmRoute());
      Assert.assertTrue(request.getParameters().isEmpty());
   }
   
   @Test
   public void createDisableRequestEngine()
   {
      Engine engine = EasyMock.createStrictMock(Engine.class);

      EasyMock.expect(engine.getJvmRoute()).andReturn("route");
      
      EasyMock.replay(engine);
      
      MCMPRequest request = this.factory.createDisableRequest(engine);
      
      EasyMock.verify(engine);
      
      Assert.assertSame(MCMPRequestType.DISABLE_APP, request.getRequestType());
      Assert.assertTrue(request.isWildcard());
      Assert.assertEquals("route", request.getJvmRoute());
      Assert.assertTrue(request.getParameters().isEmpty());
   }
   
   @Test
   public void createEnableRequestEngine()
   {
      Engine engine = EasyMock.createStrictMock(Engine.class);

      EasyMock.expect(engine.getJvmRoute()).andReturn("route");
      
      EasyMock.replay(engine);
      
      MCMPRequest request = this.factory.createEnableRequest(engine);
      
      EasyMock.verify(engine);
      
      Assert.assertSame(MCMPRequestType.ENABLE_APP, request.getRequestType());
      Assert.assertTrue(request.isWildcard());
      Assert.assertEquals("route", request.getJvmRoute());
      Assert.assertTrue(request.getParameters().isEmpty());
   }
   
   @Test
   public void createRemoveRequestEngine()
   {
      Engine engine = EasyMock.createStrictMock(Engine.class);

      EasyMock.expect(engine.getJvmRoute()).andReturn("route");
      
      EasyMock.replay(engine);
      
      MCMPRequest request = this.factory.createRemoveRequest(engine);
      
      EasyMock.verify(engine);
      
      Assert.assertSame(MCMPRequestType.REMOVE_APP, request.getRequestType());
      Assert.assertTrue(request.isWildcard());
      Assert.assertEquals("route", request.getJvmRoute());
      Assert.assertTrue(request.getParameters().isEmpty());
   }

   @Test
   public void createRemoveContextRequest()
   {
      String route = "route";
      String path = "path";
      
      MCMPRequest request = this.factory.createRemoveContextRequest(route, new LinkedHashSet<String>(Arrays.asList("alias1", "alias2")), path);
      
      Assert.assertSame(MCMPRequestType.REMOVE_APP, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertSame(route, request.getJvmRoute());
      
      Map<String, String> parameters = request.getParameters();
      
      Assert.assertEquals(2, parameters.size());
      Assert.assertEquals("alias1,alias2", parameters.get("Alias"));
      Assert.assertSame(path, parameters.get("Context"));
   }
   
   @Test
   public void createRemoveEngineRequest()
   {
      String route = "route";
      
      MCMPRequest request = this.factory.createRemoveEngineRequest(route);
      
      Assert.assertSame(MCMPRequestType.REMOVE_APP, request.getRequestType());
      Assert.assertTrue(request.isWildcard());
      Assert.assertSame(route, request.getJvmRoute());
      
      Assert.assertTrue(request.getParameters().isEmpty());
   }
   
   @Test
   public void createPingRequest()
   {
      MCMPRequest request = this.factory.createPingRequest();
      
      Assert.assertSame(MCMPRequestType.PING, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertNull(request.getJvmRoute());
      Assert.assertTrue(request.getParameters().isEmpty());
   }
   
   @Test
   public void createJvmRoutePingRequest()
   {
      String jvmRoute = "route";
      
      MCMPRequest request = this.factory.createPingRequest(jvmRoute);
      
      Assert.assertSame(MCMPRequestType.PING, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertSame(jvmRoute, request.getJvmRoute());
      Assert.assertTrue(request.getParameters().isEmpty());
   }
   
   @Test
   public void createURIPingRequest() throws URISyntaxException
   {
      MCMPRequest request = this.factory.createPingRequest("ajp", "localhost", 8009);
      
      Assert.assertSame(MCMPRequestType.PING, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertNull(request.getJvmRoute());
      
      Map<String, String> parameters = request.getParameters();
      Assert.assertEquals(3, parameters.size());
      Assert.assertEquals("ajp", parameters.get("Scheme"));
      Assert.assertEquals("localhost", parameters.get("Host"));
      Assert.assertEquals("8009", parameters.get("Port"));
   }
}
