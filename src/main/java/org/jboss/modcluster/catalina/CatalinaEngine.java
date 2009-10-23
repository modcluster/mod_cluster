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

import org.apache.catalina.Container;
import org.apache.coyote.ProtocolHandler;
import org.apache.tomcat.util.IntrospectionUtils;
import org.jboss.modcluster.Connector;
import org.jboss.modcluster.Engine;
import org.jboss.modcluster.Host;

/**
 * @author Paul Ferraro
 */
public class CatalinaEngine implements Engine
{
   private final org.apache.catalina.Engine engine;
   
   public CatalinaEngine(org.apache.catalina.Engine engine)
   {
      this.engine = engine;
   }
   
   @Override
   public Iterable<Host> getHosts()
   {
      final Iterator<Container> children = Arrays.asList(this.engine.findChildren()).iterator();
      
      final Iterator<Host> hosts = new Iterator<Host>()
      {
         @Override
         public boolean hasNext()
         {
            return children.hasNext();
         }

         @Override
         public Host next()
         {
            return new CatalinaHost((org.apache.catalina.Host) children.next(), CatalinaEngine.this);
         }

         @Override
         public void remove()
         {
            children.remove();
         }
      };
      
      return new Iterable<Host>()
      {
         @Override
         public Iterator<Host> iterator()
         {
            return hosts;
         }
      };
   }

   @Override
   public String getJvmRoute()
   {
      return this.engine.getJvmRoute();
   }

   @Override
   public void setJvmRoute(String jvmRoute)
   {
      this.engine.setJvmRoute(jvmRoute);
   }

   @Override
   public String getName()
   {
      return this.engine.getName();
   }

   @Override
   public Connector getProxyConnector()
   {
      org.apache.catalina.connector.Connector[] connectors = this.engine.getService().findConnectors();
      
      int highestMaxThreads = 0;
      Connector bestConnector = null;
      
      for (org.apache.catalina.connector.Connector connector: connectors)
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

   @Override
   public Host findHost(String name)
   {
      org.apache.catalina.Host host = (org.apache.catalina.Host) this.engine.findChild(name);
      
      return (host != null) ? new CatalinaHost(host, this) : null;
   }

   @Override
   public String toString()
   {
      return this.engine.getName();
   }
}
