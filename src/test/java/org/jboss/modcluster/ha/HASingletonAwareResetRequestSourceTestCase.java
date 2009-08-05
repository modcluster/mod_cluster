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

import java.util.ArrayList;
import java.util.Arrays;
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
import org.easymock.EasyMock;
import org.jboss.ha.framework.interfaces.ClusterNode;
import org.jboss.ha.framework.interfaces.HAPartition;
import org.jboss.ha.framework.interfaces.HAServiceKeyProvider;
import org.jboss.ha.framework.interfaces.HASingletonMBean;
import org.jboss.modcluster.config.BalancerConfiguration;
import org.jboss.modcluster.config.NodeConfiguration;
import org.jboss.modcluster.ha.rpc.ResetRequestGroupRpcResponse;
import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPRequestType;
import org.jboss.modcluster.mcmp.ResetRequestSource;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
@SuppressWarnings("boxing")
public class HASingletonAwareResetRequestSourceTestCase
{
   private NodeConfiguration nodeConfig = EasyMock.createStrictMock(NodeConfiguration.class);
   private BalancerConfiguration balancerConfig = EasyMock.createStrictMock(BalancerConfiguration.class);
   private HAServiceKeyProvider key = EasyMock.createStrictMock(HAServiceKeyProvider.class);
   private HASingletonMBean singleton = EasyMock.createStrictMock(HASingletonMBean.class);
   private HAPartition partition = EasyMock.createStrictMock(HAPartition.class);
   
   private HASingletonAwareResetRequestSource source = new HASingletonAwareResetRequestSourceImpl(this.nodeConfig, this.balancerConfig, this.singleton, this.key);

   @Test
   public void getResetRequests() throws Exception
   {
      Map<String, String> emptyMap = Collections.emptyMap();
      Map<String, Set<ResetRequestSource.VirtualHost>> emptyResponseMap = Collections.emptyMap();
      MCMPRequest request1 = new MCMPRequest(MCMPRequestType.ENABLE_APP, false, "host1", emptyMap);
      MCMPRequest request2 = new MCMPRequest(MCMPRequestType.REMOVE_APP, false, "host2", emptyMap);
      
      ResetRequestGroupRpcResponse response1 = new ResetRequestGroupRpcResponse(EasyMock.createMock(ClusterNode.class), Collections.singletonList(request1));
      ResetRequestGroupRpcResponse response2 = new ResetRequestGroupRpcResponse(EasyMock.createMock(ClusterNode.class), Collections.singletonList(request2));
      
      // Test master - w/out server
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(true);
      
      EasyMock.expect(this.key.getHAPartition()).andReturn(this.partition);
      EasyMock.expect(this.key.getHAServiceKey()).andReturn("service:domain");
      EasyMock.expect(this.partition.callMethodOnCluster(EasyMock.eq("service:domain"), EasyMock.eq("getResetRequests"), EasyMock.aryEq(new Object[] { emptyResponseMap }), EasyMock.aryEq(new Class[] { Map.class }), EasyMock.eq(true))).andReturn(new ArrayList<ResetRequestGroupRpcResponse>(Arrays.asList(response1, response2)));
      
      EasyMock.replay(this.singleton, this.key, this.partition);
      
      List<MCMPRequest> results = this.source.getResetRequests(emptyResponseMap);
      
      EasyMock.verify(this.singleton, this.key, this.partition);
      
      Assert.assertEquals(2, results.size());
      Assert.assertSame(request1, results.get(0));
      Assert.assertSame(request2, results.get(1));
      
      EasyMock.reset(this.singleton, this.key, this.partition);
      
      
      // Test master - w/server
      Server server = EasyMock.createStrictMock(Server.class);
      Service service = EasyMock.createStrictMock(Service.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Context context = EasyMock.createStrictMock(Context.class);
      Host host = EasyMock.createStrictMock(Host.class);
      Connector connector = new Connector("AJP/1.3");
      
      this.source.setJbossWebServer(server);
      
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(true);
      
      EasyMock.expect(this.key.getHAPartition()).andReturn(this.partition);
      EasyMock.expect(this.key.getHAServiceKey()).andReturn("service:domain");
      EasyMock.expect(this.partition.callMethodOnCluster(EasyMock.eq("service:domain"), EasyMock.eq("getResetRequests"), EasyMock.aryEq(new Object[] { emptyResponseMap }), EasyMock.aryEq(new Class[] { Map.class }), EasyMock.eq(true))).andReturn(new ArrayList<ResetRequestGroupRpcResponse>(Arrays.asList(response1, response2)));
      
      EasyMock.expect(server.findServices()).andReturn(new Service[] { service });
      EasyMock.expect(service.getContainer()).andReturn(engine);
      EasyMock.expect(engine.getJvmRoute()).andReturn("host1");
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
      
      EasyMock.expect(engine.findChildren()).andReturn(new Container[] { host });
      EasyMock.expect(host.getName()).andReturn("host").times(2);
      EasyMock.expect(host.findAliases()).andReturn(new String[] { "alias1", "alias2" });
      EasyMock.expect(host.findChildren()).andReturn(new Container[] { context });
      EasyMock.expect(context.getPath()).andReturn("/context");
      EasyMock.expect(context.isStarted()).andReturn(true);
       
      EasyMock.replay(this.singleton, this.key, this.partition, server, service, engine, context, host, this.nodeConfig, this.balancerConfig);
      
      results = this.source.getResetRequests(emptyResponseMap);
      
      EasyMock.verify(this.singleton, this.key, this.partition, server, service, engine, context, host, this.nodeConfig, this.balancerConfig);
      
      Assert.assertEquals(4, results.size());
      
      MCMPRequest request = results.get(0);
      Map<String, String> parameters = request.getParameters();
      
      Assert.assertSame(MCMPRequestType.CONFIG, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertEquals("host1", request.getJvmRoute());
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
      
      request = results.get(1);
      parameters = request.getParameters();
      
      Assert.assertSame(MCMPRequestType.ENABLE_APP, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertEquals("host1", request.getJvmRoute());
      Assert.assertEquals(2, parameters.size());
      Assert.assertEquals("/context", parameters.get("Context"));
      Assert.assertEquals("host,alias1,alias2", parameters.get("Alias"));
      
      Assert.assertSame(request1, results.get(2));
      Assert.assertSame(request2, results.get(3));

      EasyMock.reset(this.singleton, this.key, this.partition, server, service, engine, context, host, this.nodeConfig, this.balancerConfig);
      
      // Test non-master
      results = this.source.getResetRequests(emptyResponseMap);
      
      Assert.assertTrue(results.isEmpty());
   }
   
   @Test
   public void getLocalResetRequests() throws Exception
   {
      Map<String, Set<ResetRequestSource.VirtualHost>> emptyResponseMap = Collections.emptyMap();
      // Test w/out server
      List<MCMPRequest> results = this.source.getLocalResetRequests(emptyResponseMap);
      
      Assert.assertTrue(results.isEmpty());
      
      
      // Test w/server
      Server server = EasyMock.createStrictMock(Server.class);
      Service service = EasyMock.createStrictMock(Service.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Context context = EasyMock.createStrictMock(Context.class);
      Host host = EasyMock.createStrictMock(Host.class);
      Connector connector = new Connector("AJP/1.3");
      
      this.source.setJbossWebServer(server);
            
      EasyMock.expect(server.findServices()).andReturn(new Service[] { service });
      EasyMock.expect(service.getContainer()).andReturn(engine);
      EasyMock.expect(engine.getJvmRoute()).andReturn("host1");
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
      
      EasyMock.expect(engine.findChildren()).andReturn(new Container[] { host });
      EasyMock.expect(host.getName()).andReturn("host").times(2);
      EasyMock.expect(host.findAliases()).andReturn(new String[] { "alias1", "alias2" });
      EasyMock.expect(host.findChildren()).andReturn(new Container[] { context });
      EasyMock.expect(context.getPath()).andReturn("/context");
      EasyMock.expect(context.isStarted()).andReturn(true);
       
      EasyMock.replay(this.singleton, this.key, this.partition, server, service, engine, context, host, this.nodeConfig, this.balancerConfig);
      
      results = this.source.getLocalResetRequests(emptyResponseMap);
      
      EasyMock.verify(this.singleton, this.key, this.partition, server, service, engine, context, host, this.nodeConfig, this.balancerConfig);
      
      Assert.assertEquals(2, results.size());
      
      MCMPRequest request = results.get(0);
      Map<String, String> parameters = request.getParameters();
      
      Assert.assertSame(MCMPRequestType.CONFIG, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertEquals("host1", request.getJvmRoute());
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
      
      request = results.get(1);
      parameters = request.getParameters();
      
      Assert.assertSame(MCMPRequestType.ENABLE_APP, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertEquals("host1", request.getJvmRoute());
      Assert.assertEquals(2, parameters.size());
      Assert.assertEquals("/context", parameters.get("Context"));
      Assert.assertEquals("host,alias1,alias2", parameters.get("Alias"));

      EasyMock.reset(this.singleton, this.key, this.partition, server, service, engine, context, host, this.nodeConfig, this.balancerConfig);
   }
}
