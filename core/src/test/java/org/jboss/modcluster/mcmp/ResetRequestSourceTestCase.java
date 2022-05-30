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
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Host;
import org.jboss.modcluster.container.Server;
import org.jboss.modcluster.config.BalancerConfiguration;
import org.jboss.modcluster.config.NodeConfiguration;
import org.jboss.modcluster.mcmp.impl.DefaultMCMPHandler;
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

    private final ResetRequestSource source = new ResetRequestSourceImpl(this.nodeConfig, this.balancerConfig, this.requestFactory);

    private final MCMPRequest configRequest = mock(MCMPRequest.class);
    private final MCMPRequest enableRequest = mock(MCMPRequest.class);
    private final MCMPRequest disableRequest = mock(MCMPRequest.class);
    private final MCMPRequest stopRequest = mock(MCMPRequest.class);

    private static final String JVM_ROUTE = "host1";
    private static final String HOST_NAME = "host";
    private static final String ALIAS1 = "alias1";
    private static final String ALIAS2 = "alias2";

    @Test
    public void getResetRequestsNoServer() {
        Server server = mock(Server.class);

        List<MCMPRequest> requests = this.source.getResetRequests(Collections.emptyMap());

        assertTrue(requests.isEmpty());

        verifyNoInteractions(server);
    }

    @Test
    public void getResetRequests() {
        setupMocks(true, true);

        List<MCMPRequest> requests = this.source.getResetRequests(Collections
                .<String, Set<ResetRequestSource.VirtualHost>> emptyMap());

        assertEquals(2, requests.size());

        assertSame(this.configRequest, requests.get(0));
        assertSame(this.enableRequest, requests.get(1));
    }

    @Test
    public void getResetRequestsDisableContexts() {
        setupMocks(false, true);

        List<MCMPRequest> requests = this.source.getResetRequests(Collections
                .<String, Set<ResetRequestSource.VirtualHost>> emptyMap());

        assertEquals(2, requests.size());

        assertSame(configRequest, requests.get(0));
        assertSame(disableRequest, requests.get(1));
    }

    @Test
    public void getResetRequestsContextStoppedNoProxyStatus() {
        setupMocks(true, false);

        List<MCMPRequest> requests = this.source.getResetRequests(Collections
                .<String, Set<ResetRequestSource.VirtualHost>> emptyMap());

        assertEquals(2, requests.size());

        assertSame(configRequest, requests.get(0));
        assertSame(stopRequest, requests.get(1));
    }

    @Test
    public void getResetRequestsContextStoppedProxyStatusEnabled() {
        setupMocks(true, false);

        Map<String, Set<ResetRequestSource.VirtualHost>> infoResponse = createInfoResponse(
                ResetRequestSource.Status.ENABLED);

        List<MCMPRequest> requests = this.source.getResetRequests(infoResponse);

        assertEquals(2, requests.size());

        assertSame(configRequest, requests.get(0));
        assertSame(stopRequest, requests.get(1));
    }

    @Test
    public void getResetRequestsContextStartedProxyStatusDisabled() {
        setupMocks(true, true);
        Map<String, Set<ResetRequestSource.VirtualHost>> infoResponse = createInfoResponse(
                ResetRequestSource.Status.DISABLED);

        List<MCMPRequest> requests = this.source.getResetRequests(infoResponse);

        assertEquals(2, requests.size());

        assertSame(configRequest, requests.get(0));
        assertSame(enableRequest, requests.get(1));
    }

    private void setupMocks(boolean autoEnableContexts, boolean contextsAreStarted) {
        Server server = mock(Server.class);
        ContextFilter contextFilter = mock(ContextFilter.class);

        this.source.init(server, contextFilter);

        verifyNoInteractions(server);

        Engine engine = mock(Engine.class);
        Host host = mock(Host.class);
        Context context = mock(Context.class);
        Context excludedContext = mock(Context.class);

        when(contextFilter.getExcludedContexts(host)).thenReturn(Collections.singleton("/excluded"));
        when(contextFilter.isAutoEnableContexts()).thenReturn(autoEnableContexts);

        when(server.getEngines()).thenReturn(Collections.singleton(engine));

        when(engine.getJvmRoute()).thenReturn(JVM_ROUTE);
        when(engine.getProxyConnector()).thenReturn(mock(Connector.class));

        when(engine.getHosts()).thenReturn(Collections.singleton(host));
        when(host.getName()).thenReturn(HOST_NAME);
        when(host.getAliases()).thenReturn(new TreeSet<String>(Arrays.asList(HOST_NAME, ALIAS1, ALIAS2)));
        when(host.getContexts()).thenReturn(Arrays.asList(context, excludedContext));
        when(context.getPath()).thenReturn("/context");
        when(context.isStarted()).thenReturn(contextsAreStarted);

        when(this.requestFactory.createConfigRequest(engine, this.nodeConfig, this.balancerConfig))
                .thenReturn(this.configRequest);
        when(this.requestFactory.createEnableRequest(context)).thenReturn(this.enableRequest);
        when(this.requestFactory.createDisableRequest(context)).thenReturn(this.disableRequest);
        when(this.requestFactory.createStopRequest(context)).thenReturn(this.stopRequest);

        when(excludedContext.getPath()).thenReturn("/excluded");
    }

    private Map<String, Set<ResetRequestSource.VirtualHost>> createInfoResponse(ResetRequestSource.Status contextStatus) {
        DefaultMCMPHandler.VirtualHostImpl virtualHost = new DefaultMCMPHandler.VirtualHostImpl();
        virtualHost.getAliases().add(HOST_NAME);
        virtualHost.getAliases().add(ALIAS1);
        virtualHost.getAliases().add(ALIAS2);
        virtualHost.getContexts().put("/context", contextStatus);
        return Collections.singletonMap(JVM_ROUTE, Collections.singleton(virtualHost));
    }

}
