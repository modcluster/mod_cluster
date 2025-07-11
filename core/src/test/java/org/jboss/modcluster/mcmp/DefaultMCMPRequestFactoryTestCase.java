/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.mcmp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.TreeSet;

import org.jboss.modcluster.config.BalancerConfiguration;
import org.jboss.modcluster.config.NodeConfiguration;
import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Host;
import org.jboss.modcluster.mcmp.impl.DefaultMCMPRequestFactory;
import org.junit.jupiter.api.Test;

/**
 * @author Paul Ferraro
 */
class DefaultMCMPRequestFactoryTestCase {
    private final MCMPRequestFactory factory = new DefaultMCMPRequestFactory();

    @Test
    void createEnableRequestContext() {
        Context context = mock(Context.class);
        Host host = mock(Host.class);
        Engine engine = mock(Engine.class);

        when(context.getHost()).thenReturn(host);
        when(host.getEngine()).thenReturn(engine);
        when(engine.getJvmRoute()).thenReturn("host1");
        when(host.getAliases()).thenReturn(new TreeSet<String>(Arrays.asList("alias1", "alias2")));
        when(context.getPath()).thenReturn("/context");

        MCMPRequest request = this.factory.createEnableRequest(context);

        assertSame(MCMPRequestType.ENABLE_APP, request.getRequestType());
        assertFalse(request.isWildcard());
        assertEquals("host1", request.getJvmRoute());

        Map<String, String> parameters = request.getParameters();

        assertEquals(2, parameters.size());

        assertEquals("/context", parameters.get("Context"));
        assertEquals("alias1,alias2", parameters.get("Alias"));
    }

    @Test
    void createDisableRequestContext() {
        Context context = mock(Context.class);
        Host host = mock(Host.class);
        Engine engine = mock(Engine.class);

        when(context.getHost()).thenReturn(host);
        when(host.getEngine()).thenReturn(engine);
        when(engine.getJvmRoute()).thenReturn("host1");
        when(host.getAliases()).thenReturn(new TreeSet<String>(Arrays.asList("alias1", "alias2")));
        when(context.getPath()).thenReturn("/context");

        MCMPRequest request = this.factory.createDisableRequest(context);

        assertSame(MCMPRequestType.DISABLE_APP, request.getRequestType());
        assertFalse(request.isWildcard());
        assertEquals("host1", request.getJvmRoute());

        Map<String, String> parameters = request.getParameters();

        assertEquals(2, parameters.size());

        assertEquals("/context", parameters.get("Context"));
        assertEquals("alias1,alias2", parameters.get("Alias"));
    }

    @Test
    void createStopRequest() {
        Context context = mock(Context.class);
        Host host = mock(Host.class);
        Engine engine = mock(Engine.class);

        when(context.getHost()).thenReturn(host);
        when(host.getEngine()).thenReturn(engine);
        when(engine.getJvmRoute()).thenReturn("host1");
        when(host.getAliases()).thenReturn(new TreeSet<String>(Arrays.asList("alias1", "alias2")));
        when(context.getPath()).thenReturn("/context");

        MCMPRequest request = this.factory.createStopRequest(context);

        assertSame(MCMPRequestType.STOP_APP, request.getRequestType());
        assertFalse(request.isWildcard());
        assertEquals("host1", request.getJvmRoute());

        Map<String, String> parameters = request.getParameters();

        assertEquals(2, parameters.size());

        assertEquals("/context", parameters.get("Context"));
        assertEquals("alias1,alias2", parameters.get("Alias"));
    }

    @Test
    void createRemoveRequestContext() {
        Context context = mock(Context.class);
        Host host = mock(Host.class);
        Engine engine = mock(Engine.class);

        when(context.getHost()).thenReturn(host);
        when(host.getEngine()).thenReturn(engine);
        when(engine.getJvmRoute()).thenReturn("host1");
        when(host.getAliases()).thenReturn(new TreeSet<String>(Arrays.asList("alias1", "alias2")));
        when(context.getPath()).thenReturn("/context");

        MCMPRequest request = this.factory.createRemoveRequest(context);

        assertSame(MCMPRequestType.REMOVE_APP, request.getRequestType());
        assertFalse(request.isWildcard());
        assertEquals("host1", request.getJvmRoute());

        Map<String, String> parameters = request.getParameters();

        assertEquals(2, parameters.size());

        assertEquals("/context", parameters.get("Context"));
        assertEquals("alias1,alias2", parameters.get("Alias"));
    }

    @Test
    void createStatusRequest() {
        MCMPRequest request = this.factory.createStatusRequest("route", 10);

        assertSame(MCMPRequestType.STATUS, request.getRequestType());
        assertFalse(request.isWildcard());
        assertEquals("route", request.getJvmRoute());

        Map<String, String> parameters = request.getParameters();

        assertEquals(1, parameters.size());
        assertEquals("10", parameters.get("Load"));
    }

    @Test
    void createConfigRequest() throws Exception {
        Engine engine = mock(Engine.class);
        NodeConfiguration nodeConfig = mock(NodeConfiguration.class);
        BalancerConfiguration balancerConfig = mock(BalancerConfiguration.class);
        Connector connector = mock(Connector.class);

        when(engine.getProxyConnector()).thenReturn(connector);
        when(connector.isReverse()).thenReturn(true);
        when(connector.getAddress()).thenReturn(InetAddress.getLocalHost());
        when(connector.getPort()).thenReturn(100);
        when(connector.getType()).thenReturn(Connector.Type.AJP);

        when(nodeConfig.getLoadBalancingGroup()).thenReturn("lb-group");
        when(nodeConfig.getFlushPackets()).thenReturn(Boolean.TRUE);
        when(nodeConfig.getFlushWait()).thenReturn(1);
        when(nodeConfig.getPing()).thenReturn(2);
        when(nodeConfig.getSmax()).thenReturn(3);
        when(nodeConfig.getTtl()).thenReturn(4);
        when(nodeConfig.getNodeTimeout()).thenReturn(5);
        when(nodeConfig.getBalancer()).thenReturn("S");

        when(engine.getSessionCookieName()).thenReturn(DefaultMCMPRequestFactory.DEFAULT_SESSION_COOKIE_NAME);
        when(engine.getSessionParameterName()).thenReturn(DefaultMCMPRequestFactory.DEFAULT_SESSION_PARAMETER_NAME);

        when(balancerConfig.getStickySession()).thenReturn(Boolean.FALSE);
        when(balancerConfig.getStickySessionRemove()).thenReturn(Boolean.TRUE);
        when(balancerConfig.getStickySessionForce()).thenReturn(Boolean.FALSE);
        when(balancerConfig.getWorkerTimeout()).thenReturn(6);
        when(balancerConfig.getMaxAttempts()).thenReturn(7);

        when(engine.getJvmRoute()).thenReturn("host1");

        MCMPRequest request = this.factory.createConfigRequest(engine, nodeConfig, balancerConfig);

        assertSame(MCMPRequestType.CONFIG, request.getRequestType());
        assertFalse(request.isWildcard());
        assertEquals("host1", request.getJvmRoute());

        Map<String, String> parameters = request.getParameters();

        assertEquals(17, parameters.size());
        assertEquals("true", parameters.get("Reversed"));
        assertTrue(InetAddress.getLocalHost().getHostName().equalsIgnoreCase(parameters.get("Host")));
        assertEquals("100", parameters.get("Port"));
        assertEquals("ajp", parameters.get("Type"));
        assertEquals("lb-group", parameters.get("Domain"));
        assertEquals("On", parameters.get("flushpackets"));
        assertEquals("1", parameters.get("flushwait"));
        assertEquals("2", parameters.get("ping"));
        assertEquals("3", parameters.get("smax"));
        assertEquals("4", parameters.get("ttl"));
        assertEquals("5", parameters.get("Timeout"));
        assertEquals("S", parameters.get("Balancer"));
        assertEquals("No", parameters.get("StickySession"));
        assertEquals("Yes", parameters.get("StickySessionRemove"));
        assertEquals("No", parameters.get("StickySessionForce"));
        assertEquals("6", parameters.get("WaitWorker"));
        assertEquals("7", parameters.get("Maxattempts"));
    }

    @Test
    void createInfoRequest() {
        MCMPRequest request = this.factory.createInfoRequest();

        assertSame(MCMPRequestType.INFO, request.getRequestType());
        assertFalse(request.isWildcard());
        assertNull(request.getJvmRoute());
        assertTrue(request.getParameters().isEmpty());
    }

    @Test
    void createDumpRequest() {
        MCMPRequest request = this.factory.createDumpRequest();

        assertSame(MCMPRequestType.DUMP, request.getRequestType());
        assertTrue(request.isWildcard());
        assertNull(request.getJvmRoute());
        assertTrue(request.getParameters().isEmpty());
    }

    @Test
    void createDisableRequestEngine() {
        Engine engine = mock(Engine.class);

        when(engine.getJvmRoute()).thenReturn("route");

        MCMPRequest request = this.factory.createDisableRequest(engine);

        assertSame(MCMPRequestType.DISABLE_APP, request.getRequestType());
        assertTrue(request.isWildcard());
        assertEquals("route", request.getJvmRoute());
        assertTrue(request.getParameters().isEmpty());
    }

    @Test
    void createEnableRequestEngine() {
        Engine engine = mock(Engine.class);

        when(engine.getJvmRoute()).thenReturn("route");

        MCMPRequest request = this.factory.createEnableRequest(engine);

        assertSame(MCMPRequestType.ENABLE_APP, request.getRequestType());
        assertTrue(request.isWildcard());
        assertEquals("route", request.getJvmRoute());
        assertTrue(request.getParameters().isEmpty());
    }

    @Test
    void createRemoveRequestEngine() {
        Engine engine = mock(Engine.class);

        when(engine.getJvmRoute()).thenReturn("route");

        MCMPRequest request = this.factory.createRemoveRequest(engine);

        assertSame(MCMPRequestType.REMOVE_APP, request.getRequestType());
        assertTrue(request.isWildcard());
        assertEquals("route", request.getJvmRoute());
        assertTrue(request.getParameters().isEmpty());
    }

    @Test
    void createRemoveContextRequest() {
        String route = "route";
        String path = "path";

        MCMPRequest request = this.factory.createRemoveContextRequest(route,
                new LinkedHashSet<String>(Arrays.asList("alias1", "alias2")), path);

        assertSame(MCMPRequestType.REMOVE_APP, request.getRequestType());
        assertFalse(request.isWildcard());
        assertSame(route, request.getJvmRoute());

        Map<String, String> parameters = request.getParameters();

        assertEquals(2, parameters.size());
        assertEquals("alias1,alias2", parameters.get("Alias"));
        assertSame(path, parameters.get("Context"));
    }

    @Test
    void createRemoveEngineRequest() {
        String route = "route";

        MCMPRequest request = this.factory.createRemoveEngineRequest(route);

        assertSame(MCMPRequestType.REMOVE_APP, request.getRequestType());
        assertTrue(request.isWildcard());
        assertSame(route, request.getJvmRoute());

        assertTrue(request.getParameters().isEmpty());
    }

    @Test
    void createPingRequest() {
        MCMPRequest request = this.factory.createPingRequest();

        assertSame(MCMPRequestType.PING, request.getRequestType());
        assertFalse(request.isWildcard());
        assertNull(request.getJvmRoute());
        assertTrue(request.getParameters().isEmpty());
    }

    @Test
    void createJvmRoutePingRequest() {
        String jvmRoute = "route";

        MCMPRequest request = this.factory.createPingRequest(jvmRoute);

        assertSame(MCMPRequestType.PING, request.getRequestType());
        assertFalse(request.isWildcard());
        assertSame(jvmRoute, request.getJvmRoute());
        assertTrue(request.getParameters().isEmpty());
    }

    @Test
    void createURIPingRequest() {
        MCMPRequest request = this.factory.createPingRequest("ajp", "localhost", 8009);

        assertSame(MCMPRequestType.PING, request.getRequestType());
        assertFalse(request.isWildcard());
        assertNull(request.getJvmRoute());

        Map<String, String> parameters = request.getParameters();
        assertEquals(3, parameters.size());
        assertEquals("ajp", parameters.get("Scheme"));
        assertEquals("localhost", parameters.get("Host"));
        assertEquals("8009", parameters.get("Port"));
    }
}
