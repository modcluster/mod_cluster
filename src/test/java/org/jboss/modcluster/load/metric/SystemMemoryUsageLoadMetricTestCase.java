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

import java.lang.management.ManagementFactory;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.easymock.EasyMock;
import org.jboss.modcluster.load.metric.impl.MBeanLoadContext;
import org.jboss.modcluster.load.metric.impl.SystemMemoryUsageLoadMetric;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
public class SystemMemoryUsageLoadMetricTestCase
{
   private final MBeanServer server = EasyMock.createStrictMock(MBeanServer.class);
   
   private LoadMetric<MBeanLoadContext> metric;
   
   private ObjectName name;
   private MBeanLoadContext context;
   
   @Before
   public void prepare() throws MalformedObjectNameException
   {
      this.metric = new SystemMemoryUsageLoadMetric();
      this.name = ObjectName.getInstance(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
      this.context = new MBeanLoadContext(this.server, this.name);
   }
   
   @SuppressWarnings("boxing")
   @Test
   public void getLoad() throws Exception
   {
      EasyMock.expect(this.server.getAttribute(this.name, SystemMemoryUsageLoadMetric.FREE_MEMORY)).andReturn(256);
      EasyMock.expect(this.server.getAttribute(this.name, SystemMemoryUsageLoadMetric.TOTAL_MEMORY)).andReturn(1024L);
      
      EasyMock.replay(this.server);
      
      double load = this.metric.getLoad(this.context);
      
      EasyMock.verify(this.server);
      
      Assert.assertEquals(0.75, load, 0.0);
      
      EasyMock.reset(this.server);

      
      // Test Java 1.5 behavior
      EasyMock.expect(this.server.getAttribute(this.name, SystemMemoryUsageLoadMetric.FREE_MEMORY)).andThrow(new AttributeNotFoundException());
      
      EasyMock.replay(this.server);
      
      load = this.metric.getLoad(this.context);
      
      EasyMock.verify(this.server);
      
      Assert.assertEquals(0.0, load, 0.0);
      Assert.assertEquals(0, this.metric.getWeight());
   }
}
