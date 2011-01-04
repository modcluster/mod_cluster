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

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionListener;

import junit.framework.Assert;

import org.apache.catalina.Manager;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
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
      EasyMock.expect(this.context.isStarted()).andReturn(false);
      
      EasyMock.replay(this.context);
      
      boolean result = this.catalinaContext.isStarted();
      
      EasyMock.verify(this.context);
      
      Assert.assertFalse(result);
      
      EasyMock.reset(this.context);
      
      EasyMock.expect(this.context.isStarted()).andReturn(true);
      EasyMock.expect(this.context.getAvailable()).andReturn(false);
      
      EasyMock.replay(this.context);
      
      result = this.catalinaContext.isStarted();
      
      EasyMock.verify(this.context);
      
      Assert.assertFalse(result);
      
      EasyMock.reset(this.context);
      
      EasyMock.expect(this.context.isStarted()).andReturn(true);
      EasyMock.expect(this.context.getAvailable()).andReturn(true);
      
      EasyMock.replay(this.context);
      
      result = this.catalinaContext.isStarted();
      
      EasyMock.verify(this.context);
      
      Assert.assertTrue(result);
      
      EasyMock.reset(this.context);
   }
   
   @Test
   public void requestListener() throws IOException, ServletException
   {
      // Test addRequestListener()
      ServletRequestListener listener = EasyMock.createStrictMock(ServletRequestListener.class);
      Pipeline pipeline = EasyMock.createStrictMock(Pipeline.class);
      Capture<Valve> capturedValve = new Capture<Valve>();
      
      EasyMock.expect(this.context.getPipeline()).andReturn(pipeline);
      pipeline.addValve(EasyMock.capture(capturedValve));
      
      EasyMock.replay(this.context, this.host, pipeline, listener);
      
      this.catalinaContext.addRequestListener(listener);
      
      EasyMock.verify(this.context, this.host, pipeline, listener);
      
      Valve valve = capturedValve.getValue();
      
      EasyMock.reset(this.context, this.host, pipeline, listener);

      // Validate RequestListenerValve
      ServletContext servletContext = EasyMock.createStrictMock(ServletContext.class);
      Valve nextValve = EasyMock.createStrictMock(Valve.class);
      Capture<ServletRequestEvent> capturedInitializedEvent = new Capture<ServletRequestEvent>();
      Capture<ServletRequestEvent> capturedDestroyedEvent = new Capture<ServletRequestEvent>();
      Request request = new Request();
      request.setContext(this.context);
      Response response = new Response();
      valve.setNext(nextValve);
      
      EasyMock.expect(this.context.getServletContext()).andReturn(servletContext);
      listener.requestInitialized(EasyMock.capture(capturedInitializedEvent));
      
      nextValve.invoke(EasyMock.same(request), EasyMock.same(response));
      
      listener.requestDestroyed(EasyMock.capture(capturedDestroyedEvent));
      
      EasyMock.replay(this.context, listener);
      
      valve.invoke(request, response);
      
      EasyMock.verify(this.context, listener);
      
      ServletRequestEvent event = capturedInitializedEvent.getValue();
      
      Assert.assertSame(servletContext, event.getServletContext());
      Assert.assertSame(request, event.getServletRequest());
      Assert.assertSame(event, capturedDestroyedEvent.getValue());
      
      EasyMock.reset(this.context, listener);

      // Test removeRequestListener()
      EasyMock.expect(this.context.getPipeline()).andReturn(pipeline);
      EasyMock.expect(pipeline.getValves()).andReturn(new Valve[] { valve });
      pipeline.removeValve(valve);
      
      EasyMock.replay(this.context, this.host, pipeline, listener);
      
      this.catalinaContext.removeRequestListener(listener);
      
      EasyMock.verify(this.context, this.host, pipeline, listener);
      EasyMock.reset(this.context, this.host, pipeline, listener);
   }
   
   @Test
   public void getActiveSessionCount()
   {
      Manager manager = EasyMock.createStrictMock(Manager.class);
      
      EasyMock.expect(this.context.getManager()).andReturn(manager);
      EasyMock.expect(manager.getActiveSessions()).andReturn(10);
      
      EasyMock.replay(this.context, this.host, manager);
      
      int result = this.catalinaContext.getActiveSessionCount();
      
      EasyMock.verify(this.context, this.host, manager);
      
      Assert.assertEquals(10, result);
      
      EasyMock.reset(this.context, this.host, manager);
   }
   
   @Test
   public void addSessionListener()
   {
      HttpSessionListener listener = EasyMock.createStrictMock(HttpSessionListener.class);
      Capture<Object[]> capturedListeners = new Capture<Object[]>();
      Object otherListener = new Object();
      
      EasyMock.expect(this.context.getApplicationLifecycleListeners()).andReturn(new Object[] { otherListener });
      this.context.setApplicationLifecycleListeners(EasyMock.capture(capturedListeners));
      
      EasyMock.replay(this.context, this.host);
      
      this.catalinaContext.addSessionListener(listener);
      
      EasyMock.verify(this.context, this.host);
      
      Object[] listeners = capturedListeners.getValue();
      
      Assert.assertEquals(2, listeners.length);
      Assert.assertSame(listener, listeners[0]);
      Assert.assertSame(otherListener, listeners[1]);
      
      EasyMock.reset(this.context, this.host);
   }
   
   @Test
   public void removeSessionListener()
   {
      HttpSessionListener listener = EasyMock.createStrictMock(HttpSessionListener.class);
      Capture<Object[]> capturedListeners = new Capture<Object[]>();
      Object otherListener = new Object();
      
      EasyMock.expect(this.context.getApplicationLifecycleListeners()).andReturn(new Object[] { otherListener, listener });
      this.context.setApplicationLifecycleListeners(EasyMock.capture(capturedListeners));
      
      EasyMock.replay(this.context, this.host);
      
      this.catalinaContext.removeSessionListener(listener);
      
      EasyMock.verify(this.context, this.host);
      
      Object[] listeners = capturedListeners.getValue();
      
      Assert.assertEquals(1, listeners.length);
      Assert.assertSame(otherListener, listeners[0]);
      
      EasyMock.reset(this.context, this.host);
   }

   @Test
   public void isDistributable()
   {
      Manager manager = EasyMock.createStrictMock(Manager.class);
      
      EasyMock.expect(this.context.getManager()).andReturn(manager);
      EasyMock.expect(manager.getDistributable()).andReturn(true);
      
      EasyMock.replay(this.context, this.host, manager);
      
      boolean result = this.catalinaContext.isDistributable();
      
      EasyMock.verify(this.context, this.host, manager);
      
      Assert.assertTrue(result);
      
      EasyMock.reset(this.context, this.host, manager);
   }
}
