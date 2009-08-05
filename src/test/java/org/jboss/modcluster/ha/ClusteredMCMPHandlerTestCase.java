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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.jboss.ha.framework.interfaces.ClusterNode;
import org.jboss.ha.framework.interfaces.HAPartition;
import org.jboss.ha.framework.interfaces.HAServiceKeyProvider;
import org.jboss.ha.framework.interfaces.HASingletonMBean;
import org.jboss.modcluster.ha.rpc.BooleanGroupRpcResponse;
import org.jboss.modcluster.ha.rpc.GroupRpcResponse;
import org.jboss.modcluster.ha.rpc.GroupRpcResponseFilter;
import org.jboss.modcluster.ha.rpc.MCMPServerDiscoveryEvent;
import org.jboss.modcluster.ha.rpc.StringGroupRpcResponse;
import org.jboss.modcluster.mcmp.MCMPHandler;
import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPServer;
import org.jboss.modcluster.mcmp.MCMPServerState;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
@SuppressWarnings("boxing")
public class ClusteredMCMPHandlerTestCase
{
   private MCMPHandler localHandler = EasyMock.createStrictMock(MCMPHandler.class);
   private HAServiceKeyProvider keyProvider = EasyMock.createStrictMock(HAServiceKeyProvider.class);
   private HAPartition partition = EasyMock.createStrictMock(HAPartition.class);
   private HASingletonMBean singleton = EasyMock.createStrictMock(HASingletonMBean.class);

   private ClusteredMCMPHandler handler = new ClusteredMCMPHandlerImpl(this.localHandler, this.singleton, this.keyProvider);

   @Test
   public void init() throws Exception
   {
      ClusterNode node = EasyMock.createMock(ClusterNode.class);
      InetAddress address = InetAddress.getLocalHost();
      int port = 0;
      String key = "key";
      
      List<InetSocketAddress> list = Collections.singletonList(new InetSocketAddress(address, port));
      
      // Test master case
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(true);
      
      this.localHandler.init(list);
      
      EasyMock.replay(this.localHandler, this.singleton, this.keyProvider, this.partition);
      
      this.handler.init(list);
      
      EasyMock.verify(this.localHandler, this.singleton, this.keyProvider, this.partition);
      EasyMock.reset(this.localHandler, this.singleton, this.keyProvider, this.partition);
      
      // Test non-master case
      Capture<List<InetSocketAddress>> capturedList = new Capture<List<InetSocketAddress>>();
      Capture<Object[]> capturedEvents = new Capture<Object[]>();
      
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(false);
      
      this.localHandler.init(EasyMock.capture(capturedList));
      
      EasyMock.expect(this.keyProvider.getHAPartition()).andReturn(this.partition);
      EasyMock.expect(this.partition.getClusterNode()).andReturn(node);
      
      EasyMock.expect(this.keyProvider.getHAPartition()).andReturn(this.partition);
      EasyMock.expect(this.keyProvider.getHAServiceKey()).andReturn(key);
      
      EasyMock.expect(this.partition.callMethodOnCluster(EasyMock.eq(key), EasyMock.eq("mcmpServerDiscoveryEvent"), EasyMock.capture(capturedEvents), EasyMock.aryEq(new Class[] { MCMPServerDiscoveryEvent.class }), EasyMock.eq(false), EasyMock.isA(GroupRpcResponseFilter.class))).andReturn(new ArrayList<GroupRpcResponse>(Collections.singleton(new GroupRpcResponse(node))));
      
      EasyMock.replay(this.localHandler, this.singleton, this.keyProvider, this.partition);
      
      this.handler.init(list);
      
      EasyMock.verify(this.localHandler, this.singleton, this.keyProvider, this.partition);
      
      Assert.assertNotNull(capturedList.getValue());
      Assert.assertTrue(capturedList.getValue().isEmpty());
      
      List<MCMPServerDiscoveryEvent> events = this.handler.getPendingDiscoveryEvents();
      
      Assert.assertNotNull(events);
      Assert.assertEquals(1, events.size());
      
      MCMPServerDiscoveryEvent event = events.get(0);
      
      Assert.assertSame(node, event.getSender());
      Assert.assertSame(address, event.getMCMPServer().getAddress());
      Assert.assertEquals(port, event.getMCMPServer().getPort());
      Assert.assertTrue(event.isAddition());
      Assert.assertEquals(1, event.getEventIndex());
      
      Assert.assertNotNull(capturedEvents.getValue());
      Assert.assertEquals(1, capturedEvents.getValue().length);
      Assert.assertSame(event, capturedEvents.getValue()[0]);
      
      EasyMock.reset(this.localHandler, this.singleton, this.keyProvider, this.partition);
   }
   
   @Test
   public void updateServersFromMasterNode() throws Exception
   {
      MCMPServer server = EasyMock.createMock(MCMPServer.class);
      MCMPServerState state = EasyMock.createMock(MCMPServerState.class);
      Set<MCMPServerState> states = Collections.emptySet();
      
      InetAddress serverAddress = InetAddress.getLocalHost();
      int serverPort = 0;
      InetAddress stateAddress = InetAddress.getLocalHost();
      int statePort = 1;
      
      EasyMock.expect(server.getAddress()).andReturn(serverAddress);
      EasyMock.expect(server.getPort()).andReturn(serverPort);
      EasyMock.expect(server.isEstablished()).andReturn(true);
      
      this.localHandler.addProxy(serverAddress, serverPort, true);
      
      EasyMock.expect(this.localHandler.getProxyStates()).andReturn(Collections.singleton(state));
      
      EasyMock.expect(state.getAddress()).andReturn(stateAddress);
      EasyMock.expect(state.getPort()).andReturn(statePort);
      
      this.localHandler.removeProxy(stateAddress, statePort);
      
      this.localHandler.status();
      
      EasyMock.expect(this.localHandler.getProxyStates()).andReturn(states);
      
      EasyMock.replay(this.localHandler, this.singleton, server, state);
      
      Set<MCMPServerState> result = this.handler.updateServersFromMasterNode(Collections.singleton(server));
      
      EasyMock.verify(this.localHandler, this.singleton, server, state);
      
      Assert.assertSame(states, result);
      
      EasyMock.reset(this.localHandler, this.singleton, server, state);
   }
   
   @Test
   public void addProxy() throws Exception
   {
      InetAddress address = InetAddress.getLocalHost();
      int port = 0;
      
      // Test master use case
      // Test MCMPHandler.addProxy(String)
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(true);
      
      this.localHandler.addProxy(address, port);
      
      EasyMock.replay(this.localHandler, this.singleton);
      
      this.handler.addProxy(address.getHostName() + ":" + port);
      
      EasyMock.verify(this.localHandler, this.singleton);
      EasyMock.reset(this.localHandler, this.singleton);
      
      
      // Test MCMPHandler.addProxy(String, int)
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(true);
      
      this.localHandler.addProxy(address, port);
      
      EasyMock.replay(this.localHandler, this.singleton);
      
      this.handler.addProxy(address.getHostName(), port);
      
      EasyMock.verify(this.localHandler, this.singleton);
      EasyMock.reset(this.localHandler, this.singleton);
      
      
      // Test MCMPHandler.addProxy(InetAddress, int)
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(true);
      
      this.localHandler.addProxy(address, port);
      
      EasyMock.replay(this.localHandler, this.singleton);
      
      this.handler.addProxy(address, port);
      
      EasyMock.verify(this.localHandler, this.singleton);
      EasyMock.reset(this.localHandler, this.singleton);
      
      
      // Test MCMPHandler.addProxy(InetAddress, int, boolean)
      this.localHandler.addProxy(address, port, true);
      
      EasyMock.replay(this.localHandler);
      
      this.handler.addProxy(address, port, true);
      
      EasyMock.verify(this.localHandler);
      EasyMock.reset(this.localHandler);
      
      
      // Test non-master use case
      String key = "key";
      Capture<Object[]> capturedEvents = new Capture<Object[]>();
      ClusterNode node = EasyMock.createMock(ClusterNode.class);
      
      // Test MCMPHandler.addProxy(String)
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(false);
      
      EasyMock.expect(this.keyProvider.getHAPartition()).andReturn(this.partition);
      EasyMock.expect(this.partition.getClusterNode()).andReturn(node);
      
      EasyMock.expect(this.keyProvider.getHAPartition()).andReturn(this.partition);
      EasyMock.expect(this.keyProvider.getHAServiceKey()).andReturn(key);
      
      EasyMock.expect(this.partition.callMethodOnCluster(EasyMock.eq(key), EasyMock.eq("mcmpServerDiscoveryEvent"), EasyMock.capture(capturedEvents), EasyMock.aryEq(new Class[] { MCMPServerDiscoveryEvent.class }), EasyMock.eq(false), EasyMock.isA(GroupRpcResponseFilter.class))).andReturn(new ArrayList<GroupRpcResponse>(Collections.singleton(new GroupRpcResponse(node))));
      
      EasyMock.replay(this.keyProvider, this.partition, this.singleton);

      this.handler.addProxy(address.getHostName() + ":" + port);
      
      EasyMock.verify(this.keyProvider, this.partition, this.singleton);
      
      List<MCMPServerDiscoveryEvent> events = this.handler.getPendingDiscoveryEvents();
      
      Assert.assertNotNull(events);
      Assert.assertEquals(1, events.size());
      
      MCMPServerDiscoveryEvent event = events.get(0);
      
      Assert.assertSame(node, event.getSender());
      Assert.assertSame(address, event.getMCMPServer().getAddress());
      Assert.assertEquals(port, event.getMCMPServer().getPort());
      Assert.assertTrue(event.isAddition());
      Assert.assertEquals(1, event.getEventIndex());
      
      Assert.assertNotNull(capturedEvents.getValue());
      Assert.assertEquals(1, capturedEvents.getValue().length);
      Assert.assertSame(event, capturedEvents.getValue()[0]);
      
      EasyMock.reset(this.keyProvider, this.partition, this.singleton);
      
      
      // Test MCMPHandler.addProxy(String, int)
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(false);
      
      EasyMock.expect(this.keyProvider.getHAPartition()).andReturn(this.partition);
      EasyMock.expect(this.partition.getClusterNode()).andReturn(node);
      
      EasyMock.expect(this.keyProvider.getHAPartition()).andReturn(this.partition);
      EasyMock.expect(this.keyProvider.getHAServiceKey()).andReturn(key);
      
      EasyMock.expect(this.partition.callMethodOnCluster(EasyMock.eq(key), EasyMock.eq("mcmpServerDiscoveryEvent"), EasyMock.capture(capturedEvents), EasyMock.aryEq(new Class[] { MCMPServerDiscoveryEvent.class }), EasyMock.eq(false), EasyMock.isA(GroupRpcResponseFilter.class))).andReturn(new ArrayList<GroupRpcResponse>(Collections.singleton(new GroupRpcResponse(node))));
      
      EasyMock.replay(this.keyProvider, this.partition, this.singleton);

      this.handler.addProxy(address.getHostName(), port);
      
      EasyMock.verify(this.keyProvider, this.partition, this.singleton);
      
      events = this.handler.getPendingDiscoveryEvents();
      
      Assert.assertNotNull(events);
      Assert.assertEquals(2, events.size());
      
      event = events.get(1);
      
      Assert.assertSame(node, event.getSender());
      Assert.assertSame(address, event.getMCMPServer().getAddress());
      Assert.assertEquals(port, event.getMCMPServer().getPort());
      Assert.assertTrue(event.isAddition());
      Assert.assertEquals(2, event.getEventIndex());
      
      Assert.assertNotNull(capturedEvents.getValue());
      Assert.assertEquals(1, capturedEvents.getValue().length);
      Assert.assertSame(event, capturedEvents.getValue()[0]);
      
      EasyMock.reset(this.keyProvider, this.partition, this.singleton);
      
      
      // Test MCMPHandler.addProxy(InetAddress, int)
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(false);
      
      EasyMock.expect(this.keyProvider.getHAPartition()).andReturn(this.partition);
      EasyMock.expect(this.partition.getClusterNode()).andReturn(node);
      
      EasyMock.expect(this.keyProvider.getHAPartition()).andReturn(this.partition);
      EasyMock.expect(this.keyProvider.getHAServiceKey()).andReturn(key);
      
      EasyMock.expect(this.partition.callMethodOnCluster(EasyMock.eq(key), EasyMock.eq("mcmpServerDiscoveryEvent"), EasyMock.capture(capturedEvents), EasyMock.aryEq(new Class[] { MCMPServerDiscoveryEvent.class }), EasyMock.eq(false), EasyMock.isA(GroupRpcResponseFilter.class))).andReturn(new ArrayList<GroupRpcResponse>(Collections.singleton(new GroupRpcResponse(node))));
      
      EasyMock.replay(this.keyProvider, this.partition, this.singleton);

      this.handler.addProxy(address, port);
      
      EasyMock.verify(this.keyProvider, this.partition, this.singleton);
      
      events = this.handler.getPendingDiscoveryEvents();
      
      Assert.assertNotNull(events);
      Assert.assertEquals(3, events.size());
      
      event = events.get(2);
      
      Assert.assertSame(node, event.getSender());
      Assert.assertSame(address, event.getMCMPServer().getAddress());
      Assert.assertEquals(port, event.getMCMPServer().getPort());
      Assert.assertTrue(event.isAddition());
      Assert.assertEquals(3, event.getEventIndex());
      
      Assert.assertNotNull(capturedEvents.getValue());
      Assert.assertEquals(1, capturedEvents.getValue().length);
      Assert.assertSame(event, capturedEvents.getValue()[0]);
      
      EasyMock.reset(this.keyProvider, this.partition, this.singleton);
      
      
      // Test MCMPHandler.addProxy(InetAddress, int, boolean)
      this.localHandler.addProxy(address, port, true);
      
      EasyMock.replay(this.localHandler);
      
      this.handler.addProxy(address, port, true);
      
      EasyMock.verify(this.localHandler);
      EasyMock.reset(this.localHandler);
   }
   
   @Test
   public void removeProxy() throws Exception
   {
      InetAddress address = InetAddress.getLocalHost();
      int port = 0;
      
      // Test master use case
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(true);
      
      // Test MCMPHandler.removeProxy(String, int)
      this.localHandler.removeProxy(address, port);
      
      EasyMock.replay(this.localHandler, this.singleton);
      
      this.handler.removeProxy(address.getHostName(), port);
      
      EasyMock.verify(this.localHandler, this.singleton);
      EasyMock.reset(this.localHandler, this.singleton);
      
      
      // Test MCMPHandler.removeProxy(InetAddress, int)
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(true);
      
      this.localHandler.removeProxy(address, port);
      
      EasyMock.replay(this.localHandler, this.singleton);
      
      this.handler.removeProxy(address, port);
      
      EasyMock.verify(this.localHandler, this.singleton);
      EasyMock.reset(this.localHandler, this.singleton);
      
      
      // Test non-master use case
      String key = "key";
      Capture<Object[]> capturedEvents = new Capture<Object[]>();
      ClusterNode node = EasyMock.createMock(ClusterNode.class);
      
      // Test MCMPHandler.removeProxy(String, int)
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(false);
      
      EasyMock.expect(this.keyProvider.getHAPartition()).andReturn(this.partition);
      EasyMock.expect(this.partition.getClusterNode()).andReturn(node);
      
      EasyMock.expect(this.keyProvider.getHAPartition()).andReturn(this.partition);
      EasyMock.expect(this.keyProvider.getHAServiceKey()).andReturn(key);
      
      EasyMock.expect(this.partition.callMethodOnCluster(EasyMock.eq(key), EasyMock.eq("mcmpServerDiscoveryEvent"), EasyMock.capture(capturedEvents), EasyMock.aryEq(new Class[] { MCMPServerDiscoveryEvent.class }), EasyMock.eq(false), EasyMock.isA(GroupRpcResponseFilter.class))).andReturn(new ArrayList<GroupRpcResponse>(Collections.singleton(new GroupRpcResponse(node))));
      
      EasyMock.replay(this.keyProvider, this.partition, this.singleton);

      this.handler.removeProxy(address.getHostName(), port);
      
      EasyMock.verify(this.keyProvider, this.partition, this.singleton);
      
      List<MCMPServerDiscoveryEvent> events = this.handler.getPendingDiscoveryEvents();
      
      Assert.assertNotNull(events);
      Assert.assertEquals(1, events.size());
      
      MCMPServerDiscoveryEvent event = events.get(0);
      
      Assert.assertSame(node, event.getSender());
      Assert.assertSame(address, event.getMCMPServer().getAddress());
      Assert.assertEquals(port, event.getMCMPServer().getPort());
      Assert.assertFalse(event.isAddition());
      Assert.assertEquals(1, event.getEventIndex());
      
      Assert.assertNotNull(capturedEvents.getValue());
      Assert.assertEquals(1, capturedEvents.getValue().length);
      Assert.assertSame(event, capturedEvents.getValue()[0]);
      
      EasyMock.reset(this.keyProvider, this.partition, this.singleton);
      
      
      // Test MCMPHandler.removeProxy(InetAddress, int)
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(false);
      
      EasyMock.expect(this.keyProvider.getHAPartition()).andReturn(this.partition);
      EasyMock.expect(this.partition.getClusterNode()).andReturn(node);
      
      EasyMock.expect(this.keyProvider.getHAPartition()).andReturn(this.partition);
      EasyMock.expect(this.keyProvider.getHAServiceKey()).andReturn(key);
      
      EasyMock.expect(this.partition.callMethodOnCluster(EasyMock.eq(key), EasyMock.eq("mcmpServerDiscoveryEvent"), EasyMock.capture(capturedEvents), EasyMock.aryEq(new Class[] { MCMPServerDiscoveryEvent.class }), EasyMock.eq(false), EasyMock.isA(GroupRpcResponseFilter.class))).andReturn(new ArrayList<GroupRpcResponse>(Collections.singleton(new GroupRpcResponse(node))));
      
      EasyMock.replay(this.keyProvider, this.partition, this.singleton);

      this.handler.removeProxy(address, port);
      
      EasyMock.verify(this.keyProvider, this.partition, this.singleton);
      
      events = this.handler.getPendingDiscoveryEvents();
      
      Assert.assertNotNull(events);
      Assert.assertEquals(2, events.size());
      
      event = events.get(1);
      
      Assert.assertSame(node, event.getSender());
      Assert.assertSame(address, event.getMCMPServer().getAddress());
      Assert.assertEquals(port, event.getMCMPServer().getPort());
      Assert.assertFalse(event.isAddition());
      Assert.assertEquals(2, event.getEventIndex());
      
      Assert.assertNotNull(capturedEvents.getValue());
      Assert.assertEquals(1, capturedEvents.getValue().length);
      Assert.assertSame(event, capturedEvents.getValue()[0]);
      
      EasyMock.reset(this.keyProvider, this.partition, this.singleton);
   }
   
   @Test
   public void getProxyStates()
   {
      Set<MCMPServerState> states = Collections.emptySet();
      
      EasyMock.expect(this.localHandler.getProxyStates()).andReturn(states);
      
      EasyMock.replay(this.localHandler);
      
      Set<MCMPServerState> result = this.handler.getProxyStates();
      
      EasyMock.verify(this.localHandler);
      
      Assert.assertSame(states, result);
      
      EasyMock.reset(this.localHandler);
   }
   
   @Test
   public void getLocalAddress() throws UnknownHostException, IOException
   {
      InetAddress address = InetAddress.getLocalHost();
      
      EasyMock.expect(this.localHandler.getLocalAddress()).andReturn(address);
      
      EasyMock.replay(this.localHandler);
      
      InetAddress result = this.handler.getLocalAddress();
      
      EasyMock.verify(this.localHandler);
      
      Assert.assertSame(address, result);
      
      EasyMock.reset(this.localHandler);
   }
   
   @Test
   public void getProxyConfiguration() throws Exception
   {
      String configuration = "configuration";
      
      // Test master use case
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(true);
      
      EasyMock.expect(this.localHandler.getProxyConfiguration()).andReturn(configuration);
      
      EasyMock.replay(this.localHandler, this.singleton);
      
      String result = this.handler.getProxyConfiguration();
      
      EasyMock.verify(this.localHandler, this.singleton);
      
      Assert.assertSame(configuration, result);
      
      EasyMock.reset(this.localHandler, this.singleton);
      
      
      // Test non-master use case
      String key = "key";
      ClusterNode node = EasyMock.createMock(ClusterNode.class);
      
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(false);
      
      EasyMock.expect(this.keyProvider.getHAPartition()).andReturn(this.partition);
      EasyMock.expect(this.keyProvider.getHAServiceKey()).andReturn(key);
      
      EasyMock.expect(this.partition.callMethodOnCluster(EasyMock.same(key), EasyMock.eq("getProxyConfiguration"), EasyMock.aryEq(new Object[0]), EasyMock.aryEq(new Class[0]), EasyMock.eq(false), EasyMock.isA(GroupRpcResponseFilter.class))).andReturn(new ArrayList<GroupRpcResponse>(Collections.singleton(new StringGroupRpcResponse(node, configuration))));
      
      EasyMock.replay(this.keyProvider, this.partition, this.singleton);
      
      result = this.handler.getProxyConfiguration();
      
      EasyMock.verify(this.keyProvider, this.partition, this.singleton);
      
      Assert.assertSame(configuration, result);
      
      EasyMock.reset(this.keyProvider, this.partition, this.singleton);
   }
   
   @Test
   public void isProxyHealthOK() throws Exception
   {
      // Test master use case
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(true);
      
      EasyMock.expect(this.localHandler.isProxyHealthOK()).andReturn(true);
      
      EasyMock.replay(this.localHandler, this.singleton);
      
      boolean result = this.handler.isProxyHealthOK();
      
      EasyMock.verify(this.localHandler, this.singleton);
      
      Assert.assertTrue(result);
      
      EasyMock.reset(this.localHandler, this.singleton);
      
      
      // Test non-master use case
      String key = "key";
      ClusterNode node = EasyMock.createMock(ClusterNode.class);
      
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(false);
      
      EasyMock.expect(this.keyProvider.getHAPartition()).andReturn(this.partition);
      EasyMock.expect(this.keyProvider.getHAServiceKey()).andReturn(key);
      
      EasyMock.expect(this.partition.callMethodOnCluster(EasyMock.same(key), EasyMock.eq("isProxyHealthOk"), EasyMock.aryEq(new Object[0]), EasyMock.aryEq(new Class[0]), EasyMock.eq(false), EasyMock.isA(GroupRpcResponseFilter.class))).andReturn(new ArrayList<GroupRpcResponse>(Collections.singleton(new BooleanGroupRpcResponse(node, true))));
      
      EasyMock.replay(this.keyProvider, this.partition, this.singleton);
      
      result = this.handler.isProxyHealthOK();
      
      EasyMock.verify(this.keyProvider, this.partition, this.singleton);
      
      Assert.assertTrue(result);
      
      EasyMock.reset(this.keyProvider, this.partition, this.singleton);
   }
   
   @Test
   public void markProxiesInError() throws Exception
   {
      // Test master use case
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(true);
      
      this.localHandler.markProxiesInError();
      
      EasyMock.replay(this.localHandler, this.singleton);
      
      this.handler.markProxiesInError();
      
      EasyMock.verify(this.localHandler, this.singleton);
      EasyMock.reset(this.localHandler, this.singleton);
      
      
      // Test non-master use case
      String key = "key";
      ClusterNode node = EasyMock.createMock(ClusterNode.class);
      
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(false);
      
      EasyMock.expect(this.keyProvider.getHAPartition()).andReturn(this.partition);
      EasyMock.expect(this.keyProvider.getHAServiceKey()).andReturn(key);
      
      EasyMock.expect(this.partition.callMethodOnCluster(EasyMock.same(key), EasyMock.eq("markProxiesInError"), EasyMock.aryEq(new Object[0]), EasyMock.aryEq(new Class[0]), EasyMock.eq(false), EasyMock.isA(GroupRpcResponseFilter.class))).andReturn(new ArrayList<GroupRpcResponse>(Collections.singleton(new BooleanGroupRpcResponse(node, true))));
      
      EasyMock.replay(this.keyProvider, this.partition, this.singleton);
      
      this.handler.markProxiesInError();
      
      EasyMock.verify(this.keyProvider, this.partition, this.singleton);
      EasyMock.reset(this.keyProvider, this.partition, this.singleton);
   }
   
   @Test
   public void reset() throws Exception
   {
      // Test master use case
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(true);
      
      this.localHandler.reset();
      
      EasyMock.replay(this.localHandler, this.singleton);
      
      this.handler.reset();
      
      EasyMock.verify(this.localHandler, this.singleton);
      EasyMock.reset(this.localHandler, this.singleton);
      
      
      // Test non-master use case
      String key = "key";
      ClusterNode node = EasyMock.createMock(ClusterNode.class);
      
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(false);
      
      EasyMock.expect(this.keyProvider.getHAPartition()).andReturn(this.partition);
      EasyMock.expect(this.keyProvider.getHAServiceKey()).andReturn(key);
      
      EasyMock.expect(this.partition.callMethodOnCluster(EasyMock.same(key), EasyMock.eq("reset"), EasyMock.aryEq(new Object[0]), EasyMock.aryEq(new Class[0]), EasyMock.eq(false), EasyMock.isA(GroupRpcResponseFilter.class))).andReturn(new ArrayList<GroupRpcResponse>(Collections.singleton(new BooleanGroupRpcResponse(node, true))));
      
      EasyMock.replay(this.keyProvider, this.partition, this.singleton);
      
      this.handler.reset();
      
      EasyMock.verify(this.keyProvider, this.partition, this.singleton);
      EasyMock.reset(this.keyProvider, this.partition, this.singleton);
   }
   
   @Test
   public void sendRequest() throws Exception
   {
      MCMPRequest request = EasyMock.createMock(MCMPRequest.class);
      
      // Test master use case
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(true);
      
      this.localHandler.sendRequest(EasyMock.same(request));
      
      EasyMock.replay(this.localHandler, this.singleton);
      
      this.handler.sendRequest(request);
      
      EasyMock.verify(this.localHandler, this.singleton);
      EasyMock.reset(this.localHandler, this.singleton);
      
      
      // Test non-master use case
      String key = "key";
      
      ClusterNode node = EasyMock.createMock(ClusterNode.class);
      Capture<Object[]> captured = new Capture<Object[]>();
      
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(false);
      
      EasyMock.expect(this.keyProvider.getHAPartition()).andReturn(this.partition);
      EasyMock.expect(this.keyProvider.getHAServiceKey()).andReturn(key);
      
      EasyMock.expect(this.partition.callMethodOnCluster(EasyMock.same(key), EasyMock.eq("sendRequest"), EasyMock.capture(captured), EasyMock.aryEq(new Class[] { MCMPRequest.class }), EasyMock.eq(false), EasyMock.isA(GroupRpcResponseFilter.class))).andReturn(new ArrayList<GroupRpcResponse>(Collections.singleton(new GroupRpcResponse(node))));
      
      EasyMock.replay(this.keyProvider, this.partition, this.singleton);
      
      this.handler.sendRequest(request);
      
      EasyMock.verify(this.keyProvider, this.partition, this.singleton);
      
      Assert.assertNotNull(captured.getValue());
      Assert.assertEquals(1, captured.getValue().length);
      Assert.assertSame(request, captured.getValue()[0]);
      
      EasyMock.reset(this.keyProvider, this.partition, this.singleton);
   }
   
   @Test
   public void sendRequests() throws Exception
   {
      List<MCMPRequest> requests = Collections.emptyList();
      
      // Test master use case
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(true);
      
      this.localHandler.sendRequests(EasyMock.same(requests));
      
      EasyMock.replay(this.localHandler, this.singleton);
      
      this.handler.sendRequests(requests);
      
      EasyMock.verify(this.localHandler, this.singleton);
      EasyMock.reset(this.localHandler, this.singleton);
      
      
      // Test non-master use case
      String key = "key";
      
      ClusterNode node = EasyMock.createMock(ClusterNode.class);
      Capture<Object[]> captured = new Capture<Object[]>();
      
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(false);
      
      EasyMock.expect(this.keyProvider.getHAPartition()).andReturn(this.partition);
      EasyMock.expect(this.keyProvider.getHAServiceKey()).andReturn(key);
      
      EasyMock.expect(this.partition.callMethodOnCluster(EasyMock.same(key), EasyMock.eq("sendRequests"), EasyMock.capture(captured), EasyMock.aryEq(new Class[] { List.class }), EasyMock.eq(false), EasyMock.isA(GroupRpcResponseFilter.class))).andReturn(new ArrayList<GroupRpcResponse>(Collections.singleton(new GroupRpcResponse(node))));
      
      EasyMock.replay(this.keyProvider, this.partition, this.singleton);
      
      this.handler.sendRequests(requests);
      
      EasyMock.verify(this.keyProvider, this.partition, this.singleton);
      
      Assert.assertNotNull(captured.getValue());
      Assert.assertEquals(1, captured.getValue().length);
      Assert.assertSame(requests, captured.getValue()[0]);
      
      EasyMock.reset(this.keyProvider, this.partition, this.singleton);
   }
   
   @Test
   public void shutdown()
   {
      this.localHandler.shutdown();
      
      EasyMock.replay(this.localHandler);
      
      this.handler.shutdown();
      
      EasyMock.verify(this.localHandler);
      EasyMock.reset(this.localHandler);
   }
   
   @Test
   public void getNeedsResetTransmission()
   {
      Assert.assertFalse(this.handler.isResetNecessary());
      
      // Set State to ERROR
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(true);
      this.localHandler.markProxiesInError();
      
      EasyMock.replay(this.singleton, this.localHandler);
      
      this.handler.markProxiesInError();
      
      EasyMock.verify(this.singleton, this.localHandler);
      
      Assert.assertTrue(this.handler.isResetNecessary());
      
      EasyMock.reset(this.singleton, this.localHandler);
      
      this.handler.resetInitiated();
      
      Assert.assertFalse(this.handler.isResetNecessary());

      // Set State to ERROR again
      EasyMock.expect(this.singleton.isMasterNode()).andReturn(true);
      this.localHandler.markProxiesInError();
      
      EasyMock.replay(this.singleton, this.localHandler);
      
      this.handler.markProxiesInError();
      
      EasyMock.verify(this.singleton, this.localHandler);
      
      Assert.assertTrue(this.handler.isResetNecessary());
      
      EasyMock.reset(this.singleton, this.localHandler);
      
      this.handler.resetCompleted();
      
      Assert.assertTrue(this.handler.isResetNecessary());
      
      this.handler.resetInitiated();
      
      Assert.assertFalse(this.handler.isResetNecessary());

      this.handler.resetCompleted();
      
      Assert.assertFalse(this.handler.isResetNecessary());
   }
}
