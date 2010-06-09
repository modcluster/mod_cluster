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

import java.util.Arrays;
import java.util.Iterator;

import javax.management.MBeanServer;

import org.apache.catalina.Container;
import org.apache.catalina.Globals;
import org.apache.coyote.ProtocolHandler;
import org.apache.jasper.Constants;
import org.apache.tomcat.util.IntrospectionUtils;
import org.jboss.modcluster.Connector;
import org.jboss.modcluster.Engine;
import org.jboss.modcluster.Host;
import org.jboss.modcluster.Server;

/**
 * {@link Engine} implementation that wraps a {@link org.apache.catalina.Context}.
 * @author Paul Ferraro
 */
public class CatalinaEngine implements Engine
{
   private final org.apache.catalina.Engine engine;
   private final Server server;
   
   /**
    * Constructs a new CatalinaEngine that wraps the specified catalina engine
    * @param engine a catalina engine
    */
   public CatalinaEngine(org.apache.catalina.Engine engine, Server server)
   {
      this.engine = engine;
      this.server = server;
   }
   
   /**
    * Constructs a new CatalinaEngine that wraps the specified catalina engine
    * @param engine a catalina engine
    */
   public CatalinaEngine(org.apache.catalina.Engine engine, MBeanServer mbeanServer)
   {
      this(engine, new CatalinaServer(engine.getService().getServer(), mbeanServer));
   }
   
   /**
    * {@inheritDoc}
    * @see org.jboss.modcluster.Engine#getServer()
    */
   public Server getServer()
   {
      return this.server;
   }

   /**
    * {@inhericDoc}
    * @see org.jboss.modcluster.Engine#getHosts()
    */
   public Iterable<Host> getHosts()
   {
      final Iterator<Container> children = Arrays.asList(this.engine.findChildren()).iterator();
      
      final Iterator<Host> hosts = new Iterator<Host>()
      {
         public boolean hasNext()
         {
            return children.hasNext();
         }

         public Host next()
         {
            return new CatalinaHost((org.apache.catalina.Host) children.next(), CatalinaEngine.this);
         }

         public void remove()
         {
            children.remove();
         }
      };
      
      return new Iterable<Host>()
      {
         public Iterator<Host> iterator()
         {
            return hosts;
         }
      };
   }

   /**
    * {@inhericDoc}
    * @see org.jboss.modcluster.Engine#getJvmRoute()
    */
   public String getJvmRoute()
   {
      return this.engine.getJvmRoute();
   }

   /**
    * {@inhericDoc}
    * @see org.jboss.modcluster.Engine#setJvmRoute(java.lang.String)
    */
   public void setJvmRoute(String jvmRoute)
   {
      this.engine.setJvmRoute(jvmRoute);
   }

   /**
    * {@inhericDoc}
    * @see org.jboss.modcluster.Engine#getName()
    */
   public String getName()
   {
      return this.engine.getName();
   }

   /**
    * {@inhericDoc}
    * @see org.jboss.modcluster.Engine#getProxyConnector()
    */
   public Connector getProxyConnector()
   {
      int highestMaxThreads = 0;
      Connector bestConnector = null;
      
      for (org.apache.catalina.connector.Connector connector: this.engine.getService().findConnectors())
      {
         CatalinaConnector catalinaConnector = new CatalinaConnector(connector);
         
         if (CatalinaConnector.isAJP(connector) || catalinaConnector.isReverse())
         {
            return catalinaConnector;
         }
         
         ProtocolHandler handler = connector.getProtocolHandler();
         
         int maxThreads = ((Integer) IntrospectionUtils.getProperty(handler, "maxThreads")).intValue();
         
         if (maxThreads > highestMaxThreads)
         {
            highestMaxThreads = maxThreads;
            bestConnector = catalinaConnector;
         }
      }
      
      if (bestConnector == null)
      {
         throw new IllegalStateException();
      }

      return bestConnector;
   }

   /**
    * {@inhericDoc}
    * @see org.jboss.modcluster.Engine#findHost(java.lang.String)
    */
   public Host findHost(String name)
   {
      org.apache.catalina.Host host = (org.apache.catalina.Host) this.engine.findChild(name);
      
      return (host != null) ? new CatalinaHost(host, this) : null;
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.modcluster.Engine#getSessionCookieName()
    */
   public String getSessionCookieName()
   {
      try
      {
         return Globals.SESSION_COOKIE_NAME;
      }
      catch (NoSuchFieldError e)
      {
         // Not in Tomcat
         return "JSESSIONID";
      }
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.modcluster.Engine#getSessionParameterName()
    */
   public String getSessionParameterName()
   {
      try
      {
         return Globals.SESSION_PARAMETER_NAME;
      }
      catch (NoSuchFieldError e)
      {
         // Tomcat location is different
         return Constants.SESSION_PARAMETER_NAME;
      }
   }

   /**
    * {@inheritDoc}
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object object)
   {
      if ((object == null) || !(object instanceof CatalinaEngine)) return false;
      
      CatalinaEngine engine = (CatalinaEngine) object;
      
      return this.engine == engine.engine;
   }

   /**
    * {@inheritDoc}
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode()
   {
      return this.engine.hashCode();
   }

   /**
    * {@inhericDoc}
    * @see java.lang.Object#toString()
    */
   public String toString()
   {
      return this.engine.getName();
   }
}
