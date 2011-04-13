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
package org.jboss.modcluster;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.catalina.Engine;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.easymock.EasyMock;
import org.jboss.modcluster.load.LoadBalanceFactorProvider;
import org.jboss.modcluster.load.impl.DynamicLoadBalanceFactorProviderMBean;
import org.jboss.modcluster.load.metric.LoadMetricMBean;
import org.jboss.modcluster.load.metric.impl.ActiveSessionsLoadMetric;
import org.jboss.modcluster.mcmp.MCMPHandler;
import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPRequestFactory;
import org.jboss.modcluster.mcmp.MCMPServerState;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
public class ModClusterListenerTestCase
{
   private final MCMPHandler mcmpHandler = EasyMock.createStrictMock(MCMPHandler.class);
   private final MCMPRequestFactory requestFactory = EasyMock.createStrictMock(MCMPRequestFactory.class);
   private final LifecycleListener lifecycleListener = EasyMock.createStrictMock(LifecycleListener.class);
   @SuppressWarnings("unchecked")
   private final ServerProvider<Server> serverProvider = EasyMock.createStrictMock(ServerProvider.class);
   
   private final ModClusterListener listener = new ModClusterListener(this.mcmpHandler, this.requestFactory, this.serverProvider, this.lifecycleListener);
   
   @Test
   public void createLoadBalanceFactorProvider() throws ClassNotFoundException
   {
      this.listener.setLoadMetricClass(ActiveSessionsLoadMetric.class.getName());
      this.listener.setLoadMetricCapacity(100);
      this.listener.setLoadDecayFactor(3);
      this.listener.setLoadHistory(5);
      
      LoadBalanceFactorProvider result = this.listener.createLoadBalanceFactorProvider();
      
      Assert.assertTrue(result instanceof DynamicLoadBalanceFactorProviderMBean);
      
      DynamicLoadBalanceFactorProviderMBean lbfProvider = (DynamicLoadBalanceFactorProviderMBean) result;
      
      Assert.assertEquals(3, lbfProvider.getDecayFactor());
      Assert.assertEquals(5, lbfProvider.getHistory());
      
      Collection<LoadMetricMBean> metrics = lbfProvider.getMetrics();
      Assert.assertEquals(1, metrics.size());
      
      LoadMetricMBean metric = metrics.iterator().next();
      Assert.assertEquals(100, metric.getCapacity(), 0);
      Assert.assertEquals(1, metric.getWeight());
   }
   
   @Test
   public void lifecycleEvent()
   {
      LifecycleEvent event = new LifecycleEvent(EasyMock.createMock(Lifecycle.class), Lifecycle.INIT_EVENT);
      
      this.lifecycleListener.lifecycleEvent(event);
      
      EasyMock.replay(this.lifecycleListener);
      
      this.listener.lifecycleEvent(event);
      
      EasyMock.verify(this.lifecycleListener);
      EasyMock.reset(this.lifecycleListener);
   }
   
   @Test
   public void addProxy()
   {
      this.mcmpHandler.addProxy("host", 100);
      
      EasyMock.replay(this.mcmpHandler);
      
      this.listener.addProxy("host", 100);
      
      EasyMock.verify(this.mcmpHandler);
      EasyMock.reset(this.mcmpHandler);
   }
   
   @Test
   public void removeProxy()
   {
      this.mcmpHandler.removeProxy("host", 100);
      
      EasyMock.replay(this.mcmpHandler);
      
      this.listener.removeProxy("host", 100);
      
      EasyMock.verify(this.mcmpHandler);
      EasyMock.reset(this.mcmpHandler);
   }
   
   @Test
   public void getProxyConfiguration()
   {
      EasyMock.expect(this.mcmpHandler.getProxyConfiguration()).andReturn("config");
      
      EasyMock.replay(this.mcmpHandler);
      
      String result = this.listener.getProxyConfiguration();
      
      EasyMock.verify(this.mcmpHandler);
      
      Assert.assertEquals("config", result);
      
      EasyMock.reset(this.mcmpHandler);
   }
   
   @Test
   public void getProxyInfo()
   {
      EasyMock.expect(this.mcmpHandler.getProxyInfo()).andReturn("info");
      
      EasyMock.replay(this.mcmpHandler);
      
      String result = this.listener.getProxyInfo();
      
      EasyMock.verify(this.mcmpHandler);
      
      Assert.assertEquals("info", result);
      
      EasyMock.reset(this.mcmpHandler);
   }
   
   @Test
   public void reset()
   {
      this.mcmpHandler.reset();
      
      EasyMock.replay(this.mcmpHandler);
      
      this.listener.reset();
      
      EasyMock.verify(this.mcmpHandler);
      EasyMock.reset(this.mcmpHandler);
   }
   
   @Test
   public void refresh()
   {
      this.mcmpHandler.markProxiesInError();
      
      EasyMock.replay(this.mcmpHandler);
      
      this.listener.refresh();
      
      EasyMock.verify(this.mcmpHandler);
      EasyMock.reset(this.mcmpHandler);
   }
   
   @SuppressWarnings("boxing")
   @Test
   public void enable()
   {
      Server server = EasyMock.createStrictMock(Server.class);
      Service service = EasyMock.createStrictMock(Service.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      MCMPRequest request = EasyMock.createStrictMock(MCMPRequest.class);
      Map<MCMPServerState, String> emptyMap = Collections.emptyMap();
      
      EasyMock.expect(this.serverProvider.getServer()).andReturn(server);
      EasyMock.expect(server.findServices()).andReturn(new Service[] { service });
      EasyMock.expect(service.getContainer()).andReturn(engine);

      EasyMock.expect(this.requestFactory.createEnableRequest(engine)).andReturn(request);
      
      EasyMock.expect(this.mcmpHandler.sendRequest(request)).andReturn(emptyMap);
      
      EasyMock.expect(this.mcmpHandler.isProxyHealthOK()).andReturn(true);
      
      EasyMock.replay(this.serverProvider, this.mcmpHandler, this.requestFactory, server, service, engine);
      
      boolean result = this.listener.enable();
      
      EasyMock.verify(this.serverProvider, this.mcmpHandler, this.requestFactory, server, service, engine);
      
      Assert.assertTrue(result);
   }
   
   @SuppressWarnings("boxing")
   @Test
   public void disable()
   {
      Server server = EasyMock.createStrictMock(Server.class);
      Service service = EasyMock.createStrictMock(Service.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      MCMPRequest request = EasyMock.createStrictMock(MCMPRequest.class);
      Map<MCMPServerState, String> emptyMap = Collections.emptyMap();
      
      EasyMock.expect(this.serverProvider.getServer()).andReturn(server);
      EasyMock.expect(server.findServices()).andReturn(new Service[] { service });
      EasyMock.expect(service.getContainer()).andReturn(engine);
      
      EasyMock.expect(this.requestFactory.createDisableRequest(engine)).andReturn(request);
      
      EasyMock.expect(this.mcmpHandler.sendRequest(request)).andReturn(emptyMap);
      
      EasyMock.expect(this.mcmpHandler.isProxyHealthOK()).andReturn(true);
      
      EasyMock.replay(this.serverProvider, this.mcmpHandler, this.requestFactory, server, service, engine);
      
      boolean result = this.listener.disable();
      
      EasyMock.verify(this.serverProvider, this.mcmpHandler, this.requestFactory, server, service, engine);
      
      Assert.assertTrue(result);
   }
}
