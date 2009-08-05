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
import javax.management.ObjectName;

import org.easymock.EasyMock;
import org.jboss.modcluster.load.metric.impl.MBeanLoadContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
public class MBeanLoadContextTestCase
{
   private MBeanServer server = EasyMock.createStrictMock(MBeanServer.class);
   private ObjectName name;
   private MBeanLoadContext context;
   
   @Before
   public void construct() throws MalformedObjectNameException
   {
      this.name = ObjectName.getInstance("domain:name=test");
      
      this.context = new MBeanLoadContext(this.server, this.name);
   }
   
   @Test
   public void getAttribute() throws Exception
   {
      String attribute = "attribute";
      String expected = "value";
      
      EasyMock.expect(this.server.getAttribute(this.name, attribute)).andReturn(expected);

      EasyMock.replay(this.server);
      
      String value = this.context.getAttribute(attribute, String.class);
      
      EasyMock.verify(this.server);
      
      Assert.assertSame(expected, value);
      
      EasyMock.reset(this.server);
   }
   
   @Test
   public void close()
   {
      EasyMock.replay(this.server);
      
      this.context.close();
      
      EasyMock.verify(this.server);
      EasyMock.reset(this.server);      
   }
}
