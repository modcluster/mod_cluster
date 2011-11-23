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
import static org.mockito.AdditionalMatchers.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.ha.framework.interfaces.ClusterNode;
import org.jboss.ha.framework.interfaces.HAPartition;
import org.jboss.ha.framework.interfaces.HAServiceKeyProvider;
import org.jboss.ha.framework.interfaces.HASingletonMBean;
import org.jboss.modcluster.ha.rpc.MCMPServerDiscoveryEvent;
import org.jboss.modcluster.ha.rpc.RpcResponse;
import org.jboss.modcluster.ha.rpc.RpcResponseFilter;
import org.jboss.modcluster.mcmp.MCMPConnectionListener;
import org.jboss.modcluster.mcmp.MCMPHandler;
import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPServer;
import org.jboss.modcluster.mcmp.MCMPServerState;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Paul Ferraro
 * 
 */
@SuppressWarnings("boxing")
public class ClusteredMCMPHandlerTestCase {
    private MCMPHandler localHandler = mock(MCMPHandler.class);
    private MCMPConnectionListener connectionListener = mock(MCMPConnectionListener.class);
    private HAServiceKeyProvider keyProvider = mock(HAServiceKeyProvider.class);
    private HAPartition partition = mock(HAPartition.class);
    private HASingletonMBean singleton = mock(HASingletonMBean.class);

    private ClusteredMCMPHandler handler = new ClusteredMCMPHandlerImpl(this.localHandler, this.singleton, this.keyProvider);

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Test
    public void init() throws Exception {
        ClusterNode node = mock(ClusterNode.class);
        InetAddress address = InetAddress.getLocalHost();
        int port = 0;
        String key = "key";

        List<InetSocketAddress> list = Collections.singletonList(new InetSocketAddress(address, port));

        // Test master case
        when(this.singleton.isMasterNode()).thenReturn(true);

        this.handler.init(list, this.connectionListener);

        verify(this.localHandler).init(list, this.connectionListener);
        Mockito.reset(this.localHandler);

        // Test non-master case
        ArgumentCaptor<List<InetSocketAddress>> capturedList = new ArgumentCaptor<List<InetSocketAddress>>();
        ArgumentCaptor<Object[]> capturedEvents = ArgumentCaptor.forClass(Object[].class);
        RpcResponse<Void> response1 = mock(RpcResponse.class);
        RpcResponse<Void> response2 = mock(RpcResponse.class);
        ArrayList<RpcResponse<Void>> responses = new ArrayList<RpcResponse<Void>>(Arrays.asList(response1, response2));

        when(this.singleton.isMasterNode()).thenReturn(false);

        when(this.keyProvider.getHAPartition()).thenReturn(this.partition);
        when(this.partition.getClusterNode()).thenReturn(node);

        when(this.keyProvider.getHAPartition()).thenReturn(this.partition);
        when(this.keyProvider.getHAServiceKey()).thenReturn(key);

        when(
                (List<RpcResponse<Void>>) this.partition.callMethodOnCluster(eq(key), eq("mcmpServerDiscoveryEvent"),
                        capturedEvents.capture(), aryEq(new Class[] { MCMPServerDiscoveryEvent.class }), eq(false),
                        isA(RpcResponseFilter.class))).thenReturn(responses);

        when(response1.getResult()).thenReturn(null);

        this.handler.init(list, this.connectionListener);

        verify(this.localHandler).init(capturedList.capture(), same(this.connectionListener));
        Mockito.reset(this.localHandler);

        assertNotNull(capturedList.getValue());
        assertTrue(capturedList.getValue().isEmpty());

        List<MCMPServerDiscoveryEvent> events = this.handler.getPendingDiscoveryEvents();

        assertNotNull(events);
        assertEquals(1, events.size());

        MCMPServerDiscoveryEvent event = events.get(0);

        assertSame(node, event.getSender());
        assertSame(address, event.getMCMPServer().getAddress());
        assertEquals(port, event.getMCMPServer().getPort());
        assertTrue(event.isAddition());
        assertEquals(1, event.getEventIndex());

        assertNotNull(capturedEvents.getValue());
        assertEquals(1, capturedEvents.getValue().length);
        assertSame(event, capturedEvents.getValue()[0]);
    }

    @Test
    public void updateServersFromMasterNode() throws Exception {
        MCMPServer server = mock(MCMPServer.class);
        MCMPServerState state = mock(MCMPServerState.class);

        InetSocketAddress serverAddress = InetSocketAddress.createUnresolved("localhost", 1);
        InetSocketAddress stateAddress = InetSocketAddress.createUnresolved("localhost", 1);

        when(server.getSocketAddress()).thenReturn(serverAddress);
        when(server.isEstablished()).thenReturn(true);

        when(this.localHandler.getProxyStates()).thenReturn(Collections.singleton(state));
        doNothing().when(this.localHandler).addProxy(same(serverAddress), eq(true));

        when(state.getSocketAddress()).thenReturn(stateAddress);

        when(this.localHandler.getProxyStates()).thenReturn(Collections.<MCMPServerState> emptySet());
        doNothing().when(this.localHandler).removeProxy(same(stateAddress));

        doNothing().when(this.localHandler).status();

        Set<MCMPServerState> result = this.handler.updateServersFromMasterNode(Collections.singleton(server));

        Mockito.reset(this.localHandler);

        assertTrue(result.isEmpty());
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Test
    public void addProxy() throws Exception {
        InetSocketAddress socketAddress = InetSocketAddress.createUnresolved("localhost", 8080);

        // Test MCMPHandler.addProxy(InetSocketAddress)
        when(this.singleton.isMasterNode()).thenReturn(true);

        this.handler.addProxy(socketAddress);

        verify(this.localHandler).addProxy(same(socketAddress));
        Mockito.reset(this.localHandler);

        // Test MCMPHandler.addProxy(InetSocketAddress, boolean)
        this.handler.addProxy(socketAddress, true);

        verify(this.localHandler).addProxy(same(socketAddress), eq(true));
        Mockito.reset(this.localHandler);

        // Test non-master use case
        String key = "key";
        ArgumentCaptor<Object[]> capturedEvents = ArgumentCaptor.forClass(Object[].class);
        ClusterNode node = mock(ClusterNode.class);
        RpcResponse<Void> response1 = mock(RpcResponse.class);
        RpcResponse<Void> response2 = mock(RpcResponse.class);
        List<RpcResponse<Void>> responses = new ArrayList<RpcResponse<Void>>(Arrays.asList(response1, response2));

        when(this.singleton.isMasterNode()).thenReturn(false);

        when(this.keyProvider.getHAPartition()).thenReturn(this.partition);
        when(this.partition.getClusterNode()).thenReturn(node);

        when(this.keyProvider.getHAPartition()).thenReturn(this.partition);
        when(this.keyProvider.getHAServiceKey()).thenReturn(key);

        when(
                (List<RpcResponse<Void>>) this.partition.callMethodOnCluster(eq(key), eq("mcmpServerDiscoveryEvent"),
                        capturedEvents.capture(), aryEq(new Class[] { MCMPServerDiscoveryEvent.class }), eq(false),
                        isA(RpcResponseFilter.class))).thenReturn(responses);

        when(response1.getResult()).thenReturn(null);

        this.handler.addProxy(socketAddress);

        verifyZeroInteractions(this.localHandler);

        List<MCMPServerDiscoveryEvent> events = this.handler.getPendingDiscoveryEvents();

        assertNotNull(events);
        assertEquals(1, events.size());

        MCMPServerDiscoveryEvent event = events.get(0);

        assertSame(node, event.getSender());
        assertSame(socketAddress, event.getMCMPServer());
        assertTrue(event.isAddition());
        assertEquals(1, event.getEventIndex());

        assertNotNull(capturedEvents.getValue());
        assertEquals(1, capturedEvents.getValue().length);
        assertSame(event, capturedEvents.getValue()[0]);

        // Test MCMPHandler.addProxy(InetAddress, int, boolean)
        this.handler.addProxy(socketAddress, true);

        verify(this.localHandler).addProxy(same(socketAddress), eq(true));
        Mockito.reset(this.localHandler);
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    @Test
    public void removeProxy() throws Exception {
        InetSocketAddress socketAddress = InetSocketAddress.createUnresolved("localhost", 8080);

        // Test master use case
        when(this.singleton.isMasterNode()).thenReturn(true);

        this.handler.removeProxy(socketAddress);

        verify(this.localHandler).removeProxy(same(socketAddress));
        Mockito.reset(this.localHandler);

        // Test non-master use case
        String key = "key";
        ArgumentCaptor<Object[]> capturedEvents = ArgumentCaptor.forClass(Object[].class);
        ClusterNode node = mock(ClusterNode.class);
        RpcResponse<Void> response1 = mock(RpcResponse.class);
        RpcResponse<Void> response2 = mock(RpcResponse.class);
        ArrayList<RpcResponse<Void>> responses = new ArrayList<RpcResponse<Void>>(Arrays.asList(response1, response2));

        when(this.singleton.isMasterNode()).thenReturn(false);

        when(this.keyProvider.getHAPartition()).thenReturn(this.partition);
        when(this.partition.getClusterNode()).thenReturn(node);

        when(this.keyProvider.getHAPartition()).thenReturn(this.partition);
        when(this.keyProvider.getHAServiceKey()).thenReturn(key);

        when(
                (List<RpcResponse<Void>>) this.partition.callMethodOnCluster(eq(key), eq("mcmpServerDiscoveryEvent"),
                        capturedEvents.capture(), aryEq(new Class[] { MCMPServerDiscoveryEvent.class }), eq(false),
                        isA(RpcResponseFilter.class))).thenReturn(responses);

        when(response1.getResult()).thenReturn(null);

        this.handler.removeProxy(socketAddress);

        verifyZeroInteractions(this.localHandler);

        List<MCMPServerDiscoveryEvent> events = this.handler.getPendingDiscoveryEvents();

        assertNotNull(events);
        assertEquals(1, events.size());

        MCMPServerDiscoveryEvent event = events.get(0);

        assertSame(node, event.getSender());
        assertSame(socketAddress, event.getMCMPServer());
        assertFalse(event.isAddition());
        assertEquals(1, event.getEventIndex());

        assertNotNull(capturedEvents.getValue());
        assertEquals(1, capturedEvents.getValue().length);
        assertSame(event, capturedEvents.getValue()[0]);
    }

    @Test
    public void getProxyStates() {
        when(this.localHandler.getProxyStates()).thenReturn(Collections.<MCMPServerState> emptySet());

        Set<MCMPServerState> result = this.handler.getProxyStates();

        assertTrue(result.isEmpty());
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Test
    public void isProxyHealthOK() throws Exception {
        // Test master use case
        when(this.singleton.isMasterNode()).thenReturn(true);

        when(this.localHandler.isProxyHealthOK()).thenReturn(true);

        boolean result = this.handler.isProxyHealthOK();

        assertTrue(result);
        Mockito.reset(this.localHandler);

        // Test non-master use case
        String key = "key";
        RpcResponse<Boolean> response1 = mock(RpcResponse.class);
        RpcResponse<Boolean> response2 = mock(RpcResponse.class);
        ArrayList<RpcResponse<Boolean>> responses = new ArrayList<RpcResponse<Boolean>>(Arrays.asList(response1, response2));

        when(this.singleton.isMasterNode()).thenReturn(false);

        when(this.keyProvider.getHAPartition()).thenReturn(this.partition);
        when(this.keyProvider.getHAServiceKey()).thenReturn(key);

        when(
                (List<RpcResponse<Boolean>>) this.partition.callMethodOnCluster(same(key), eq("isProxyHealthOk"),
                        aryEq(new Object[0]), aryEq(new Class[0]), eq(false), isA(RpcResponseFilter.class))).thenReturn(
                responses);

        when(response1.getResult()).thenReturn(true);

        result = this.handler.isProxyHealthOK();

        verifyZeroInteractions(this.localHandler);

        assertTrue(result);
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Test
    public void markProxiesInError() throws Exception {
        // Test master use case
        when(this.singleton.isMasterNode()).thenReturn(true);

        this.handler.markProxiesInError();

        verify(this.localHandler).markProxiesInError();
        Mockito.reset(this.localHandler);

        // Test non-master use case
        String key = "key";
        RpcResponse<Void> response1 = mock(RpcResponse.class);
        RpcResponse<Void> response2 = mock(RpcResponse.class);
        ArrayList<RpcResponse<Void>> responses = new ArrayList<RpcResponse<Void>>(Arrays.asList(response1, response2));

        when(this.singleton.isMasterNode()).thenReturn(false);

        when(this.keyProvider.getHAPartition()).thenReturn(this.partition);
        when(this.keyProvider.getHAServiceKey()).thenReturn(key);

        when(
                (List<RpcResponse<Void>>) this.partition.callMethodOnCluster(same(key), eq("markProxiesInError"),
                        aryEq(new Object[0]), aryEq(new Class[0]), eq(false), isA(RpcResponseFilter.class))).thenReturn(
                responses);

        when(response1.getResult()).thenReturn(null);

        this.handler.markProxiesInError();

        verifyZeroInteractions(this.localHandler);
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    @Test
    public void reset() throws Exception {
        // Test master use case
        when(this.singleton.isMasterNode()).thenReturn(true);

        this.handler.reset();

        verify(this.localHandler).reset();
        Mockito.reset(this.localHandler);

        // Test non-master use case
        String key = "key";
        RpcResponse<Void> response1 = mock(RpcResponse.class);
        RpcResponse<Void> response2 = mock(RpcResponse.class);
        ArrayList<RpcResponse<Void>> responses = new ArrayList<RpcResponse<Void>>(Arrays.asList(response1, response2));

        when(this.singleton.isMasterNode()).thenReturn(false);

        when(this.keyProvider.getHAPartition()).thenReturn(this.partition);
        when(this.keyProvider.getHAServiceKey()).thenReturn(key);

        when(
                (List<RpcResponse<Void>>) this.partition.callMethodOnCluster(same(key), eq("reset"), aryEq(new Object[0]),
                        aryEq(new Class[0]), eq(false), isA(RpcResponseFilter.class))).thenReturn(responses);

        when(response1.getResult()).thenReturn(null);

        this.handler.reset();

        verifyZeroInteractions(this.localHandler);
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Test
    public void sendRequest() throws Exception {
        MCMPRequest request = mock(MCMPRequest.class);

        // Test master use case
        when(this.singleton.isMasterNode()).thenReturn(true);

        when(this.localHandler.sendRequest(same(request))).thenReturn(Collections.<MCMPServerState, String> emptyMap());

        Map<MCMPServerState, String> result = this.handler.sendRequest(request);

        assertTrue(result.isEmpty());

        // Test non-master use case
        String key = "key";

        ArgumentCaptor<Object[]> captured = ArgumentCaptor.forClass(Object[].class);
        RpcResponse<Map<MCMPServerState, String>> response1 = mock(RpcResponse.class);
        RpcResponse<Map<MCMPServerState, String>> response2 = mock(RpcResponse.class);
        List<RpcResponse<Map<MCMPServerState, String>>> responses = new ArrayList<RpcResponse<Map<MCMPServerState, String>>>(
                Arrays.asList(response1, response2));

        when(this.singleton.isMasterNode()).thenReturn(false);

        when(this.keyProvider.getHAPartition()).thenReturn(this.partition);
        when(this.keyProvider.getHAServiceKey()).thenReturn(key);

        when(
                (List<RpcResponse<Map<MCMPServerState, String>>>) this.partition.callMethodOnCluster(same(key),
                        eq("sendRequest"), captured.capture(), aryEq(new Class[] { MCMPRequest.class }), eq(false),
                        isA(RpcResponseFilter.class))).thenReturn(responses);

        when(response1.getResult()).thenReturn(Collections.<MCMPServerState, String> emptyMap());

        result = this.handler.sendRequest(request);

        assertNotNull(captured.getValue());
        assertEquals(1, captured.getValue().length);
        assertSame(request, captured.getValue()[0]);
        assertTrue(result.isEmpty());
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    @Test
    public void sendRequests() throws Exception {
        List<MCMPRequest> requests = Collections.emptyList();

        // Test master use case
        when(this.singleton.isMasterNode()).thenReturn(true);

        when(this.localHandler.sendRequests(same(requests))).thenReturn(Collections.<MCMPServerState, List<String>> emptyMap());

        Map<MCMPServerState, List<String>> result = this.handler.sendRequests(requests);

        assertTrue(result.isEmpty());

        // Test non-master use case
        String key = "key";

        ArgumentCaptor<Object[]> captured = ArgumentCaptor.forClass(Object[].class);
        RpcResponse<Map<MCMPServerState, List<String>>> response1 = mock(RpcResponse.class);
        RpcResponse<Map<MCMPServerState, List<String>>> response2 = mock(RpcResponse.class);
        List<RpcResponse<Map<MCMPServerState, List<String>>>> responses = new ArrayList<RpcResponse<Map<MCMPServerState, List<String>>>>(
                Arrays.asList(response1, response2));

        when(this.singleton.isMasterNode()).thenReturn(false);

        when(this.keyProvider.getHAPartition()).thenReturn(this.partition);
        when(this.keyProvider.getHAServiceKey()).thenReturn(key);

        when(
                (List<RpcResponse<Map<MCMPServerState, List<String>>>>) this.partition.callMethodOnCluster(same(key),
                        eq("sendRequests"), captured.capture(), aryEq(new Class[] { List.class }), eq(false),
                        isA(RpcResponseFilter.class))).thenReturn(responses);

        when(response1.getResult()).thenReturn(Collections.<MCMPServerState, List<String>> emptyMap());

        result = this.handler.sendRequests(requests);

        assertNotNull(captured.getValue());
        assertEquals(1, captured.getValue().length);
        assertSame(requests, captured.getValue()[0]);
        assertTrue(result.isEmpty());
    }

    @Test
    public void shutdown() {
        this.handler.shutdown();

        verify(this.localHandler).shutdown();
        Mockito.reset(this.localHandler);
    }

    @Test
    public void getNeedsResetTransmission() {
        assertFalse(this.handler.isResetNecessary());

        // Set State to ERROR
        when(this.singleton.isMasterNode()).thenReturn(true);

        this.handler.markProxiesInError();

        verify(this.localHandler).markProxiesInError();
        Mockito.reset(this.localHandler);

        assertTrue(this.handler.isResetNecessary());

        this.handler.resetInitiated();

        assertFalse(this.handler.isResetNecessary());

        // Set State to ERROR again
        when(this.singleton.isMasterNode()).thenReturn(true);

        this.handler.markProxiesInError();

        verify(this.localHandler).markProxiesInError();
        Mockito.reset(this.localHandler);

        assertTrue(this.handler.isResetNecessary());

        this.handler.resetCompleted();

        assertTrue(this.handler.isResetNecessary());

        this.handler.resetInitiated();

        assertFalse(this.handler.isResetNecessary());

        this.handler.resetCompleted();

        assertFalse(this.handler.isResetNecessary());
    }
}
