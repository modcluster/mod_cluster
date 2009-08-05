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

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.jboss.modcluster.load.metric.impl.MBeanLoadContext;
import org.jboss.modcluster.load.metric.impl.MBeanLoadMetricSource;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
public class MBeanLoadMetricSourceTestCase
{
   private MBeanServer server;
   private LoadMetricSource<MBeanLoadContext> source;

   public MBeanLoadMetricSourceTestCase() throws MalformedObjectNameException
   {
      this("domain:name=test", EasyMock.createStrictMock(MBeanServer.class));
   }
   
   private MBeanLoadMetricSourceTestCase(String name, MBeanServer server) throws MalformedObjectNameException
   {
      this(new MBeanLoadMetricSource(name, server), server);
   }
   
   protected MBeanLoadMetricSourceTestCase(MBeanLoadMetricSource source, MBeanServer server)
   {
      this.source = source;
      this.server = server;
   }
   
   @Test
   public void createContext()
   {
      EasyMock.replay(this.server);
      
      MBeanLoadContext context = this.source.createContext();
      
      EasyMock.verify(this.server);
      
      Assert.assertNotNull(context);
      
      EasyMock.reset(this.server);
   }
}
