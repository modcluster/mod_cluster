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
import org.easymock.EasyMock;
import org.jboss.ha.framework.interfaces.HAPartition;
import org.jboss.ha.framework.interfaces.HAServiceKeyProvider;
import org.jboss.ha.framework.interfaces.HASingletonMBean;
import org.jboss.modcluster.ServerProvider;
import org.jboss.modcluster.config.BalancerConfiguration;
import org.jboss.modcluster.config.NodeConfiguration;
import org.jboss.modcluster.ha.rpc.RpcResponse;
import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPRequestFactory;
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
   private MCMPRequestFactory requestFactory = EasyMock.createStrictMock(MCMPRequestFactory.class);
   @SuppressWarnings("unchecked")
   private ServerProvider<Server> serverProvider = EasyMock.createStrictMock(ServerProvider.class);
   
   @Test
   public void getResetRequestsNonMaster()
   {
      HASingletonAwareResetRequestSource source = new HASingletonAwareResetRequestSourceImpl(this.nodeConfig, this.balancerConfig, this.serverProvider, this.requestFactory, this.singleton, this.key);

      EasyMock.expect(this.singleton.isMasterNode()).andReturn(false);

      EasyMock.replay(this.singleton);
      
      Map<String, Set<ResetRequestSource.VirtualHost>> emptyResponseMap = Collections.emptyMap();

      List<MCMPRequest> requests = source.getResetRequests(emptyResponseMap);

      EasyMock.verify(this.singleton);
      
      Assert.assertTrue(requests.isEmpty());

      EasyMock.reset(this.singleton);
   }

   @Test
   public void getResetRequestsNoInit() throws Exception
   {
      HASingletonAwareResetRequestSource source = new HASingletonAwareResetRequestSourceImpl(this.nodeConfig, this.balancerConfig, this.serverProvider, this.requestFactory, this.singleton, this.key);

      Map<String, Set<ResetRequestSource.VirtualHost>> emptyResponseMap = Collections.emptyMap();
      
      MCMPRequest request1 = EasyMock.createMock(MCMPRequest.class);
      MCMPRequest request2 = EasyMock.createMock(MCMPRequest.class);
      RpcResponse<List<MCMPRequest>> response1 = EasyMock.createMock(RpcResponse.class);
      RpcResponse<List<MCMPRequest>> response2 = EasyMock.createMock(RpcResponse.class);
      ArrayList<RpcResponse<List<MCMPRequest>>> responses = new ArrayList<RpcResponse<List<MCMPRequest>>>(Arrays.asList(response1, response2));
      
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(true);
      
      EasyMock.expect(this.key.getHAPartition()).andReturn(this.partition);
      EasyMock.expect(this.key.getHAServiceKey()).andReturn("service:domain");
      EasyMock.expect(this.partition.callMethodOnCluster(EasyMock.eq("service:domain"), EasyMock.eq("getResetRequests"), EasyMock.aryEq(new Object[] { emptyResponseMap }), EasyMock.aryEq(new Class[] { Map.class }), EasyMock.eq(true))).andReturn(responses);
      
      EasyMock.expect(response1.getResult()).andReturn(Collections.singletonList(request1));
      EasyMock.expect(response2.getResult()).andReturn(Collections.singletonList(request2));
      
      EasyMock.replay(this.key, this.singleton, this.partition, response1, response2);
      
      List<MCMPRequest> results = source.getResetRequests(emptyResponseMap);
      
      EasyMock.verify(this.key, this.singleton, this.partition, response1, response2);
      
      Assert.assertEquals(2, results.size());
      Assert.assertSame(request1, results.get(0));
      Assert.assertSame(request2, results.get(1));
      
      EasyMock.reset(this.key, this.singleton, this.partition, response1, response2);
   }
   
   @Test
   public void getResetRequests() throws Exception
   {
      HASingletonAwareResetRequestSource source = new HASingletonAwareResetRequestSourceImpl(this.nodeConfig, this.balancerConfig, this.serverProvider, this.requestFactory, this.singleton, this.key);

      Map<String, Set<ResetRequestSource.VirtualHost>> emptyResponseMap = Collections.emptyMap();
      Map<String, Set<String>> emptyContextMap = Collections.emptyMap();
      MCMPRequest request1 = EasyMock.createMock(MCMPRequest.class);
      MCMPRequest request2 = EasyMock.createMock(MCMPRequest.class);
      RpcResponse<List<MCMPRequest>> response1 = EasyMock.createMock(RpcResponse.class);
      RpcResponse<List<MCMPRequest>> response2 = EasyMock.createMock(RpcResponse.class);
      ArrayList<RpcResponse<List<MCMPRequest>>> responses = new ArrayList<RpcResponse<List<MCMPRequest>>>(Arrays.asList(response1, response2));
      
      Server server = EasyMock.createStrictMock(Server.class);
      Service service = EasyMock.createStrictMock(Service.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Context context = EasyMock.createStrictMock(Context.class);
      Host host = EasyMock.createStrictMock(Host.class);
      MCMPRequest configRequest = EasyMock.createStrictMock(MCMPRequest.class);
      MCMPRequest contextRequest = EasyMock.createStrictMock(MCMPRequest.class);
      
      source.init(emptyContextMap);
      
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(true);
      
      EasyMock.expect(this.serverProvider.getServer()).andReturn(server);
      
      EasyMock.expect(server.findServices()).andReturn(new Service[] { service });
      EasyMock.expect(service.getContainer()).andReturn(engine);
      
      EasyMock.expect(this.requestFactory.createConfigRequest(engine, this.nodeConfig, this.balancerConfig)).andReturn(configRequest);

      EasyMock.expect(engine.getJvmRoute()).andReturn("host1");
      
      EasyMock.expect(engine.findChildren()).andReturn(new Container[] { host });
      EasyMock.expect(host.getName()).andReturn("host").times(2);
      EasyMock.expect(host.findAliases()).andReturn(new String[] { "alias1", "alias2" });
      EasyMock.expect(host.findChildren()).andReturn(new Container[] { context });
      EasyMock.expect(context.getPath()).andReturn("/context");
      EasyMock.expect(context.isStarted()).andReturn(true);
      EasyMock.expect(context.getAvailable()).andReturn(true);
      
      EasyMock.expect(this.requestFactory.createEnableRequest(context)).andReturn(contextRequest);
      
      EasyMock.expect(this.key.getHAPartition()).andReturn(this.partition);
      EasyMock.expect(this.key.getHAServiceKey()).andReturn("service:domain");
      EasyMock.expect(this.partition.callMethodOnCluster(EasyMock.eq("service:domain"), EasyMock.eq("getResetRequests"), EasyMock.aryEq(new Object[] { emptyResponseMap }), EasyMock.aryEq(new Class[] { Map.class }), EasyMock.eq(true))).andReturn(responses);
      
      EasyMock.expect(response1.getResult()).andReturn(Collections.singletonList(request1));
      EasyMock.expect(response2.getResult()).andReturn(Collections.singletonList(request2));
      
      EasyMock.replay(this.serverProvider, this.key, this.singleton, this.partition, this.requestFactory, server, service, engine, host, context, response1, response2);
      
      List<MCMPRequest> result = source.getResetRequests(emptyResponseMap);
      
      EasyMock.verify(this.serverProvider, this.key, this.singleton, this.partition, this.requestFactory, server, service, engine, host, context, response1, response2);
      
      Assert.assertEquals(4, result.size());
      
      Assert.assertSame(configRequest, result.get(0));
      Assert.assertSame(contextRequest, result.get(1));
      Assert.assertSame(request1, result.get(2));
      Assert.assertSame(request2, result.get(3));
   }
   
   @Test
   public void getLocalResetRequestsNoServer() throws Exception
   {
      HASingletonAwareResetRequestSource source = new HASingletonAwareResetRequestSourceImpl(this.nodeConfig, this.balancerConfig, this.serverProvider, this.requestFactory, this.singleton, this.key);

      Map<String, Set<ResetRequestSource.VirtualHost>> emptyResponseMap = Collections.emptyMap();

      EasyMock.expect(this.serverProvider.getServer()).andReturn(null);
      
      EasyMock.replay(this.serverProvider, this.nodeConfig, this.balancerConfig, this.key, this.singleton, this.partition, this.requestFactory);
      
      List<MCMPRequest> requests = source.getLocalResetRequests(emptyResponseMap);

      EasyMock.verify(this.serverProvider, this.nodeConfig, this.balancerConfig, this.key, this.singleton, this.partition, this.requestFactory);
      
      Assert.assertTrue(requests.isEmpty());
   }
   
   @Test
   public void getLocalResetRequests() throws Exception
   {
      HASingletonAwareResetRequestSource source = new HASingletonAwareResetRequestSourceImpl(this.nodeConfig, this.balancerConfig, this.serverProvider, this.requestFactory, this.singleton, this.key);

      Map<String, Set<ResetRequestSource.VirtualHost>> emptyResponseMap = Collections.emptyMap();
      Map<String, Set<String>> emptyContextMap = Collections.emptyMap();
      
      // Test w/server
      Server server = EasyMock.createStrictMock(Server.class);
      Service service = EasyMock.createStrictMock(Service.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Context context = EasyMock.createStrictMock(Context.class);
      Host host = EasyMock.createStrictMock(Host.class);
      MCMPRequest configRequest = EasyMock.createStrictMock(MCMPRequest.class);
      MCMPRequest contextRequest = EasyMock.createStrictMock(MCMPRequest.class);

      source.init(emptyContextMap);
      
      EasyMock.expect(this.serverProvider.getServer()).andReturn(server);
      
      EasyMock.expect(server.findServices()).andReturn(new Service[] { service });
      EasyMock.expect(service.getContainer()).andReturn(engine);
      
      EasyMock.expect(this.requestFactory.createConfigRequest(engine, this.nodeConfig, this.balancerConfig)).andReturn(configRequest);

      EasyMock.expect(engine.getJvmRoute()).andReturn("host1");
      
      EasyMock.expect(engine.findChildren()).andReturn(new Container[] { host });
      EasyMock.expect(host.getName()).andReturn("host").times(2);
      EasyMock.expect(host.findAliases()).andReturn(new String[] { "alias1", "alias2" });
      EasyMock.expect(host.findChildren()).andReturn(new Container[] { context });
      EasyMock.expect(context.getPath()).andReturn("/context");
      EasyMock.expect(context.isStarted()).andReturn(true);
      EasyMock.expect(context.getAvailable()).andReturn(true);
      
      EasyMock.expect(this.requestFactory.createEnableRequest(context)).andReturn(contextRequest);
      
      EasyMock.replay(this.serverProvider, this.nodeConfig, this.balancerConfig, this.key, this.singleton, this.partition, this.requestFactory, server, service, engine, host, context);
      
      List<MCMPRequest> requests = source.getLocalResetRequests(emptyResponseMap);
      
      EasyMock.verify(this.serverProvider, this.nodeConfig, this.balancerConfig, this.key, this.singleton, this.partition, this.requestFactory, server, service, engine, host, context);
      
      Assert.assertEquals(2, requests.size());
      
      Assert.assertSame(configRequest, requests.get(0));
      Assert.assertSame(contextRequest, requests.get(1));
   }
}
