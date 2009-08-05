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

import javax.management.MalformedObjectNameException;

import org.easymock.EasyMock;
import org.jboss.modcluster.load.metric.impl.RequestCountLoadMetric;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
@SuppressWarnings("boxing")
public class RequestCountLoadMetricTestCase extends MBeanAttributeLoadMetricTestCase
{
   public RequestCountLoadMetricTestCase() throws MalformedObjectNameException
   {
      super(new RequestCountLoadMetric(), RequestCountLoadMetric.DEFAULT_ATTRIBUTE);
   }

   @Test
   @Override
   public void getLoad() throws Exception
   {
      EasyMock.expect(this.server.getAttribute(this.name1, RequestCountLoadMetric.DEFAULT_ATTRIBUTE)).andReturn(0);
      EasyMock.expect(this.server.getAttribute(this.name2, RequestCountLoadMetric.DEFAULT_ATTRIBUTE)).andReturn(0);
      
      EasyMock.replay(this.server);
      
      long start = System.currentTimeMillis();
      
      double load = this.metric.getLoad(this.context);
      
      EasyMock.verify(this.server);
      
      Assert.assertEquals(0, load, 0);
      
      EasyMock.reset(this.server);
      
      EasyMock.expect(this.server.getAttribute(this.name1, RequestCountLoadMetric.DEFAULT_ATTRIBUTE)).andReturn(10);
      EasyMock.expect(this.server.getAttribute(this.name2, RequestCountLoadMetric.DEFAULT_ATTRIBUTE)).andReturn(20);
      
      EasyMock.replay(this.server);

      long sleep = 1000 - (System.currentTimeMillis() - start);
      
      if (sleep > 0)
      {
         Thread.sleep(sleep);
      }

      start = System.currentTimeMillis();
      
      load = this.metric.getLoad(this.context);
      
      EasyMock.verify(this.server);
      
      Assert.assertEquals(30.0, load, 2.0);
      
      EasyMock.reset(this.server);
      
      // Test incremental load
      EasyMock.expect(this.server.getAttribute(this.name1, RequestCountLoadMetric.DEFAULT_ATTRIBUTE)).andReturn(20);
      EasyMock.expect(this.server.getAttribute(this.name2, RequestCountLoadMetric.DEFAULT_ATTRIBUTE)).andReturn(30);
      
      EasyMock.replay(this.server);
      
      sleep = 1000 - (System.currentTimeMillis() - start);
      
      if (sleep > 0)
      {
         Thread.sleep(sleep);
      }
      
      load = this.metric.getLoad(this.context);
      
      EasyMock.verify(this.server);
      
      Assert.assertEquals(20.0, load, 2.0);
      
      EasyMock.reset(this.server);
   }
}
