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

import org.apache.catalina.Container;
import org.apache.catalina.ContainerEvent;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.jboss.modcluster.ContainerEventHandler;

/**
 * Adapts lifecycle and container listener events to the {@link ContainerEventHandler} interface.
 */
public class CatalinaEventHandlerAdapter implements LifecycleListener, ContainerListener
{
   /** Initialization flag. */
   private volatile boolean init = false;

   private ContainerEventHandler eventHandler;

   // ----------------------------------------------------------- Constructors

   /**
    * Constructs a new CatalinaEventHandlerAdapter using the specified event handler.
    * @param eventHandler an event hadnler
    */
   public CatalinaEventHandlerAdapter(ContainerEventHandler eventHandler)
   {
      this.eventHandler = eventHandler;
   }

   // ------------------------------------------------------------- Properties

   // ---------------------------------------------- LifecycleListener Methods

   /**
    * {@inhericDoc}
    * Acknowledge the occurrence of the specified event.
    * Note: Will never be called when the listener is associated to a Server,
    * since it is not a Container.
    * @see org.apache.catalina.ContainerListener#containerEvent(org.apache.catalina.ContainerEvent)
    */
   public void containerEvent(ContainerEvent event)
   {
      Container container = event.getContainer();
      Object child = event.getData();
      String type = event.getType();

      if (type.equals(Container.ADD_CHILD_EVENT))
      {
         if (container instanceof Host)
         {
            // Deploying a webapp
            ((Lifecycle) child).addLifecycleListener(this);
            this.eventHandler.add(new CatalinaContext((Context) child));
         }
         else if (container instanceof Engine)
         {
            // Deploying a host
            container.addContainerListener(this);
            Host host = (Host) child;
            if (host != null)
               host.addContainerListener(this);
         }
      }
      else if (type.equals(Container.REMOVE_CHILD_EVENT))
      {
         if (container instanceof Host)
         {
            // Undeploying a webapp
            ((Lifecycle) child).removeLifecycleListener(this);
            this.eventHandler.remove(new CatalinaContext((Context) child));
         }
         else if (container instanceof Engine)
         {
            // Undeploying a host
            Host host = (Host) child;
            if (host != null)
               host.removeContainerListener(this);
            container.removeContainerListener(this);
         }
      }
   }

   /**
    * {@inhericDoc}
    * Primary entry point for startup and shutdown events.
    * @see org.apache.catalina.LifecycleListener#lifecycleEvent(org.apache.catalina.LifecycleEvent)
    */
   public void lifecycleEvent(LifecycleEvent event)
   {
      Lifecycle source = event.getLifecycle();
      String type = event.getType();
      
      if (type.equals(Lifecycle.START_EVENT))
      {
         if (source instanceof Context)
         {
            // Start a webapp
            this.eventHandler.start(new CatalinaContext((Context) source));
         }
      }
      else if (type.equals(Lifecycle.AFTER_START_EVENT))
      {
         if (source instanceof Server)
         {
            Server server = (Server) source;
            CatalinaServer catalinaServer = new CatalinaServer(server);
            
            this.eventHandler.init(catalinaServer);
            
            this.addListeners(server);

            this.eventHandler.start(catalinaServer);

            this.init = true;
         }
      }
      else if (type.equals(Lifecycle.BEFORE_STOP_EVENT))
      {
         if (source instanceof Context)
         {
            // Stop a webapp
            this.eventHandler.stop(new CatalinaContext((Context) source));
         }
         else if (source instanceof Server)
         {
            this.init = false;

            Server server = (Server) source;
            
            this.removeListeners(server);

            this.eventHandler.stop(new CatalinaServer(server));

            this.eventHandler.shutdown();
         }
      }
      else if (type.equals(Lifecycle.PERIODIC_EVENT))
      {
         if (this.init && (source instanceof Engine))
         {
            this.eventHandler.status(new CatalinaEngine((Engine) source));
         }
      }
   }

   private void addListeners(Server server)
   {
      // Register ourself as a listener for child services
      for (Service service: server.findServices())
      {
         Container engine = service.getContainer();
         engine.addContainerListener(this);
         ((Lifecycle) engine).addLifecycleListener(this);

         for (Container host: engine.findChildren())
         {
            host.addContainerListener(this);
            
            for (Container context: host.findChildren())
            {
               ((Lifecycle) context).addLifecycleListener(this);
            }
         }
      }
   }

   private void removeListeners(Server server)
   {
      // Unregister ourself as a listener to child components
      for (Service service: server.findServices())
      {
         Container engine = service.getContainer();
         engine.removeContainerListener(this);
         ((Lifecycle) engine).removeLifecycleListener(this);
         
         for (Container host: engine.findChildren())
         {
            host.removeContainerListener(this);
            
            for (Container context: host.findChildren())
            {
               ((Lifecycle) context).removeLifecycleListener(this);
            }
         }
      }
   }
}
