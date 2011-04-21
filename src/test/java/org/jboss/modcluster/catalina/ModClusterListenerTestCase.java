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
package org.jboss.modcluster.catalina;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.easymock.EasyMock;
import org.jboss.modcluster.ModClusterServiceMBean;
import org.jboss.modcluster.load.LoadBalanceFactorProvider;
import org.jboss.modcluster.load.impl.DynamicLoadBalanceFactorProviderMBean;
import org.jboss.modcluster.load.metric.LoadMetricMBean;
import org.jboss.modcluster.load.metric.impl.ActiveSessionsLoadMetric;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
@SuppressWarnings("boxing")
public class ModClusterListenerTestCase
{
   private final ModClusterServiceMBean mbean = EasyMock.createStrictMock(ModClusterServiceMBean.class);
   private final LifecycleListener lifecycleListener = EasyMock.createStrictMock(LifecycleListener.class);
   
   private final ModClusterListener listener = new ModClusterListener(this.mbean, this.lifecycleListener);
   
   @Test
   public void createLoadBalanceFactorProvider() throws ClassNotFoundException
   {
      this.listener.setLoadMetricClass(ActiveSessionsLoadMetric.class.getName());
      this.listener.setLoadMetricCapacity("100");
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
      String host = "host";
      int port = 10;
      
      this.mbean.addProxy(host, port);
      
      EasyMock.replay(this.mbean);
      
      this.listener.addProxy(host, port);
      
      EasyMock.verify(this.mbean);
      EasyMock.reset(this.mbean);
   }

   @Test
   public void disableAll()
   {
      EasyMock.expect(this.mbean.disable()).andReturn(true);
      
      EasyMock.replay(this.mbean);
      
      boolean result = this.listener.disable();
      
      EasyMock.verify(this.mbean);
      
      Assert.assertTrue(result);
      
      EasyMock.reset(this.mbean);
   }

   @Test
   public void disable()
   {
      String host = "host";
      String context = "context";
      
      EasyMock.expect(this.mbean.disableContext(host, context)).andReturn(true);
      
      EasyMock.replay(this.mbean);
      
      boolean result = this.listener.disableContext(host, context);
      
      EasyMock.verify(this.mbean);
      
      Assert.assertTrue(result);
      
      EasyMock.reset(this.mbean);
   }

   @Test
   public void ping()
   {
      Map<InetSocketAddress, String> expected = Collections.singletonMap(InetSocketAddress.createUnresolved("localhost", 8080), "OK");
      
      String jvmRoute = "route";
      
      EasyMock.expect(this.mbean.ping(jvmRoute)).andReturn(expected);
      
      EasyMock.replay(this.mbean);
      
      Map<InetSocketAddress, String> result = this.listener.ping(jvmRoute);
      
      EasyMock.verify(this.mbean);

      Assert.assertSame(expected, result);
      
      EasyMock.reset(this.mbean);
   }

   @Test
   public void enableAll()
   {
      EasyMock.expect(this.mbean.enable()).andReturn(true);
      
      EasyMock.replay(this.mbean);
      
      boolean result = this.listener.enable();
      
      EasyMock.verify(this.mbean);
      
      Assert.assertTrue(result);
      
      EasyMock.reset(this.mbean);
   }

   @Test
   public void enable()
   {
      String host = "host";
      String context = "context";
      
      EasyMock.expect(this.mbean.enableContext(host, context)).andReturn(true);
      
      EasyMock.replay(this.mbean);
      
      boolean result = this.listener.enableContext(host, context);
      
      EasyMock.verify(this.mbean);
      
      Assert.assertTrue(result);
      
      EasyMock.reset(this.mbean);
   }

   @Test
   public void getProxyConfiguration()
   {
      Map<InetSocketAddress, String> expected = Collections.singletonMap(InetSocketAddress.createUnresolved("localhost", 8080), "config");
      
      EasyMock.expect(this.mbean.getProxyConfiguration()).andReturn(expected);
      
      EasyMock.replay(this.mbean);
      
      Map<InetSocketAddress, String> result = this.listener.getProxyConfiguration();
      
      EasyMock.verify(this.mbean);

      Assert.assertSame(expected, result);
      
      EasyMock.reset(this.mbean);
   }

   @Test
   public void getProxyInfo()
   {
      Map<InetSocketAddress, String> expected = Collections.singletonMap(InetSocketAddress.createUnresolved("localhost", 8080), "info");
      
      EasyMock.expect(this.mbean.getProxyInfo()).andReturn(expected);
      
      EasyMock.replay(this.mbean);
      
      Map<InetSocketAddress, String> result = this.listener.getProxyInfo();
      
      EasyMock.verify(this.mbean);

      Assert.assertSame(expected, result);
      
      EasyMock.reset(this.mbean);
   }

   @Test
   public void refresh()
   {
      this.mbean.refresh();
      
      EasyMock.replay(this.mbean);
      
      this.listener.refresh();
      
      EasyMock.verify(this.mbean);
      EasyMock.reset(this.mbean);
   }

   @Test
   public void removeProxy()
   {
      String host = "host";
      int port = 10;
      
      this.mbean.removeProxy(host, port);
      
      EasyMock.replay(this.mbean);
      
      this.listener.removeProxy(host, port);
      
      EasyMock.verify(this.mbean);
      EasyMock.reset(this.mbean);
   }

   @Test
   public void reset()
   {
      this.mbean.reset();
      
      EasyMock.replay(this.mbean);
      
      this.listener.reset();
      
      EasyMock.verify(this.mbean);
      EasyMock.reset(this.mbean);
   }
   
   @Test
   public void gracefulStop()
   {
      EasyMock.expect(this.mbean.stop(10, TimeUnit.SECONDS)).andReturn(true);
      
      EasyMock.replay(this.mbean);
      
      boolean result = this.listener.stop(10, TimeUnit.SECONDS);
      
      EasyMock.verify(this.mbean);
      
      Assert.assertTrue(result);
      
      EasyMock.reset(this.mbean);
   }
   
   @Test
   public void gracefulStopContext()
   {
      EasyMock.expect(this.mbean.stopContext("host", "path", 10, TimeUnit.SECONDS)).andReturn(true);
      
      EasyMock.replay(this.mbean);
      
      boolean result = this.listener.stopContext("host", "path", 10, TimeUnit.SECONDS);
      
      EasyMock.verify(this.mbean);
      
      Assert.assertTrue(result);
      
      EasyMock.reset(this.mbean);
   }
}
