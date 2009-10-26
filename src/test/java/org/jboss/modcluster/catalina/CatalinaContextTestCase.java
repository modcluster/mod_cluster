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

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import junit.framework.Assert;

import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.SessionEvent;
import org.apache.catalina.SessionListener;
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
      Manager manager = EasyMock.createStrictMock(Manager.class);
      CatalinaSession session = EasyMock.createStrictMock(CatalinaSession.class);
      Capture<SessionListener> capturedListener = new Capture<SessionListener>();
      
      EasyMock.expect(this.context.getManager()).andReturn(manager);
      EasyMock.expect(manager.findSessions()).andReturn(new Session[] { session });
      session.addSessionListener(EasyMock.capture(capturedListener));
      
      EasyMock.replay(this.context, manager, session);
      
      this.catalinaContext.addSessionListener(listener);
      
      EasyMock.verify(this.context, manager, session);
      
      SessionListener sessionListener = capturedListener.getValue();
      
      EasyMock.reset(this.context, manager, session);

      Capture<HttpSessionEvent> capturedEvent = new Capture<HttpSessionEvent>();
      
      listener.sessionCreated(EasyMock.capture(capturedEvent));
      
      EasyMock.replay(listener);
      
      sessionListener.sessionEvent(new SessionEvent(session, Session.SESSION_CREATED_EVENT, null));
      
      EasyMock.verify(listener);
      
      Assert.assertSame(session, capturedEvent.getValue().getSession());
      
      EasyMock.reset(listener);
      capturedEvent.reset();
      
      listener.sessionDestroyed(EasyMock.capture(capturedEvent));
      
      EasyMock.replay(listener);
      
      sessionListener.sessionEvent(new SessionEvent(session, Session.SESSION_DESTROYED_EVENT, null));
      
      EasyMock.verify(listener);
      
      Assert.assertSame(session, capturedEvent.getValue().getSession());
      
      EasyMock.reset(listener);
   }
   
   @Test
   public void removeSessionListener()
   {
      HttpSessionListener listener = EasyMock.createStrictMock(HttpSessionListener.class);
      Manager manager = EasyMock.createStrictMock(Manager.class);
      CatalinaSession session = EasyMock.createStrictMock(CatalinaSession.class);
      Capture<SessionListener> capturedListener = new Capture<SessionListener>();
      
      EasyMock.expect(this.context.getManager()).andReturn(manager);
      EasyMock.expect(manager.findSessions()).andReturn(new Session[] { session });
      session.removeSessionListener(EasyMock.capture(capturedListener));
      
      EasyMock.replay(this.context, manager, session);
      
      this.catalinaContext.removeSessionListener(listener);
      
      EasyMock.verify(this.context, manager, session);
      
      SessionListener sessionListener = capturedListener.getValue();
      
      EasyMock.reset(this.context, manager, session);

      Capture<HttpSessionEvent> capturedEvent = new Capture<HttpSessionEvent>();
      
      listener.sessionCreated(EasyMock.capture(capturedEvent));
      
      EasyMock.replay(listener);
      
      sessionListener.sessionEvent(new SessionEvent(session, Session.SESSION_CREATED_EVENT, null));
      
      EasyMock.verify(listener);
      
      Assert.assertSame(session, capturedEvent.getValue().getSession());
      
      EasyMock.reset(listener);
      capturedEvent.reset();
      
      listener.sessionDestroyed(EasyMock.capture(capturedEvent));
      
      EasyMock.replay(listener);
      
      sessionListener.sessionEvent(new SessionEvent(session, Session.SESSION_DESTROYED_EVENT, null));
      
      EasyMock.verify(listener);
      
      Assert.assertSame(session, capturedEvent.getValue().getSession());
      
      EasyMock.reset(listener);
   }
   
   interface CatalinaSession extends Session, HttpSession
   {
      
   }
}
