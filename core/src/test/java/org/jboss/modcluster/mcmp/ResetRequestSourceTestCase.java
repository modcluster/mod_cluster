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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Host;
import org.jboss.modcluster.container.Server;
import org.jboss.modcluster.config.BalancerConfiguration;
import org.jboss.modcluster.config.NodeConfiguration;
import org.jboss.modcluster.mcmp.impl.ResetRequestSourceImpl;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
public class ResetRequestSourceTestCase {
    private final NodeConfiguration nodeConfig = mock(NodeConfiguration.class);
    private final BalancerConfiguration balancerConfig = mock(BalancerConfiguration.class);
    private final MCMPRequestFactory requestFactory = mock(MCMPRequestFactory.class);

    private ResetRequestSource source = new ResetRequestSourceImpl(this.nodeConfig, this.balancerConfig, this.requestFactory);

    @Test
    public void getResetRequestsNoServer() {
        Server server = mock(Server.class);

        List<MCMPRequest> requests = this.source.getResetRequests(Collections
                .<String, Set<ResetRequestSource.VirtualHost>> emptyMap());

        assertTrue(requests.isEmpty());

        verifyZeroInteractions(server);
    }

    @Test
    public void getResetRequests() throws Exception {
        Server server = mock(Server.class);
        ContextFilter contextFilter = mock(ContextFilter.class);

        this.source.init(server, contextFilter);

        verifyZeroInteractions(server);

        Engine engine = mock(Engine.class);
        Host host = mock(Host.class);
        Context context = mock(Context.class);
        Context excludedContext = mock(Context.class);
        MCMPRequest configRequest = mock(MCMPRequest.class);
        MCMPRequest contextRequest = mock(MCMPRequest.class);

        when(contextFilter.getExcludedContexts(host)).thenReturn(Collections.singleton("/excluded"));
        when(contextFilter.isAutoEnableContexts()).thenReturn(true);

        when(server.getEngines()).thenReturn(Collections.singleton(engine));

        when(this.requestFactory.createConfigRequest(engine, this.nodeConfig, this.balancerConfig)).thenReturn(configRequest);

        when(engine.getJvmRoute()).thenReturn("host1");

        when(engine.getHosts()).thenReturn(Collections.singleton(host));
        when(host.getName()).thenReturn("host");
        when(host.getAliases()).thenReturn(new TreeSet<String>(Arrays.asList("alias1", "alias2")));
        when(host.getContexts()).thenReturn(Arrays.asList(context, excludedContext));
        when(context.getPath()).thenReturn("/context");
        when(context.isStarted()).thenReturn(true);

        when(this.requestFactory.createEnableRequest(context)).thenReturn(contextRequest);

        when(excludedContext.getPath()).thenReturn("/excluded");

        List<MCMPRequest> requests = this.source.getResetRequests(Collections
                .<String, Set<ResetRequestSource.VirtualHost>> emptyMap());

        assertEquals(2, requests.size());

        assertSame(configRequest, requests.get(0));
        assertSame(contextRequest, requests.get(1));
    }

    @Test
    public void getResetRequestsDisableContexts() throws Exception {
        Server server = mock(Server.class);
        ContextFilter contextFilter = mock(ContextFilter.class);

        this.source.init(server, contextFilter);

        verifyZeroInteractions(server);

        Engine engine = mock(Engine.class);
        Host host = mock(Host.class);
        Context context = mock(Context.class);
        Context excludedContext = mock(Context.class);
        MCMPRequest configRequest = mock(MCMPRequest.class);
        MCMPRequest contextRequest = mock(MCMPRequest.class);

        when(contextFilter.getExcludedContexts(host)).thenReturn(Collections.singleton("/excluded"));
        when(contextFilter.isAutoEnableContexts()).thenReturn(false);

        when(server.getEngines()).thenReturn(Collections.singleton(engine));

        when(this.requestFactory.createConfigRequest(engine, this.nodeConfig, this.balancerConfig)).thenReturn(configRequest);

        when(engine.getJvmRoute()).thenReturn("host1");

        when(engine.getHosts()).thenReturn(Collections.singleton(host));
        when(host.getName()).thenReturn("host");
        when(host.getAliases()).thenReturn(new TreeSet<String>(Arrays.asList("alias1", "alias2")));
        when(host.getContexts()).thenReturn(Arrays.asList(context, excludedContext));
        when(context.getPath()).thenReturn("/context");
        when(context.isStarted()).thenReturn(true);

        when(this.requestFactory.createDisableRequest(context)).thenReturn(contextRequest);

        when(excludedContext.getPath()).thenReturn("/excluded");

        List<MCMPRequest> requests = this.source.getResetRequests(Collections
                .<String, Set<ResetRequestSource.VirtualHost>> emptyMap());

        assertEquals(2, requests.size());

        assertSame(configRequest, requests.get(0));
        assertSame(contextRequest, requests.get(1));
    }
}
