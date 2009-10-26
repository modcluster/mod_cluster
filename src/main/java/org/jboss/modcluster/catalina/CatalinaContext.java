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

import org.apache.catalina.Session;
import org.apache.catalina.SessionEvent;
import org.apache.catalina.SessionListener;
import org.jboss.modcluster.Context;
import org.jboss.modcluster.Host;

/**
 * @author Paul Ferraro
 */
public class CatalinaContext implements Context
{
   private final org.apache.catalina.Context context;
   private final Host host;
   
   public CatalinaContext(org.apache.catalina.Context context, Host host)
   {
      this.context = context;
      this.host = host;
   }
   
   public CatalinaContext(org.apache.catalina.Context context)
   {
      this.context = context;
      this.host = new CatalinaHost((org.apache.catalina.Host) context.getParent());
   }
   
   @Override
   public Host getHost()
   {
      return this.host;
   }

   @Override
   public String getPath()
   {
      return this.context.getPath();
   }

   @Override
   public boolean isStarted()
   {
      try
      {
         return this.context.isStarted();
      }
      catch (NoSuchMethodError e)
      {
         return true;
      }
   }
   
   @Override
   public int getActiveSessionCount()
   {
      return this.context.getManager().getActiveSessions();
   }

   @Override
   public void addSessionListener(final HttpSessionListener listener)
   {
      SessionListener listenerAdapter = new SessionListenerAdapter(listener);
      
      for (Session session: this.context.getManager().findSessions())
      {
         session.addSessionListener(listenerAdapter);
      }
   }

   @Override
   public void removeSessionListener(HttpSessionListener listener)
   {
      SessionListener listenerAdapter = new SessionListenerAdapter(listener);
      
      for (Session session: this.context.getManager().findSessions())
      {
         session.removeSessionListener(listenerAdapter);
      }
   }

   @Override
   public String toString()
   {
      return this.context.getPath();
   }
   
   private static class SessionListenerAdapter implements SessionListener
   {
      private final HttpSessionListener listener;
      
      SessionListenerAdapter(HttpSessionListener listener)
      {
         this.listener = listener;
      }
      
      @Override
      public void sessionEvent(SessionEvent event)
      {
         if (event.getType().equals(Session.SESSION_CREATED_EVENT))
         {
            this.listener.sessionCreated(this.adaptEvent(event));
         }
         else if (event.getType().equals(Session.SESSION_DESTROYED_EVENT))
         {
            this.listener.sessionDestroyed(this.adaptEvent(event));
         }
      }

      private HttpSessionEvent adaptEvent(SessionEvent event)
      {
         return new HttpSessionEvent((HttpSession) event.getSession());
      }
      
      public int hashCode()
      {
         return this.listener.hashCode();
      }
      
      public boolean equals(Object object)
      {
         if (this == object) return true;
         if ((object == null) || !(object instanceof SessionListenerAdapter)) return false;
         
         SessionListenerAdapter listener = (SessionListenerAdapter) object;
         
         return this.listener.equals(listener.listener);
      }
   }
}
