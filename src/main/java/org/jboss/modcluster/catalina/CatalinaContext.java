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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpSessionListener;

import org.jboss.modcluster.Context;
import org.jboss.modcluster.Host;

/**
 * {@link Context} implementation that wraps a {@link org.apache.catalina.Context}.
 * @author Paul Ferraro
 */
public class CatalinaContext implements Context
{
   private final org.apache.catalina.Context context;
   private final Host host;
   
   /**
    * Constructs a new CatalinaContext wrapping the specified context.
    * @param context the catalina context
    * @param host the parent container
    */
   public CatalinaContext(org.apache.catalina.Context context, Host host)
   {
      this.context = context;
      this.host = host;
   }
   
   /**
    * Constructs a new CatalinaContext wrapping the specified context.
    * @param context the catalina context
    */
   public CatalinaContext(org.apache.catalina.Context context)
   {
      this(context, new CatalinaHost((org.apache.catalina.Host) context.getParent()));
   }
   
   /**
    * {@inhericDoc}
    * @see org.jboss.modcluster.Context#getHost()
    */
   public Host getHost()
   {
      return this.host;
   }

   /**
    * {@inhericDoc}
    * @see org.jboss.modcluster.Context#getPath()
    */
   public String getPath()
   {
      return this.context.getPath();
   }

   /**
    * {@inhericDoc}
    * @see org.jboss.modcluster.Context#isStarted()
    */
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
   
   /**
    * {@inhericDoc}
    * @see org.jboss.modcluster.Context#getActiveSessionCount()
    */
   public int getActiveSessionCount()
   {
      return this.context.getManager().getActiveSessions();
   }

   /**
    * {@inhericDoc}
    * @see org.jboss.modcluster.Context#addSessionListener(javax.servlet.http.HttpSessionListener)
    */
   public void addSessionListener(final HttpSessionListener listener)
   {
      synchronized (this.context)
      {
         Object[] listeners = this.context.getApplicationLifecycleListeners();
         List<Object> listenerList = new ArrayList<Object>(listeners.length + 1);
         
         listenerList.add(listener);
         listenerList.addAll(Arrays.asList(listeners));
         
         this.context.setApplicationLifecycleListeners(listenerList.toArray());
      }
   }

   /**
    * {@inhericDoc}
    * @see org.jboss.modcluster.Context#removeSessionListener(javax.servlet.http.HttpSessionListener)
    */
   public void removeSessionListener(HttpSessionListener listener)
   {
      synchronized (this.context)
      {
         List<Object> listenerList = new ArrayList<Object>(Arrays.asList(this.context.getApplicationLifecycleListeners()));

         listenerList.remove(listener);
         
         this.context.setApplicationLifecycleListeners(listenerList.toArray());
      }
   }

   /**
    * {@inhericDoc}
    * @see java.lang.Object#toString()
    */
   public String toString()
   {
      return this.context.getPath();
   }
}
