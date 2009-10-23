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

import java.util.Collections;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.QueryExp;

import junit.framework.Assert;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.jboss.modcluster.load.metric.impl.MBeanQueryLoadContext;
import org.jboss.modcluster.load.metric.impl.MBeanQueryLoadMetricSource;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
public class MBeanQueryLoadMetricSourceTestCase
{
   private MBeanServer server;
   private String pattern;
   private LoadMetricSource<MBeanQueryLoadContext> source;

   public MBeanQueryLoadMetricSourceTestCase() throws MalformedObjectNameException
   {
      this("domain:*", EasyMock.createStrictMock(MBeanServer.class));
   }
   
   private MBeanQueryLoadMetricSourceTestCase(String pattern, MBeanServer server) throws MalformedObjectNameException
   {
      this(new MBeanQueryLoadMetricSource(pattern, server), pattern, server);
   }
   
   protected MBeanQueryLoadMetricSourceTestCase(MBeanQueryLoadMetricSource source, String pattern, MBeanServer server)
   {
      this.source = source;
      this.pattern = pattern;
      this.server = server;
   }
   
   @Test
   public void createContext()
   {
      Capture<ObjectName> capturedName = new Capture<ObjectName>();
      
      EasyMock.expect(this.server.queryNames(EasyMock.capture(capturedName), (QueryExp) EasyMock.isNull())).andReturn(Collections.<ObjectName>emptySet());
      
      EasyMock.replay(this.server);
      
      MBeanQueryLoadContext context = this.source.createContext();
      
      EasyMock.verify(this.server);
      
      Assert.assertNotNull(context);
      
      Assert.assertEquals(this.pattern, capturedName.getValue().toString());
      
      EasyMock.reset(this.server);
   }
}
