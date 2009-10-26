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

import javax.servlet.http.HttpSessionListener;

import junit.framework.Assert;

import org.apache.catalina.Manager;
import org.easymock.Capture;
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
   
   @Test
   public void getActiveSessionCount()
   {
      Manager manager = EasyMock.createStrictMock(Manager.class);
      
      EasyMock.expect(this.context.getManager()).andReturn(manager);
      EasyMock.expect(manager.getActiveSessions()).andReturn(10);
      
      EasyMock.replay(this.context, manager);
      
      int result = this.catalinaContext.getActiveSessionCount();
      
      EasyMock.verify(this.context, manager);
      
      Assert.assertEquals(10, result);
      
      EasyMock.reset(this.context);
   }
   
   @Test
   public void addSessionListener()
   {
      HttpSessionListener listener = EasyMock.createStrictMock(HttpSessionListener.class);
      Capture<Object[]> capturedListeners = new Capture<Object[]>();
      Object otherListener = new Object();
      
      EasyMock.expect(this.context.getApplicationLifecycleListeners()).andReturn(new Object[] { otherListener });
      this.context.setApplicationLifecycleListeners(EasyMock.capture(capturedListeners));
      
      EasyMock.replay(this.context);
      
      this.catalinaContext.addSessionListener(listener);
      
      EasyMock.verify(this.context);
      
      Object[] listeners = capturedListeners.getValue();
      
      Assert.assertEquals(2, listeners.length);
      Assert.assertSame(listener, listeners[0]);
      Assert.assertSame(otherListener, listeners[1]);
      
      EasyMock.reset(this.context);
   }
   
   @Test
   public void removeSessionListener()
   {
      HttpSessionListener listener = EasyMock.createStrictMock(HttpSessionListener.class);
      Capture<Object[]> capturedListeners = new Capture<Object[]>();
      Object otherListener = new Object();
      
      EasyMock.expect(this.context.getApplicationLifecycleListeners()).andReturn(new Object[] { otherListener, listener });
      this.context.setApplicationLifecycleListeners(EasyMock.capture(capturedListeners));
      
      EasyMock.replay(this.context);
      
      this.catalinaContext.removeSessionListener(listener);
      
      EasyMock.verify(this.context);
      
      Object[] listeners = capturedListeners.getValue();
      
      Assert.assertEquals(1, listeners.length);
      Assert.assertSame(otherListener, listeners[0]);
      
      EasyMock.reset(this.context);
   }
}
