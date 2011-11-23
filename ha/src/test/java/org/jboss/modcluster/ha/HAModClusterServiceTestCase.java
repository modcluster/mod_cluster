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
import static org.mockito.Mockito.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Iterator;

import org.jboss.ha.framework.interfaces.ClusterNode;
import org.jboss.ha.framework.interfaces.DistributedReplicantManager;
import org.jboss.ha.framework.interfaces.HAPartition;
import org.jboss.ha.framework.interfaces.HASingletonElectionPolicy;
import org.jboss.ha.framework.server.EventFactory;
import org.jboss.ha.framework.server.HAServiceEvent;
import org.jboss.ha.framework.server.SimpleCachableMarshalledValue;
import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Host;
import org.jboss.modcluster.config.JvmRouteFactory;
import org.jboss.modcluster.container.Server;
import org.jboss.modcluster.advertise.AdvertiseListenerFactory;
import org.jboss.modcluster.config.BalancerConfiguration;
import org.jboss.modcluster.config.MCMPHandlerConfiguration;
import org.jboss.modcluster.config.NodeConfiguration;
import org.jboss.modcluster.config.ha.HAConfiguration;
import org.jboss.modcluster.load.LoadBalanceFactorProvider;
import org.jboss.modcluster.load.LoadBalanceFactorProviderFactory;
import org.jboss.modcluster.mcmp.ContextFilter;
import org.jboss.modcluster.mcmp.MCMPHandler;
import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPRequestFactory;
import org.jboss.modcluster.mcmp.MCMPResponseParser;
import org.jboss.modcluster.mcmp.MCMPServerState;
import org.jboss.modcluster.mcmp.ResetRequestSource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;

/**
 * @author Paul Ferraro
 * 
 */
@SuppressWarnings({ "boxing" })
public class HAModClusterServiceTestCase {
    private final HAPartition partition = mock(HAPartition.class);
    @SuppressWarnings("unchecked")
    private final EventFactory<HAServiceEvent> eventFactory = mock(EventFactory.class);
    private final HAConfiguration haConfig = mock(HAConfiguration.class);
    private final ResetRequestSource resetRequestSource = mock(ResetRequestSource.class);
    private final ClusteredMCMPHandler clusteredMCMPHandler = mock(ClusteredMCMPHandler.class);
    private final HASingletonElectionPolicy electionPolicy = mock(HASingletonElectionPolicy.class);
    private final ClusterNode node = mock(ClusterNode.class);
    private final NodeConfiguration nodeConfig = mock(NodeConfiguration.class);
    private final BalancerConfiguration balancerConfig = mock(BalancerConfiguration.class);
    private final MCMPHandlerConfiguration mcmpConfig = mock(MCMPHandlerConfiguration.class);
    private final MCMPHandler mcmpHandler = mock(MCMPHandler.class);
    private final MCMPRequestFactory requestFactory = mock(MCMPRequestFactory.class);
    private final MCMPResponseParser responseParser = mock(MCMPResponseParser.class);
    private final LoadBalanceFactorProviderFactory lbfProviderFactory = mock(LoadBalanceFactorProviderFactory.class);
    private final LoadBalanceFactorProvider lbfProvider = mock(LoadBalanceFactorProvider.class);
    private final AdvertiseListenerFactory advertiseListenerFactory = mock(AdvertiseListenerFactory.class);

    private static final boolean MASTER_PER_DOMAIN = true;
    private static final String SERVICE_HA_NAME = "myservice";
    private static final String DOMAIN = "domain";

    private HAModClusterService service;

    @Before
    public void setUp() throws Exception {
        when(this.haConfig.isMasterPerLoadBalancingGroup()).thenReturn(MASTER_PER_DOMAIN);
        when(this.nodeConfig.getLoadBalancingGroup()).thenReturn(DOMAIN);

        when(this.partition.getClusterNode()).thenReturn(this.node);

        this.service = new HAModClusterService(this.eventFactory, this.haConfig, this.nodeConfig, this.balancerConfig,
                this.mcmpConfig, this.lbfProviderFactory, this.partition, this.electionPolicy, this.requestFactory,
                this.responseParser, this.resetRequestSource, this.mcmpHandler, this.clusteredMCMPHandler,
                this.advertiseListenerFactory);
        this.service.setServiceHAName(SERVICE_HA_NAME);
    }

    private void init(Server server, Engine engine, Host host) {
        InetAddress localAddress = this.getLocalAddress();
        String localHostName = localAddress.getHostAddress();
        InetSocketAddress socketAddress = new InetSocketAddress(localAddress, 8000);

        // Test advertise = false
        when(this.mcmpConfig.getProxyList()).thenReturn(localHostName);

        when(this.mcmpConfig.isAutoEnableContexts()).thenReturn(true);
        when(this.mcmpConfig.getExcludedContexts()).thenReturn("ignored");

        when(server.getEngines()).thenReturn(Collections.singleton(engine));
        when(engine.getHosts()).thenReturn(Collections.singleton(host));
        when(host.getName()).thenReturn("localhost");

        when(this.lbfProviderFactory.createLoadBalanceFactorProvider()).thenReturn(this.lbfProvider);

        when(this.mcmpConfig.getAdvertise()).thenReturn(false);

        this.service.init(server);

        verify(this.clusteredMCMPHandler).init(eq(Collections.singletonList(socketAddress)), isA(HAModClusterService.ClusteredModClusterService.class));
        verify(this.resetRequestSource).init(same(server), Matchers.<ContextFilter>any());
    }

    private InetAddress getLocalAddress() {
        try {
            return InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }

    private InetAddress getWildcardAddress() {
        try {
            return InetAddress.getByName("0.0.0.0");
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }

    private void establishConnection(Server server) throws Exception {
        this.establishConnection(server, mock(Engine.class), mock(Host.class));
    }

    private void establishConnection(Server server, Engine engine, Host host) throws Exception {
        this.init(server, engine, host);

        InetAddress localAddress = this.getLocalAddress();
        Connector connector = mock(Connector.class);
        JvmRouteFactory factory = mock(JvmRouteFactory.class);
        DistributedReplicantManager drm = mock(DistributedReplicantManager.class);
        ArgumentCaptor<SimpleCachableMarshalledValue> capturedMarshalledValue = ArgumentCaptor
                .forClass(SimpleCachableMarshalledValue.class);

        when(server.getEngines()).thenReturn(Collections.singleton(engine));

        when(engine.getProxyConnector()).thenReturn(connector);
        when(connector.getAddress()).thenReturn(this.getWildcardAddress());

        doNothing().when(connector).setAddress(localAddress);

        when(engine.getJvmRoute()).thenReturn(null);
        when(this.mcmpConfig.getJvmRouteFactory()).thenReturn(factory);
        when(factory.createJvmRoute(same(engine))).thenReturn("jvm-route");

        doNothing().when(engine).setJvmRoute("jvm-route");

        when(engine.getJvmRoute()).thenReturn("jvm-route");
        when(this.partition.getDistributedReplicantManager()).thenReturn(drm);

        this.service.connectionEstablished(localAddress);

        verify(drm).add(eq(SERVICE_HA_NAME + ":domain"), capturedMarshalledValue.capture());

        SimpleCachableMarshalledValue marshalledValue = capturedMarshalledValue.getValue();

        Object value = marshalledValue.get();
        assertTrue(value instanceof ModClusterServiceDRMEntry);
        ModClusterServiceDRMEntry entry = (ModClusterServiceDRMEntry) value;
        Iterator<String> routes = entry.getJvmRoutes().iterator();
        assertTrue(routes.hasNext());
        assertEquals("jvm-route", routes.next());
        assertFalse(routes.hasNext());
        assertSame(this.node, entry.getPeer());
        assertNull(entry.getMCMPServerStates());
    }

    @Test
    public void addProxy() {
        ArgumentCaptor<InetSocketAddress> capturedSocketAddress = ArgumentCaptor.forClass(InetSocketAddress.class);

        String host = "127.0.0.1";
        int port = 0;

        this.service.addProxy(host, port);

        verify(this.clusteredMCMPHandler).addProxy(capturedSocketAddress.capture());

        InetSocketAddress socketAddress = capturedSocketAddress.getValue();
        assertEquals(host, socketAddress.getAddress().getHostAddress());
        assertEquals(port, socketAddress.getPort());
    }

    @Test
    public void removeProxy() {
        ArgumentCaptor<InetSocketAddress> capturedSocketAddress = ArgumentCaptor.forClass(InetSocketAddress.class);

        String host = "127.0.0.1";
        int port = 0;

        this.service.removeProxy(host, port);

        verify(this.clusteredMCMPHandler).removeProxy(capturedSocketAddress.capture());

        InetSocketAddress socketAddress = capturedSocketAddress.getValue();
        assertEquals(host, socketAddress.getAddress().getHostAddress());
        assertEquals(port, socketAddress.getPort());
    }

    @Test
    public void reset() throws Exception {
        Server server = mock(Server.class);

        this.establishConnection(server);

        this.service.reset();

        verify(this.clusteredMCMPHandler).reset();
    }

    @Test
    public void refresh() throws Exception {
        Server server = mock(Server.class);

        this.establishConnection(server);

        this.service.refresh();

        verify(this.clusteredMCMPHandler).markProxiesInError();
    }

    @Test
    public void enable() throws Exception {
        Server server = mock(Server.class);

        this.establishConnection(server);

        Engine engine = mock(Engine.class);
        MCMPRequest request = mock(MCMPRequest.class);

        when(server.getEngines()).thenReturn(Collections.singleton(engine));

        when(this.requestFactory.createEnableRequest(engine)).thenReturn(request);

        when(this.clusteredMCMPHandler.sendRequest(request)).thenReturn(Collections.<MCMPServerState, String> emptyMap());

        when(this.clusteredMCMPHandler.isProxyHealthOK()).thenReturn(true);

        boolean result = this.service.enable();

        assertTrue(result);
    }

    @Test
    public void disable() throws Exception {
        Server server = mock(Server.class);

        this.establishConnection(server);

        Engine engine = mock(Engine.class);
        MCMPRequest request = mock(MCMPRequest.class);

        when(server.getEngines()).thenReturn(Collections.singleton(engine));

        when(this.requestFactory.createDisableRequest(engine)).thenReturn(request);

        when(this.clusteredMCMPHandler.sendRequest(request)).thenReturn(Collections.<MCMPServerState, String> emptyMap());

        when(this.clusteredMCMPHandler.isProxyHealthOK()).thenReturn(true);

        boolean result = this.service.disable();

        assertTrue(result);
    }

    @Test
    public void enableContext() throws Exception {
        String hostName = "host1";
        String path = "/context";

        Server server = mock(Server.class);
        Engine engine = mock(Engine.class);
        Host host = mock(Host.class);

        this.establishConnection(server, engine, host);

        Context context = mock(Context.class);
        MCMPRequest request = mock(MCMPRequest.class);

        when(server.getEngines()).thenReturn(Collections.singleton(engine));
        when(engine.findHost(hostName)).thenReturn(host);
        when(host.findContext(path)).thenReturn(context);

        when(this.requestFactory.createEnableRequest(context)).thenReturn(request);

        when(this.clusteredMCMPHandler.sendRequest(request)).thenReturn(Collections.<MCMPServerState, String> emptyMap());

        when(this.clusteredMCMPHandler.isProxyHealthOK()).thenReturn(true);

        boolean result = this.service.enableContext(hostName, path);

        assertTrue(result);
    }

    @Test
    public void disableContext() throws Exception {
        String hostName = "host1";
        String path = "/context";

        Server server = mock(Server.class);
        Engine engine = mock(Engine.class);
        Host host = mock(Host.class);

        this.establishConnection(server, engine, host);

        Context context = mock(Context.class);
        MCMPRequest request = mock(MCMPRequest.class);

        when(server.getEngines()).thenReturn(Collections.singleton(engine));
        when(engine.findHost(hostName)).thenReturn(host);
        when(host.findContext(path)).thenReturn(context);

        when(this.requestFactory.createDisableRequest(context)).thenReturn(request);

        when(this.clusteredMCMPHandler.sendRequest(request)).thenReturn(Collections.<MCMPServerState, String> emptyMap());

        when(this.clusteredMCMPHandler.isProxyHealthOK()).thenReturn(true);

        boolean result = this.service.disableContext(hostName, path);

        assertTrue(result);
    }
    /*
     * @Test public void getProxyConfiguration() throws Exception { String configuration = "configuration";
     * 
     * // Test master use case when(this.singleton.isMasterNode()).thenReturn(true);
     * 
     * when(this.localHandler.getProxyConfiguration()).thenReturn(configuration);
     * 
     * EasyMock.replay(this.localHandler, this.singleton);
     * 
     * String result = this.handler.getProxyConfiguration();
     * 
     * EasyMock.verify(this.localHandler, this.singleton);
     * 
     * assertSame(configuration, result);
     * 
     * EasyMock.reset(this.localHandler, this.singleton);
     * 
     * 
     * // Test non-master use case String key = "key"; ClusterNode node = mock(ClusterNode.class); RpcResponse<String> response1
     * = mock(RpcResponse.class); RpcResponse<String> response2 = mock(RpcResponse.class); ArrayList<RpcResponse<String>>
     * responses = new ArrayList<RpcResponse<String>>(Arrays.asList(response1, response2));
     * 
     * when(this.singleton.isMasterNode()).thenReturn(false);
     * 
     * when(this.keyProvider.getHAPartition()).thenReturn(this.partition);
     * when(this.keyProvider.getHAServiceKey()).thenReturn(key);
     * 
     * when(this.partition.callMethodOnCluster(same(key), eq("getProxyConfiguration"), EasyMock.aryEq(new Object[0]),
     * EasyMock.aryEq(new Class[0]), eq(false), EasyMock.isA(RpcResponseFilter.class))).thenReturn(responses);
     * 
     * when(response1.getResult()).thenReturn(configuration);
     * 
     * EasyMock.replay(this.keyProvider, this.partition, this.singleton, response1, response2);
     * 
     * result = this.handler.getProxyConfiguration();
     * 
     * EasyMock.verify(this.keyProvider, this.partition, this.singleton, response1, response2);
     * 
     * assertSame(configuration, result);
     * 
     * EasyMock.reset(this.keyProvider, this.partition, this.singleton, response1, response2); }
     * 
     * @Test public void init() throws UnknownHostException { InetAddress localAddress = InetAddress.getLocalHost(); String
     * localHostName = localAddress.getHostName();
     * 
     * AdvertiseListener listener = mock(AdvertiseListener.class);
     * 
     * // Test advertise = false when(this.mcmpConfig.getProxyList()).thenReturn(localHostName);
     * 
     * this.clusteredMCMPHandler.init(Collections.singletonList(new AddressPort(localAddress, 8000)));
     * 
     * when(this.mcmpConfig.getAdvertise()).thenReturn(Boolean.FALSE);
     * 
     * EasyMock.replay(this.clusteredMCMPHandler, this.mcmpConfig, listener);
     * 
     * this.listener.init();
     * 
     * EasyMock.verify(this.clusteredMCMPHandler, this.mcmpConfig, listener); EasyMock.reset(this.clusteredMCMPHandler,
     * this.mcmpConfig, listener);
     * 
     * 
     * // Test advertise = null, proxies configured when(this.mcmpConfig.getProxyList()).thenReturn(localHostName);
     * 
     * this.clusteredMCMPHandler.init(Collections.singletonList(new AddressPort(localAddress, 8000)));
     * 
     * when(this.mcmpConfig.getAdvertise()).thenReturn(null);
     * 
     * EasyMock.replay(this.clusteredMCMPHandler, this.mcmpConfig, listener);
     * 
     * this.listener.init();
     * 
     * EasyMock.verify(this.clusteredMCMPHandler, this.mcmpConfig, listener); EasyMock.reset(this.clusteredMCMPHandler,
     * this.mcmpConfig, listener); }
     * 
     * @Test public void startServer() throws Exception { Server server = mock(Server.class);
     * 
     * this.resetRequestSource.setJbossWebServer(server);
     * 
     * EasyMock.replay(this.resetRequestSource);
     * 
     * // Test not initialized try { this.listener.startServer(server);
     * 
     * Assert.fail(); } catch (IllegalStateException e) { // Expected }
     * 
     * EasyMock.verify(this.resetRequestSource); EasyMock.reset(this.resetRequestSource);
     * 
     * init();
     * 
     * // Test initialized Service service = mock(Service.class); Engine engine = mock(Engine.class); Container container =
     * mock(Container.class); Context context = mock(Context.class); DistributedReplicantManager drm =
     * mock(DistributedReplicantManager.class); Capture<MCMPRequest> capturedRequest = new Capture<MCMPRequest>(); Connector
     * connector = new Connector("AJP/1.3");
     * 
     * this.resetRequestSource.setJbossWebServer(server);
     * 
     * when(server.findServices()).thenReturn(new Service[] { service });
     * 
     * when(service.getContainer()).thenReturn(engine);
     * 
     * // Expect log message when(engine.getName()).thenReturn("engine");
     * 
     * when(engine.getService()).thenReturn(service); when(service.findConnectors()).thenReturn(new Connector[] { connector });
     * when(engine.getJvmRoute()).thenReturn(null); Set<MCMPServerState> states = Collections.emptySet();
     * when(this.mcmpHandler.getProxyStates()).thenReturn(states);
     * 
     * when(engine.getJvmRoute()).thenReturn("route"); when(this.partition.getDistributedReplicantManager()).thenReturn(drm);
     * drm.add("myservice:domain", this.listener.drmEntry);
     * 
     * when(engine.getJvmRoute()).thenReturn("host1"); when(engine.getService()).thenReturn(service);
     * when(service.findConnectors()).thenReturn(new Connector[] { connector });
     * 
     * when(this.nodeConfig.getDomain()).thenReturn("domain"); when(this.nodeConfig.getFlushPackets()).thenReturn(Boolean.TRUE);
     * when(this.nodeConfig.getFlushWait()).thenReturn(1); when(this.nodeConfig.getPing()).thenReturn(2);
     * when(this.nodeConfig.getSmax()).thenReturn(3); when(this.nodeConfig.getTtl()).thenReturn(4);
     * when(this.nodeConfig.getNodeTimeout()).thenReturn(5); when(this.nodeConfig.getBalancer()).thenReturn("S");
     * 
     * when(this.balancerConfig.getStickySession()).thenReturn(Boolean.FALSE);
     * when(this.balancerConfig.getStickySessionRemove()).thenReturn(Boolean.TRUE);
     * when(this.balancerConfig.getStickySessionForce()).thenReturn(Boolean.FALSE);
     * when(this.balancerConfig.getWorkerTimeout()).thenReturn(6); when(this.balancerConfig.getMaxAttempts()).thenReturn(7);
     * 
     * this.clusteredMCMPHandler.sendRequest(EasyMock.capture(capturedRequest));
     * 
     * when(engine.findChildren()).thenReturn(new Container[] { container }); when(container.findChildren()).thenReturn(new
     * Container[] { context }); this.recordAddContext(context, container);
     * 
     * EasyMock.replay(this.partition, this.resetRequestSource, this.mcmpHandler, this.clusteredMCMPHandler, this.nodeConfig,
     * this.balancerConfig, drm, server, service, engine, container, context);
     * 
     * this.listener.startServer(server);
     * 
     * EasyMock.verify(this.partition, this.resetRequestSource, this.mcmpHandler, this.clusteredMCMPHandler, this.nodeConfig,
     * this.balancerConfig, drm, server, service, engine, container, context);
     * 
     * MCMPRequest request = capturedRequest.getValue();
     * 
     * assertSame(MCMPRequestType.CONFIG, request.getRequestType()); assertFalse(request.isWildcard()); assertEquals("host1",
     * request.getJvmRoute()); Map<String, String> parameters = request.getParameters();
     * 
     * assertEquals(16, parameters.size()); assertEquals("127.0.0.1", parameters.get("Host")); assertEquals("0",
     * parameters.get("Port")); assertEquals("ajp", parameters.get("Type")); assertEquals("domain", parameters.get("Domain"));
     * assertEquals("On", parameters.get("flushpackets")); assertEquals("1", parameters.get("flushwait")); assertEquals("2",
     * parameters.get("ping")); assertEquals("3", parameters.get("smax")); assertEquals("4", parameters.get("ttl"));
     * assertEquals("5", parameters.get("Timeout")); assertEquals("S", parameters.get("Balancer")); assertEquals("No",
     * parameters.get("StickySession")); assertEquals("Yes", parameters.get("StickySessionRemove")); assertEquals("No",
     * parameters.get("StickySessionForce")); assertEquals("6", parameters.get("WaitWorker")); assertEquals("7",
     * parameters.get("Maxattempts"));
     * 
     * Set<String> routes = this.listener.drmEntry.getJvmRoutes(); assertEquals(1, routes.size()); assertEquals("route",
     * routes.iterator().next());
     * 
     * EasyMock.reset(this.partition, this.resetRequestSource, this.mcmpHandler, this.clusteredMCMPHandler, this.nodeConfig,
     * this.balancerConfig, drm, server, service, engine, container, context); }
     * 
     * private void recordAddContext(Context context, Container container) { // Expect log message
     * when(context.getPath()).thenReturn("/context"); when(context.getParent()).thenReturn(container);
     * when(container.getName()).thenReturn("parent-container");
     * 
     * when(context.isStarted()).thenReturn(false); }
     * 
     * @Test public void stopServer() throws Exception { Server server = mock(Server.class);
     * 
     * // Test not initialized try { this.listener.stopServer(server);
     * 
     * Assert.fail(); } catch (IllegalStateException e) { // Expected }
     * 
     * init();
     * 
     * this.listener.drmEntry.addJvmRoute("route");
     * 
     * // Test initialized Service service = mock(Service.class); Engine engine = mock(Engine.class); Container container =
     * mock(Container.class); Context context = mock(Context.class); DistributedReplicantManager drm =
     * mock(DistributedReplicantManager.class); Capture<MCMPRequest> capturedRequest = new Capture<MCMPRequest>();
     * 
     * when(server.findServices()).thenReturn(new Service[] { service }); when(service.getContainer()).thenReturn(engine);
     * 
     * // Expect log message when(engine.getName()).thenReturn("engine");
     * 
     * when(engine.getJvmRoute()).thenReturn("host1").times(2);
     * 
     * this.clusteredMCMPHandler.sendRequest(EasyMock.capture(capturedRequest));
     * 
     * when(engine.getJvmRoute()).thenReturn("route"); when(this.partition.getDistributedReplicantManager()).thenReturn(drm);
     * drm.add("myservice:domain", this.listener.drmEntry);
     * 
     * when(engine.findChildren()).thenReturn(new Container[] { container }); when(container.findChildren()).thenReturn(new
     * Container[] { context }); this.recordRemoveContext(context, container, engine);
     * 
     * EasyMock.replay(this.partition, this.clusteredMCMPHandler, this.nodeConfig, this.balancerConfig, drm, server, service,
     * engine, container, context);
     * 
     * this.listener.stopServer(server);
     * 
     * EasyMock.verify(this.partition, this.clusteredMCMPHandler, this.nodeConfig, this.balancerConfig, drm, server, service,
     * engine, container, context);
     * 
     * MCMPRequest request = capturedRequest.getValue();
     * 
     * assertSame(MCMPRequestType.REMOVE_APP, request.getRequestType()); assertTrue(request.isWildcard()); assertEquals("host1",
     * request.getJvmRoute()); assertTrue(request.getParameters().isEmpty());
     * 
     * assertTrue(this.listener.drmEntry.getJvmRoutes().isEmpty());
     * 
     * EasyMock.reset(this.partition, this.clusteredMCMPHandler, this.nodeConfig, this.balancerConfig, drm, server, service,
     * engine, container, context); }
     * 
     * private void recordRemoveContext(Context context, Container container, Engine engine) { // Expect log message
     * when(context.getPath()).thenReturn("/context"); when(context.getParent()).thenReturn(container);
     * when(container.getName()).thenReturn("parent-container");
     * 
     * when(context.getParent()).thenReturn(container); when(container.getParent()).thenReturn(engine);
     * when(engine.getJvmRoute()).thenReturn(null); }
     * 
     * @Test public void addContext() throws UnknownHostException { Context context = mock(Context.class);
     * 
     * EasyMock.replay(context);
     * 
     * // Test not initialized try { this.listener.addContext(context);
     * 
     * Assert.fail(); } catch (IllegalStateException e) { // Expected }
     * 
     * EasyMock.verify(context); EasyMock.reset(context);
     * 
     * init();
     * 
     * // Test context not started Host host = mock(Host.class);
     * 
     * recordAddContext(context, host);
     * 
     * EasyMock.replay(context, host);
     * 
     * this.listener.addContext(context);
     * 
     * EasyMock.verify(context, host); EasyMock.reset(context, host);
     * 
     * // Test context started Engine engine = mock(Engine.class); Capture<MCMPRequest> capturedRequest = new
     * Capture<MCMPRequest>();
     * 
     * // Expect log message when(context.getPath()).thenReturn("/context"); when(context.getParent()).thenReturn(host);
     * when(host.getName()).thenReturn("host");
     * 
     * when(context.isStarted()).thenReturn(true);
     * 
     * // Building request when(context.getParent()).thenReturn(host); when(host.getParent()).thenReturn(engine);
     * when(engine.getJvmRoute()).thenReturn("host1"); when(context.getParent()).thenReturn(host);
     * when(host.getName()).thenReturn("host"); when(host.findAliases()).thenReturn(new String[] { "alias1", "alias2" });
     * when(context.getPath()).thenReturn("/context");
     * 
     * this.clusteredMCMPHandler.sendRequest(EasyMock.capture(capturedRequest));
     * 
     * EasyMock.replay(this.clusteredMCMPHandler, context, engine, host);
     * 
     * this.listener.addContext(context);
     * 
     * EasyMock.verify(this.clusteredMCMPHandler, context, engine, host);
     * 
     * MCMPRequest request = capturedRequest.getValue();
     * 
     * assertSame(MCMPRequestType.ENABLE_APP, request.getRequestType()); assertFalse(request.isWildcard());
     * assertEquals("host1", request.getJvmRoute());
     * 
     * Map<String, String> parameters = request.getParameters();
     * 
     * assertEquals(2, parameters.size());
     * 
     * assertEquals("/context", parameters.get("Context")); assertEquals("host,alias1,alias2", parameters.get("Alias"));
     * 
     * EasyMock.reset(this.clusteredMCMPHandler, context, engine, host); }
     * 
     * @Test public void startContext() throws UnknownHostException { Context context = mock(Context.class);
     * 
     * EasyMock.replay(context);
     * 
     * // Test not initialized try { this.listener.startContext(context);
     * 
     * Assert.fail(); } catch (IllegalStateException e) { // Expected }
     * 
     * EasyMock.verify(context); EasyMock.reset(context);
     * 
     * init();
     * 
     * // Test initialized Engine engine = mock(Engine.class); Host host = mock(Host.class); Capture<MCMPRequest>
     * capturedRequest = new Capture<MCMPRequest>();
     * 
     * // Expect log message when(context.getPath()).thenReturn("/context"); when(context.getParent()).thenReturn(host);
     * when(host.getName()).thenReturn("host");
     * 
     * // Building request when(context.getParent()).thenReturn(host); when(host.getParent()).thenReturn(engine);
     * when(engine.getJvmRoute()).thenReturn("host1"); when(context.getParent()).thenReturn(host);
     * when(host.getName()).thenReturn("host"); when(host.findAliases()).thenReturn(new String[] { "alias1", "alias2" });
     * when(context.getPath()).thenReturn("/context");
     * 
     * this.clusteredMCMPHandler.sendRequest(EasyMock.capture(capturedRequest));
     * 
     * EasyMock.replay(this.clusteredMCMPHandler, context, engine, host);
     * 
     * this.listener.startContext(context);
     * 
     * EasyMock.verify(this.clusteredMCMPHandler, context, engine, host);
     * 
     * MCMPRequest request = capturedRequest.getValue();
     * 
     * assertSame(MCMPRequestType.ENABLE_APP, request.getRequestType()); assertFalse(request.isWildcard());
     * assertEquals("host1", request.getJvmRoute());
     * 
     * Map<String, String> parameters = request.getParameters();
     * 
     * assertEquals(2, parameters.size());
     * 
     * assertEquals("/context", parameters.get("Context")); assertEquals("host,alias1,alias2", parameters.get("Alias"));
     * 
     * EasyMock.reset(this.clusteredMCMPHandler, context, engine, host); }
     * 
     * @Test public void stopContext() throws UnknownHostException { Context context = mock(Context.class);
     * 
     * EasyMock.replay(context);
     * 
     * // Test not initialized try { this.listener.stopContext(context);
     * 
     * Assert.fail(); } catch (IllegalStateException e) { // Expected }
     * 
     * EasyMock.verify(context); EasyMock.reset(context);
     * 
     * init();
     * 
     * // Test initialized Engine engine = mock(Engine.class); Host host = mock(Host.class); Capture<MCMPRequest>
     * capturedRequest = new Capture<MCMPRequest>();
     * 
     * // Expect log message when(context.getPath()).thenReturn("/context"); when(context.getParent()).thenReturn(host);
     * when(host.getName()).thenReturn("host");
     * 
     * // Building request when(context.getParent()).thenReturn(host); when(host.getParent()).thenReturn(engine);
     * when(engine.getJvmRoute()).thenReturn("host1"); when(context.getParent()).thenReturn(host);
     * when(host.getName()).thenReturn("host"); when(host.findAliases()).thenReturn(new String[] { "alias1", "alias2" });
     * when(context.getPath()).thenReturn("/context");
     * 
     * this.clusteredMCMPHandler.sendRequest(EasyMock.capture(capturedRequest));
     * 
     * EasyMock.replay(this.clusteredMCMPHandler, context, engine, host);
     * 
     * this.listener.stopContext(context);
     * 
     * EasyMock.verify(this.clusteredMCMPHandler, context, engine, host);
     * 
     * MCMPRequest request = capturedRequest.getValue();
     * 
     * assertSame(MCMPRequestType.STOP_APP, request.getRequestType()); assertFalse(request.isWildcard()); assertEquals("host1",
     * request.getJvmRoute());
     * 
     * Map<String, String> parameters = request.getParameters();
     * 
     * assertEquals(2, parameters.size());
     * 
     * assertEquals("/context", parameters.get("Context")); assertEquals("host,alias1,alias2", parameters.get("Alias"));
     * 
     * EasyMock.reset(this.clusteredMCMPHandler, context, engine, host); }
     * 
     * @Test public void removeContext() throws UnknownHostException { Context context = mock(Context.class);
     * 
     * // Test not initialized try { this.listener.removeContext(context);
     * 
     * Assert.fail(); } catch (IllegalStateException e) { // Expected }
     * 
     * init();
     * 
     * // Test initialized - no jvm route Engine engine = mock(Engine.class); Host host = mock(Host.class);
     * 
     * this.recordRemoveContext(context, host, engine);
     * 
     * EasyMock.replay(context, host, engine);
     * 
     * this.listener.removeContext(context);
     * 
     * EasyMock.verify(context, host, engine); EasyMock.reset(context, host, engine);
     * 
     * 
     * // Test initialized - jvm route exists Capture<MCMPRequest> capturedRequest = new Capture<MCMPRequest>();
     * 
     * // Expect log message when(context.getPath()).thenReturn("/context"); when(context.getParent()).thenReturn(host);
     * when(host.getName()).thenReturn("host");
     * 
     * // jvm route null check when(context.getParent()).thenReturn(host); when(host.getParent()).thenReturn(engine);
     * when(engine.getJvmRoute()).thenReturn("host1");
     * 
     * // Building request when(context.getParent()).thenReturn(host); when(host.getParent()).thenReturn(engine);
     * when(engine.getJvmRoute()).thenReturn("host1"); when(context.getParent()).thenReturn(host);
     * when(host.getName()).thenReturn("host"); when(host.findAliases()).thenReturn(new String[] { "alias1", "alias2" });
     * when(context.getPath()).thenReturn("/context");
     * 
     * this.clusteredMCMPHandler.sendRequest(EasyMock.capture(capturedRequest));
     * 
     * EasyMock.replay(this.clusteredMCMPHandler, context, engine, host);
     * 
     * this.listener.removeContext(context);
     * 
     * EasyMock.verify(this.clusteredMCMPHandler, context, engine, host);
     * 
     * MCMPRequest request = capturedRequest.getValue();
     * 
     * assertSame(MCMPRequestType.REMOVE_APP, request.getRequestType()); assertFalse(request.isWildcard());
     * assertEquals("host1", request.getJvmRoute());
     * 
     * Map<String, String> parameters = request.getParameters();
     * 
     * assertEquals(2, parameters.size());
     * 
     * assertEquals("/context", parameters.get("Context")); assertEquals("host,alias1,alias2", parameters.get("Alias"));
     * 
     * EasyMock.reset(this.clusteredMCMPHandler, context, engine, host); }
     * 
     * @SuppressWarnings("unchecked")
     * 
     * @Test public void status() throws Exception { Engine engine = mock(Engine.class);
     * 
     * EasyMock.replay(engine);
     * 
     * // Test not initialized try { this.listener.status(engine);
     * 
     * Assert.fail(); } catch (IllegalStateException e) { // Expected }
     * 
     * EasyMock.verify(engine); EasyMock.reset(engine);
     * 
     * init();
     * 
     * // Test non-master status when(engine.getName()).thenReturn("engine");
     * when(this.lbfProvider.getLoadBalanceFactor()).thenReturn(10);
     * 
     * EasyMock.replay(this.lbfProvider, engine);
     * 
     * this.listener.status(engine);
     * 
     * EasyMock.verify(this.lbfProvider, engine); EasyMock.reset(this.lbfProvider, engine);
     * 
     * 
     * // Make master String key = SERVICE_HA_NAME + ":" + DOMAIN; DistributedReplicantManager drm =
     * mock(DistributedReplicantManager.class);
     * 
     * this.listener.setElectionPolicy(null);
     * 
     * when(this.partition.getDistributedReplicantManager()).thenReturn(drm); when(drm.isMasterReplica(key)).thenReturn(true);
     * 
     * this.partition.callAsynchMethodOnCluster(eq("myservice"), eq("stopOldMaster"), EasyMock.aryEq(new Object[0]),
     * EasyMock.aryEq(new Class[0]), eq(true));
     * 
     * EasyMock.replay(this.partition, drm);
     * 
     * this.listener.replicantsChanged(key, Collections.EMPTY_LIST, 1, false);
     * 
     * EasyMock.verify(this.partition, drm);
     * 
     * assertTrue(this.listener.isMasterNode());
     * 
     * EasyMock.reset(this.partition, drm);
     * 
     * // Create drm entries ClusterNode remoteNode1 = mock(ClusterNode.class); MCMPServerState remoteState1 =
     * mock(MCMPServerState.class);
     * 
     * when(remoteState1.getState()).thenReturn(MCMPServerState.State.OK); when(remoteState1.isEstablished()).thenReturn(true);
     * 
     * EasyMock.replay(remoteState1);
     * 
     * ModClusterServiceDRMEntry drmEntry1 = new ModClusterServiceDRMEntry(remoteNode1, Collections.singleton(remoteState1));
     * drmEntry1.addJvmRoute("host1");
     * 
     * EasyMock.verify(remoteState1); EasyMock.reset(remoteState1);
     * 
     * 
     * MCMPServerState remoteState2 = mock(MCMPServerState.class); ClusterNode remoteNode2 = mock(ClusterNode.class);
     * 
     * when(remoteState2.getState()).thenReturn(MCMPServerState.State.DOWN);
     * when(remoteState2.isEstablished()).thenReturn(false);
     * 
     * EasyMock.replay(remoteState2);
     * 
     * ModClusterServiceDRMEntry drmEntry2 = new ModClusterServiceDRMEntry(remoteNode2, Collections.singleton(remoteState2));
     * drmEntry2.addJvmRoute("host2");
     * 
     * EasyMock.verify(remoteState2); EasyMock.reset(remoteState2);
     * 
     * // Test master status MCMPServerState localState = mock(MCMPServerState.class); ModClusterServiceDRMEntry drmEntry = new
     * ModClusterServiceDRMEntry(this.node, null); InetAddress address1 = InetAddress.getByName("127.0.0.1"); InetAddress
     * address2 = InetAddress.getByName("127.0.1.1"); MCMPServerDiscoveryEvent event1 = new
     * MCMPServerDiscoveryEvent(remoteNode1, new AddressPort(address1, 1), true, 1); MCMPServerDiscoveryEvent event2 = new
     * MCMPServerDiscoveryEvent(remoteNode2, new AddressPort(address2, 2), false, 2);
     * 
     * Map<String, String> emptyMap = Collections.emptyMap(); MCMPRequest request1 = new MCMPRequest(MCMPRequestType.ENABLE_APP,
     * false, "route", emptyMap); MCMPRequest request2 = new MCMPRequest(MCMPRequestType.DISABLE_APP, false, "route", emptyMap);
     * Capture<List<MCMPRequest>> capturedRequests = new Capture<List<MCMPRequest>>(); Capture<ModClusterServiceDRMEntry>
     * capturedEntry = new Capture<ModClusterServiceDRMEntry>(); Capture<Object[]> capturedArgs = new Capture<Object[]>();
     * 
     * ModClusterServiceStateGroupRpcResponse response1 = new ModClusterServiceStateGroupRpcResponse(remoteNode1, 10, new
     * TreeSet<MCMPServerState>(), Collections.singletonList(event1), new ArrayList<MCMPRequest>());
     * ModClusterServiceStateGroupRpcResponse response2 = new ModClusterServiceStateGroupRpcResponse(remoteNode2, 20, new
     * TreeSet<MCMPServerState>(), Collections.singletonList(event2), new ArrayList<MCMPRequest>());
     * 
     * when(engine.getName()).thenReturn("engine"); when(this.lbfProvider.getLoadBalanceFactor()).thenReturn(10);
     * 
     * this.mcmpHandler.status();
     * 
     * when(this.mcmpHandler.getProxyStates()).thenReturn(new TreeSet<MCMPServerState>());
     * 
     * when(this.partition.getDistributedReplicantManager()).thenReturn(drm);
     * when(drm.lookupReplicants(key)).thenReturn(Collections.singletonList(drmEntry));
     * when(this.partition.getClusterNode()).thenReturn(this.node); when(this.partition.callMethodOnCluster(eq(SERVICE_HA_NAME),
     * eq("getClusterCoordinatorState"), EasyMock.aryEq(new Object[] { new TreeSet<MCMPServerState>() }), EasyMock.aryEq(new
     * Class[] { Set.class }), eq(true))).thenReturn(new ArrayList<Object>(Arrays.asList(response1, response2)));
     * 
     * // Process discovery events this.mcmpHandler.addProxy(address1, 1); this.mcmpHandler.removeProxy(address2, 2);
     * 
     * // Start over - this time with no discovery events response1 = new ModClusterServiceStateGroupRpcResponse(remoteNode1,
     * 10, Collections.singleton(remoteState1), new ArrayList<MCMPServerDiscoveryEvent>(), Collections.singletonList(request1));
     * response2 = new ModClusterServiceStateGroupRpcResponse(remoteNode2, 20, Collections.singleton(remoteState2), new
     * ArrayList<MCMPServerDiscoveryEvent>(), Collections.singletonList(request2));
     * 
     * this.mcmpHandler.status();
     * 
     * Set<MCMPServerState> states = new LinkedHashSet<MCMPServerState>(Arrays.asList(remoteState1, remoteState2));
     * 
     * when(this.mcmpHandler.getProxyStates()).thenReturn(states);
     * 
     * when(drm.lookupReplicants(key)).thenReturn(Arrays.asList(drmEntry1, drmEntry2));
     * when(this.partition.getClusterNode()).thenReturn(this.node); when(this.partition.callMethodOnCluster(eq(SERVICE_HA_NAME),
     * eq("getClusterCoordinatorState"), EasyMock.aryEq(new Object[] { states }), EasyMock.aryEq(new Class[] { Set.class }),
     * eq(true))).thenReturn(new ArrayList<Object>(Arrays.asList(response1, response2)));
     * 
     * when(remoteState1.getState()).thenReturn(MCMPServerState.State.OK); when(remoteState1.isEstablished()).thenReturn(true);
     * when(remoteState2.getState()).thenReturn(MCMPServerState.State.DOWN);
     * when(remoteState2.isEstablished()).thenReturn(false);
     * 
     * this.mcmpHandler.sendRequests(Arrays.asList(request1, request2));
     * this.mcmpHandler.sendRequests(EasyMock.capture(capturedRequests));
     * 
     * when(this.partition.getDistributedReplicantManager()).thenReturn(drm);
     * when(drm.lookupLocalReplicant(key)).thenReturn(drmEntry);
     * 
     * when(this.partition.getClusterNode()).thenReturn(this.node);
     * 
     * when(remoteState1.getState()).thenReturn(MCMPServerState.State.OK); when(remoteState1.isEstablished()).thenReturn(true);
     * when(remoteState2.getState()).thenReturn(MCMPServerState.State.DOWN);
     * when(remoteState2.isEstablished()).thenReturn(false);
     * 
     * when(this.partition.getDistributedReplicantManager()).thenReturn(drm);
     * 
     * drm.add(eq(key), EasyMock.capture(capturedEntry));
     * 
     * when(this.partition.callMethodOnCluster(eq("myservice"), eq("clusterStatusComplete"), EasyMock.capture(capturedArgs),
     * EasyMock.aryEq(new Class[] { Map.class }), eq(true))).thenReturn(null);
     * 
     * EasyMock.replay(this.lbfProvider, this.mcmpHandler, this.clusteredMCMPHandler, this.partition, drm, localState,
     * this.node, remoteState1, remoteState2, remoteNode1, remoteNode2, engine);
     * 
     * this.listener.status(engine);
     * 
     * EasyMock.verify(this.lbfProvider, this.mcmpHandler, this.clusteredMCMPHandler, this.partition, drm, localState,
     * this.node, remoteState1, remoteState2, remoteNode1, remoteNode2, engine);
     * 
     * List<MCMPRequest> requests = capturedRequests.getValue();
     * 
     * assertEquals(2, requests.size());
     * 
     * assertSame(MCMPRequestType.STATUS, requests.get(0).getRequestType()); assertFalse(requests.get(0).isWildcard());
     * assertEquals("host2", requests.get(0).getJvmRoute()); assertEquals(1, requests.get(0).getParameters().size());
     * assertEquals("20", requests.get(0).getParameters().get("Load"));
     * 
     * assertSame(MCMPRequestType.STATUS, requests.get(1).getRequestType()); assertFalse(requests.get(1).isWildcard());
     * assertEquals("host1", requests.get(1).getJvmRoute()); assertEquals(1, requests.get(1).getParameters().size());
     * assertEquals("10", requests.get(1).getParameters().get("Load"));
     * 
     * ModClusterServiceDRMEntry entry = capturedEntry.getValue();
     * 
     * assertSame(this.node, entry.getPeer()); assertEquals(states, entry.getMCMPServerStates());
     * assertTrue(entry.getJvmRoutes().isEmpty());
     * 
     * Object[] args = capturedArgs.getValue(); assertEquals(1, args.length); assertTrue(args[0] instanceof Map);
     * 
     * Map<ClusterNode, PeerMCMPDiscoveryStatus> map = (Map<ClusterNode, PeerMCMPDiscoveryStatus>) args[0];
     * 
     * assertEquals(2, map.size()); assertTrue(map.containsKey(remoteNode1)); assertTrue(map.containsKey(remoteNode2));
     * 
     * PeerMCMPDiscoveryStatus status1 = map.get(remoteNode1);
     * 
     * assertSame(remoteNode1, status1.getPeer()); assertEquals(Collections.singleton(remoteState1),
     * status1.getMCMPServerStates()); assertTrue(status1.getJvmRoutes().isEmpty());
     * assertNull(status1.getLatestDiscoveryEvent());
     * 
     * EasyMock.reset(this.lbfProvider, this.mcmpHandler, this.clusteredMCMPHandler, this.partition, drm, localState, this.node,
     * remoteState1, remoteState2, remoteNode1, remoteNode2, engine);
     * 
     * 
     * // Test master status, but off-frequency this.listener.setProcessStatusFrequency(2);
     * 
     * when(engine.getName()).thenReturn("engine"); when(this.lbfProvider.getLoadBalanceFactor()).thenReturn(10);
     * 
     * EasyMock.replay(this.lbfProvider, engine);
     * 
     * this.listener.status(engine);
     * 
     * EasyMock.verify(this.lbfProvider, engine); EasyMock.reset(this.lbfProvider, engine); }
     */
}
