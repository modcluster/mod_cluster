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

import java.util.Arrays;
import java.util.LinkedHashSet;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.easymock.EasyMock;
import org.jboss.modcluster.load.metric.impl.MBeanAttributeLoadMetric;
import org.jboss.modcluster.load.metric.impl.MBeanQueryLoadContext;
import org.jboss.modcluster.load.metric.impl.MBeanQueryLoadMetricSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
@SuppressWarnings("boxing")
public class MBeanAttributeLoadMetricTestCase
{
   MBeanServer server = EasyMock.createStrictMock(MBeanServer.class); 
   ObjectName name1;
   ObjectName name2;
   MBeanQueryLoadContext context;

   private String attribute;
   LoadMetric<MBeanQueryLoadContext> metric;
   
   public MBeanAttributeLoadMetricTestCase() throws MalformedObjectNameException
   {
      this("attribute");
   }
   
   private MBeanAttributeLoadMetricTestCase(String attribute) throws MalformedObjectNameException
   {
      this(new MBeanAttributeLoadMetric(new MBeanQueryLoadMetricSource("domain:*"), attribute), attribute);
   }
   
   protected MBeanAttributeLoadMetricTestCase(LoadMetric<MBeanQueryLoadContext> metric, String attribute)
   {
      this.metric = metric;
      this.attribute = attribute;
   }
   
   @Before
   public void prepare() throws MalformedObjectNameException
   {
      ObjectName pattern = ObjectName.getInstance("domain:*");
      this.name1 = ObjectName.getInstance("domain:name=test1");
      this.name2 = ObjectName.getInstance("domain:name=test2");
      
      EasyMock.expect(this.server.queryNames(pattern, null)).andReturn(new LinkedHashSet<ObjectName>(Arrays.asList(this.name1, this.name2)));
      
      EasyMock.replay(this.server);
      
      this.context = new MBeanQueryLoadContext(this.server, pattern);
      
      EasyMock.verify(this.server);
      EasyMock.reset(this.server);
   }
   
   @Test
   public void getLoad() throws Exception
   {
      EasyMock.expect(this.server.getAttribute(this.name1, this.attribute)).andReturn(1);
      EasyMock.expect(this.server.getAttribute(this.name2, this.attribute)).andReturn(2);
      
      EasyMock.replay(this.server);
      
      double load = this.metric.getLoad(this.context);
      
      EasyMock.verify(this.server);
      
      Assert.assertEquals(3.0, load, 0.0);
      
      EasyMock.reset(this.server);
   }
}
