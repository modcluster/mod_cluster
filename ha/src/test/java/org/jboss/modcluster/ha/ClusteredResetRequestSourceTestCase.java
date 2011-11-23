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

import static org.junit.Assert.*;
import static org.mockito.AdditionalMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.ha.framework.interfaces.HAPartition;
import org.jboss.ha.framework.interfaces.HAServiceKeyProvider;
import org.jboss.ha.framework.interfaces.HASingletonMBean;
import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Host;
import org.jboss.modcluster.container.Server;
import org.jboss.modcluster.config.BalancerConfiguration;
import org.jboss.modcluster.config.NodeConfiguration;
import org.jboss.modcluster.ha.rpc.RpcResponse;
import org.jboss.modcluster.mcmp.ContextFilter;
import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPRequestFactory;
import org.jboss.modcluster.mcmp.ResetRequestSource;
import org.junit.Test;

/**
 * @author Paul Ferraro
 * 
 */
@SuppressWarnings("boxing")
public class ClusteredResetRequestSourceTestCase {
    private NodeConfiguration nodeConfig = mock(NodeConfiguration.class);
    private BalancerConfiguration balancerConfig = mock(BalancerConfiguration.class);
    private HAServiceKeyProvider key = mock(HAServiceKeyProvider.class);
    private HASingletonMBean singleton = mock(HASingletonMBean.class);
    private HAPartition partition = mock(HAPartition.class);
    private MCMPRequestFactory requestFactory = mock(MCMPRequestFactory.class);

    private ResetRequestSource source = new ClusteredResetRequestSource(this.nodeConfig, this.balancerConfig,
            this.requestFactory, this.singleton, this.key);

    @Test
    public void getResetRequestsNonMaster() {
        when(this.singleton.isMasterNode()).thenReturn(false);

        List<MCMPRequest> requests = this.source.getResetRequests(Collections
                .<String, Set<ResetRequestSource.VirtualHost>> emptyMap());

        assertTrue(requests.isEmpty());
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Test
    public void getResetRequestsNoInit() throws Exception {
        MCMPRequest request1 = mock(MCMPRequest.class);
        MCMPRequest request2 = mock(MCMPRequest.class);
        RpcResponse<List<MCMPRequest>> response1 = mock(RpcResponse.class);
        RpcResponse<List<MCMPRequest>> response2 = mock(RpcResponse.class);
        List<RpcResponse<List<MCMPRequest>>> responses = new ArrayList<RpcResponse<List<MCMPRequest>>>(Arrays.asList(response1,
                response2));

        when(this.singleton.isMasterNode()).thenReturn(true);

        when(this.key.getHAPartition()).thenReturn(this.partition);
        when(this.key.getHAServiceKey()).thenReturn("service:domain");
        when(
                (List<RpcResponse<List<MCMPRequest>>>) this.partition.callMethodOnCluster(eq("service:domain"),
                        eq("getResetRequests"),
                        aryEq(new Object[] { Collections.<String, Set<ResetRequestSource.VirtualHost>> emptyMap() }),
                        aryEq(new Class[] { Map.class }), eq(true))).thenReturn(responses);

        when(response1.getResult()).thenReturn(Collections.singletonList(request1));
        when(response2.getResult()).thenReturn(Collections.singletonList(request2));

        List<MCMPRequest> results = this.source.getResetRequests(Collections
                .<String, Set<ResetRequestSource.VirtualHost>> emptyMap());

        assertEquals(2, results.size());
        assertSame(request1, results.get(0));
        assertSame(request2, results.get(1));
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Test
    public void getResetRequestsContextNotAutoEnabled() throws Exception {
        ContextFilter contextFilter = mock(ContextFilter.class);
        MCMPRequest request1 = mock(MCMPRequest.class);
        MCMPRequest request2 = mock(MCMPRequest.class);
        RpcResponse<List<MCMPRequest>> response1 = mock(RpcResponse.class);
        RpcResponse<List<MCMPRequest>> response2 = mock(RpcResponse.class);
        List<RpcResponse<List<MCMPRequest>>> responses = new ArrayList<RpcResponse<List<MCMPRequest>>>(Arrays.asList(response1,
                response2));

        Server server = mock(Server.class);
        Engine engine = mock(Engine.class);
        Context context = mock(Context.class);
        Context excludedContext = mock(Context.class);
        Host host = mock(Host.class);
        MCMPRequest configRequest = mock(MCMPRequest.class);
        MCMPRequest contextRequest = mock(MCMPRequest.class);

        this.source.init(server, contextFilter);

        when(this.singleton.isMasterNode()).thenReturn(true);

        when(contextFilter.getExcludedContexts())
                .thenReturn(Collections.singletonMap(host, Collections.singleton("/excluded")));
        when(contextFilter.isAutoEnableContexts()).thenReturn(false);

        when(server.getEngines()).thenReturn(Collections.singleton(engine));

        when(this.requestFactory.createConfigRequest(engine, this.nodeConfig, this.balancerConfig)).thenReturn(configRequest);

        when(engine.getJvmRoute()).thenReturn("host1");

        when(engine.getHosts()).thenReturn(Collections.singleton(host));
        when(host.getName()).thenReturn("host");
        when(host.getAliases()).thenReturn(new LinkedHashSet<String>(Arrays.asList("host", "alias1")));
        when(host.getContexts()).thenReturn(Arrays.asList(context, excludedContext));
        when(context.getPath()).thenReturn("/context");
        when(context.isStarted()).thenReturn(true);

        when(this.requestFactory.createDisableRequest(context)).thenReturn(contextRequest);

        when(excludedContext.getPath()).thenReturn("/excluded");

        when(this.key.getHAPartition()).thenReturn(this.partition);
        when(this.key.getHAServiceKey()).thenReturn("service:domain");
        when(
                (List<RpcResponse<List<MCMPRequest>>>) this.partition.callMethodOnCluster(eq("service:domain"),
                        eq("getResetRequests"),
                        aryEq(new Object[] { Collections.<String, Set<ResetRequestSource.VirtualHost>> emptyMap() }),
                        aryEq(new Class[] { Map.class }), eq(true))).thenReturn(responses);

        when(response1.getResult()).thenReturn(Collections.singletonList(request1));
        when(response2.getResult()).thenReturn(Collections.singletonList(request2));

        List<MCMPRequest> result = this.source.getResetRequests(Collections
                .<String, Set<ResetRequestSource.VirtualHost>> emptyMap());

        assertEquals(4, result.size());

        assertSame(configRequest, result.get(0));
        assertSame(contextRequest, result.get(1));
        assertSame(request1, result.get(2));
        assertSame(request2, result.get(3));
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Test
    public void getResetRequests() throws Exception {
        ContextFilter contextFilter = mock(ContextFilter.class);
        MCMPRequest request1 = mock(MCMPRequest.class);
        MCMPRequest request2 = mock(MCMPRequest.class);
        RpcResponse<List<MCMPRequest>> response1 = mock(RpcResponse.class);
        RpcResponse<List<MCMPRequest>> response2 = mock(RpcResponse.class);
        List<RpcResponse<List<MCMPRequest>>> responses = new ArrayList<RpcResponse<List<MCMPRequest>>>(Arrays.asList(response1,
                response2));

        Server server = mock(Server.class);
        Engine engine = mock(Engine.class);
        Context context = mock(Context.class);
        Context excludedContext = mock(Context.class);
        Host host = mock(Host.class);
        MCMPRequest configRequest = mock(MCMPRequest.class);
        MCMPRequest contextRequest = mock(MCMPRequest.class);

        this.source.init(server, contextFilter);

        when(this.singleton.isMasterNode()).thenReturn(true);

        when(contextFilter.getExcludedContexts())
                .thenReturn(Collections.singletonMap(host, Collections.singleton("/excluded")));
        when(contextFilter.isAutoEnableContexts()).thenReturn(true);

        when(server.getEngines()).thenReturn(Collections.singleton(engine));

        when(this.requestFactory.createConfigRequest(engine, this.nodeConfig, this.balancerConfig)).thenReturn(configRequest);

        when(engine.getJvmRoute()).thenReturn("host1");

        when(engine.getHosts()).thenReturn(Collections.singleton(host));
        when(host.getName()).thenReturn("host");
        when(host.getAliases()).thenReturn(new LinkedHashSet<String>(Arrays.asList("host", "alias1")));
        when(host.getContexts()).thenReturn(Arrays.asList(context, excludedContext));
        when(context.getPath()).thenReturn("/context");
        when(context.isStarted()).thenReturn(true);

        when(this.requestFactory.createEnableRequest(context)).thenReturn(contextRequest);

        when(excludedContext.getPath()).thenReturn("/excluded");

        when(this.key.getHAPartition()).thenReturn(this.partition);
        when(this.key.getHAServiceKey()).thenReturn("service:domain");
        when(
                (List<RpcResponse<List<MCMPRequest>>>) this.partition.callMethodOnCluster(eq("service:domain"),
                        eq("getResetRequests"),
                        aryEq(new Object[] { Collections.<String, Set<ResetRequestSource.VirtualHost>> emptyMap() }),
                        aryEq(new Class[] { Map.class }), eq(true))).thenReturn(responses);

        when(response1.getResult()).thenReturn(Collections.singletonList(request1));
        when(response2.getResult()).thenReturn(Collections.singletonList(request2));

        List<MCMPRequest> result = this.source.getResetRequests(Collections
                .<String, Set<ResetRequestSource.VirtualHost>> emptyMap());

        assertEquals(4, result.size());

        assertSame(configRequest, result.get(0));
        assertSame(contextRequest, result.get(1));
        assertSame(request1, result.get(2));
        assertSame(request2, result.get(3));
    }
}
