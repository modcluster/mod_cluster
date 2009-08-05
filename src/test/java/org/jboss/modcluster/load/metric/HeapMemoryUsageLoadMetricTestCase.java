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
package org.jboss.modcluster.load.metric;

import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import org.easymock.EasyMock;
import org.jboss.modcluster.load.metric.impl.HeapMemoryUsageLoadMetric;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
public class HeapMemoryUsageLoadMetricTestCase
{
   private MemoryMXBean bean = EasyMock.createStrictMock(MemoryMXBean.class);
   private HeapMemoryUsageLoadMetric metric = new HeapMemoryUsageLoadMetric(this.bean);

   @Test
   public void getLoad() throws Exception
   {
      LoadContext context = EasyMock.createMock(LoadContext.class);
      
      MemoryUsage usage = new MemoryUsage(0, 1000, 2000, 5000);
      
      EasyMock.expect(this.bean.getHeapMemoryUsage()).andReturn(usage);
      
      EasyMock.replay(this.bean);
      
      double load = this.metric.getLoad(context);
      
      EasyMock.verify(this.bean);
      
      Assert.assertEquals(0.2, load, 0.0);
      
      EasyMock.reset(this.bean);
   }

   @Test
   public void getLoadNoMax() throws Exception
   {
      LoadContext context = EasyMock.createMock(LoadContext.class);
      
      MemoryUsage usage = new MemoryUsage(0, 1000, 2000, -1);
      
      EasyMock.expect(this.bean.getHeapMemoryUsage()).andReturn(usage);
      
      EasyMock.replay(this.bean);
      
      double load = this.metric.getLoad(context);
      
      EasyMock.verify(this.bean);
      
      Assert.assertEquals(0.5, load, 0.0);
      
      EasyMock.reset(this.bean);
   }
   
   @Test
   public void createContext()
   {
      LoadContext context = this.metric.createContext();
      
      Assert.assertNotNull(context);
   }
}
