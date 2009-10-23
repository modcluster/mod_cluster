/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.jboss.modcluster.Context;
import org.jboss.modcluster.Host;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
public class CatalinaContextTestCase
{
   private Host host = EasyMock.createStrictMock(Host.class);
   private org.apache.catalina.Context context = EasyMock.createStrictMock(org.apache.catalina.Context.class);

   private Context catalinaContext = new CatalinaContext(this.context, this.host);
   
   @Test
   public void getHost()
   {
      Assert.assertSame(this.host, this.catalinaContext.getHost());
   }

   @Test
   public void getPath()
   {
      String expected = "path";
      
      EasyMock.expect(this.context.getPath()).andReturn(expected);

      EasyMock.replay(this.context);
      
      String result = this.catalinaContext.getPath();
      
      EasyMock.verify(this.context);
      
      Assert.assertSame(expected, result);
      
      EasyMock.reset(this.context);
   }

   @Test
   public void isStarted()
   {
      EasyMock.expect(this.context.isStarted()).andReturn(true);
      
      EasyMock.replay(this.context);
      
      boolean result = this.catalinaContext.isStarted();
      
      EasyMock.verify(this.context);
      
      Assert.assertTrue(result);
      
      EasyMock.reset(this.context);
   }
}
