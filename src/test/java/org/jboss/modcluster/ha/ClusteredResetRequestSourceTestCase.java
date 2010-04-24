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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.easymock.EasyMock;
import org.easymock.IExpectationSetters;
import org.jboss.ha.framework.interfaces.HAPartition;
import org.jboss.ha.framework.interfaces.HAServiceKeyProvider;
import org.jboss.ha.framework.interfaces.HASingletonMBean;
import org.jboss.modcluster.Context;
import org.jboss.modcluster.Engine;
import org.jboss.modcluster.Host;
import org.jboss.modcluster.Server;
import org.jboss.modcluster.config.BalancerConfiguration;
import org.jboss.modcluster.config.NodeConfiguration;
import org.jboss.modcluster.ha.rpc.RpcResponse;
import org.jboss.modcluster.mcmp.ContextFilter;
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
public class ClusteredResetRequestSourceTestCase
{
   private NodeConfiguration nodeConfig = EasyMock.createStrictMock(NodeConfiguration.class);
   private BalancerConfiguration balancerConfig = EasyMock.createStrictMock(BalancerConfiguration.class);
   private HAServiceKeyProvider key = EasyMock.createStrictMock(HAServiceKeyProvider.class);
   private HASingletonMBean singleton = EasyMock.createStrictMock(HASingletonMBean.class);
   private HAPartition partition = EasyMock.createStrictMock(HAPartition.class);
   private MCMPRequestFactory requestFactory = EasyMock.createStrictMock(MCMPRequestFactory.class);
   
   private ResetRequestSource source = new ClusteredResetRequestSource(this.nodeConfig, this.balancerConfig, this.requestFactory, this.singleton, this.key);
   
   @Test
   public void getResetRequestsNonMaster()
   {
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(false);

      EasyMock.replay(this.singleton);
      
      List<MCMPRequest> requests = this.source.getResetRequests(Collections.<String, Set<ResetRequestSource.VirtualHost>>emptyMap());

      EasyMock.verify(this.singleton);
      
      Assert.assertTrue(requests.isEmpty());

      EasyMock.reset(this.singleton);
   }

   @Test
   public void getResetRequestsNoInit() throws Exception
   {
      MCMPRequest request1 = EasyMock.createMock(MCMPRequest.class);
      MCMPRequest request2 = EasyMock.createMock(MCMPRequest.class);
      @SuppressWarnings("unchecked")
      RpcResponse<List<MCMPRequest>> response1 = EasyMock.createMock(RpcResponse.class);
      @SuppressWarnings("unchecked")
      RpcResponse<List<MCMPRequest>> response2 = EasyMock.createMock(RpcResponse.class);
      @SuppressWarnings("unchecked")
      List<RpcResponse<List<MCMPRequest>>> responses = new ArrayList<RpcResponse<List<MCMPRequest>>>(Arrays.asList(response1, response2));
      
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(true);
      
      EasyMock.expect(this.key.getHAPartition()).andReturn(this.partition);
      EasyMock.expect(this.key.getHAServiceKey()).andReturn("service:domain");
      EasyMock.expect((List<RpcResponse<List<MCMPRequest>>>) this.partition.callMethodOnCluster(EasyMock.eq("service:domain"), EasyMock.eq("getResetRequests"), EasyMock.aryEq(new Object[] { Collections.<String, Set<ResetRequestSource.VirtualHost>>emptyMap() }), EasyMock.aryEq(new Class[] { Map.class }), EasyMock.eq(true))).andReturn(responses);
      
      EasyMock.expect(response1.getResult()).andReturn(Collections.singletonList(request1));
      EasyMock.expect(response2.getResult()).andReturn(Collections.singletonList(request2));
      
      EasyMock.replay(this.key, this.singleton, this.partition, response1, response2);
      
      List<MCMPRequest> results = this.source.getResetRequests(Collections.<String, Set<ResetRequestSource.VirtualHost>>emptyMap());
      
      EasyMock.verify(this.key, this.singleton, this.partition, response1, response2);
      
      Assert.assertEquals(2, results.size());
      Assert.assertSame(request1, results.get(0));
      Assert.assertSame(request2, results.get(1));
      
      EasyMock.reset(this.key, this.singleton, this.partition, response1, response2);
   }
   
   @Test
   public void getResetRequestsContextNotAutoEnabled() throws Exception
   {
      ContextFilter contextFilter = EasyMock.createStrictMock(ContextFilter.class);
      MCMPRequest request1 = EasyMock.createMock(MCMPRequest.class);
      MCMPRequest request2 = EasyMock.createMock(MCMPRequest.class);
      @SuppressWarnings("unchecked")
      RpcResponse<List<MCMPRequest>> response1 = EasyMock.createMock(RpcResponse.class);
      @SuppressWarnings("unchecked")
      RpcResponse<List<MCMPRequest>> response2 = EasyMock.createMock(RpcResponse.class);
      @SuppressWarnings("unchecked")
      List<RpcResponse<List<MCMPRequest>>> responses = new ArrayList<RpcResponse<List<MCMPRequest>>>(Arrays.asList(response1, response2));
      
      Server server = EasyMock.createStrictMock(Server.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Context context = EasyMock.createStrictMock(Context.class);
      Context excludedContext = EasyMock.createStrictMock(Context.class);
      Host host = EasyMock.createStrictMock(Host.class);
      MCMPRequest configRequest = EasyMock.createStrictMock(MCMPRequest.class);
      MCMPRequest contextRequest = EasyMock.createStrictMock(MCMPRequest.class);
      
      this.source.init(server, contextFilter);
      
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(true);
      
      EasyMock.expect(contextFilter.getExcludedContexts()).andReturn(Collections.singletonMap(host, Collections.singleton("/excluded")));
      EasyMock.expect(contextFilter.isAutoEnableContexts()).andReturn(false);
      
      EasyMock.expect(server.getEngines()).andReturn(Collections.singleton(engine));
      
      EasyMock.expect(this.requestFactory.createConfigRequest(engine, this.nodeConfig, this.balancerConfig)).andReturn(configRequest);

      EasyMock.expect(engine.getJvmRoute()).andReturn("host1");
      
      EasyMock.expect(engine.getHosts()).andReturn(Collections.singleton(host));
      EasyMock.expect(host.getName()).andReturn("host");
      EasyMock.expect(host.getAliases()).andReturn(new LinkedHashSet<String>(Arrays.asList("host", "alias1")));
      EasyMock.expect(host.getContexts()).andReturn(Arrays.asList(context, excludedContext));
      EasyMock.expect(context.getPath()).andReturn("/context");
      EasyMock.expect(context.isStarted()).andReturn(true);
      
      EasyMock.expect(this.requestFactory.createDisableRequest(context)).andReturn(contextRequest);

      EasyMock.expect(excludedContext.getPath()).andReturn("/excluded");
      
      EasyMock.expect(this.key.getHAPartition()).andReturn(this.partition);
      EasyMock.expect(this.key.getHAServiceKey()).andReturn("service:domain");
      EasyMock.expect((List<RpcResponse<List<MCMPRequest>>>) this.partition.callMethodOnCluster(EasyMock.eq("service:domain"), EasyMock.eq("getResetRequests"), EasyMock.aryEq(new Object[] { Collections.<String, Set<ResetRequestSource.VirtualHost>>emptyMap() }), EasyMock.aryEq(new Class[] { Map.class }), EasyMock.eq(true))).andReturn(responses);
      
      EasyMock.expect(response1.getResult()).andReturn(Collections.singletonList(request1));
      EasyMock.expect(response2.getResult()).andReturn(Collections.singletonList(request2));
      
      EasyMock.replay(this.key, this.singleton, this.partition, this.requestFactory, server, engine, host, context, excludedContext, response1, response2, contextFilter);
      
      List<MCMPRequest> result = this.source.getResetRequests(Collections.<String, Set<ResetRequestSource.VirtualHost>>emptyMap());
      
      EasyMock.verify(this.key, this.singleton, this.partition, this.requestFactory, server, engine, host, context, excludedContext, response1, response2, contextFilter);
      
      Assert.assertEquals(4, result.size());
      
      Assert.assertSame(configRequest, result.get(0));
      Assert.assertSame(contextRequest, result.get(1));
      Assert.assertSame(request1, result.get(2));
      Assert.assertSame(request2, result.get(3));
   }
   
   @Test
   public void getResetRequests() throws Exception
   {
      ContextFilter contextFilter = EasyMock.createStrictMock(ContextFilter.class);
      MCMPRequest request1 = EasyMock.createMock(MCMPRequest.class);
      MCMPRequest request2 = EasyMock.createMock(MCMPRequest.class);
      @SuppressWarnings("unchecked")
      RpcResponse<List<MCMPRequest>> response1 = EasyMock.createMock(RpcResponse.class);
      @SuppressWarnings("unchecked")
      RpcResponse<List<MCMPRequest>> response2 = EasyMock.createMock(RpcResponse.class);
      @SuppressWarnings("unchecked")
      List<RpcResponse<List<MCMPRequest>>> responses = new ArrayList<RpcResponse<List<MCMPRequest>>>(Arrays.asList(response1, response2));
      
      Server server = EasyMock.createStrictMock(Server.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Context context = EasyMock.createStrictMock(Context.class);
      Context excludedContext = EasyMock.createStrictMock(Context.class);
      Host host = EasyMock.createStrictMock(Host.class);
      MCMPRequest configRequest = EasyMock.createStrictMock(MCMPRequest.class);
      MCMPRequest contextRequest = EasyMock.createStrictMock(MCMPRequest.class);
      
      this.source.init(server, contextFilter);
      
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(true);
      
      EasyMock.expect(contextFilter.getExcludedContexts()).andReturn(Collections.singletonMap(host, Collections.singleton("/excluded")));
      EasyMock.expect(contextFilter.isAutoEnableContexts()).andReturn(true);
      
      EasyMock.expect(server.getEngines()).andReturn(Collections.singleton(engine));
      
      EasyMock.expect(this.requestFactory.createConfigRequest(engine, this.nodeConfig, this.balancerConfig)).andReturn(configRequest);

      EasyMock.expect(engine.getJvmRoute()).andReturn("host1");
      
      EasyMock.expect(engine.getHosts()).andReturn(Collections.singleton(host));
      EasyMock.expect(host.getName()).andReturn("host");
      EasyMock.expect(host.getAliases()).andReturn(new LinkedHashSet<String>(Arrays.asList("host", "alias1")));
      EasyMock.expect(host.getContexts()).andReturn(Arrays.asList(context, excludedContext));
      EasyMock.expect(context.getPath()).andReturn("/context");
      EasyMock.expect(context.isStarted()).andReturn(true);
      
      EasyMock.expect(this.requestFactory.createEnableRequest(context)).andReturn(contextRequest);

      EasyMock.expect(excludedContext.getPath()).andReturn("/excluded");
      
      EasyMock.expect(this.key.getHAPartition()).andReturn(this.partition);
      EasyMock.expect(this.key.getHAServiceKey()).andReturn("service:domain");
      EasyMock.expect((List<RpcResponse<List<MCMPRequest>>>) this.partition.callMethodOnCluster(EasyMock.eq("service:domain"), EasyMock.eq("getResetRequests"), EasyMock.aryEq(new Object[] { Collections.<String, Set<ResetRequestSource.VirtualHost>>emptyMap() }), EasyMock.aryEq(new Class[] { Map.class }), EasyMock.eq(true))).andReturn(responses);
      
      EasyMock.expect(response1.getResult()).andReturn(Collections.singletonList(request1));
      EasyMock.expect(response2.getResult()).andReturn(Collections.singletonList(request2));
      
      EasyMock.replay(this.key, this.singleton, this.partition, this.requestFactory, server, engine, host, context, excludedContext, response1, response2, contextFilter);
      
      List<MCMPRequest> result = this.source.getResetRequests(Collections.<String, Set<ResetRequestSource.VirtualHost>>emptyMap());
      
      EasyMock.verify(this.key, this.singleton, this.partition, this.requestFactory, server, engine, host, context, excludedContext, response1, response2, contextFilter);
      
      Assert.assertEquals(4, result.size());
      
      Assert.assertSame(configRequest, result.get(0));
      Assert.assertSame(contextRequest, result.get(1));
      Assert.assertSame(request1, result.get(2));
      Assert.assertSame(request2, result.get(3));
   }
}
