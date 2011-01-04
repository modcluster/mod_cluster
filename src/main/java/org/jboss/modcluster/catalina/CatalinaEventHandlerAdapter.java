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

import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

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
public class CatalinaEventHandlerAdapter implements LifecycleListener, ContainerListener, NotificationListener, PropertyChangeListener
{
   private volatile ObjectName serviceObjectName = toObjectName("jboss.web:service=WebServer");
   private volatile ObjectName serverObjectName = toObjectName("jboss.web:type=Server");
   private volatile String connectorsStartedNotificationType = "jboss.tomcat.connectors.started";
   private volatile String connectorsStoppedNotificationType = "jboss.tomcat.connectors.stopped";
   
   private final ContainerEventHandler eventHandler;
   private final MBeanServer mbeanServer;
   
   // Flags used to ignore redundant or invalid events
   private final AtomicBoolean init = new AtomicBoolean(false);
   private final AtomicBoolean start = new AtomicBoolean(false);
   
   // ----------------------------------------------------------- Constructors

   /**
    * Constructs a new CatalinaEventHandlerAdapter using the specified event handler.
    * @param eventHandler an event handler
    * @throws NullPointerException 
    * @throws MalformedObjectNameException 
    */
   public CatalinaEventHandlerAdapter(ContainerEventHandler eventHandler)
   {
      this(eventHandler, ManagementFactory.getPlatformMBeanServer());
   }
   
   public CatalinaEventHandlerAdapter(ContainerEventHandler eventHandler, MBeanServer mbeanServer)
   {
      this.eventHandler = eventHandler;
      this.mbeanServer = mbeanServer;
   }
   
   public void start() throws JMException
   {
      Server server = this.findServer();
      
      if (server != null)
      {
         if (!(server instanceof Lifecycle)) throw new IllegalStateException();

         Lifecycle lifecycle = (Lifecycle) server;
         
         if (!this.containsListener(lifecycle))
         {
            lifecycle.addLifecycleListener(this);
         }
         
         if (this.init.compareAndSet(false, true))
         {
            this.init(server);
         }
         
         if (this.start.compareAndSet(false, true))
         {
            this.eventHandler.start(new CatalinaServer(server, this.mbeanServer));
         }
      }
   }
   
   public void stop() throws JMException
   {
      Server server = this.findServer();
      
      if (server != null)
      {
         if (!(server instanceof Lifecycle)) throw new IllegalStateException();
         
         Lifecycle lifecycle = (Lifecycle) server;

         lifecycle.removeLifecycleListener(this);
         
         if (this.init.get() && this.start.compareAndSet(true, false))
         {
            this.eventHandler.stop(new CatalinaServer(server, this.mbeanServer));
         }
         
         if (this.init.compareAndSet(true, false))
         {
            this.destroy(server);
         }
      }
   }
   
   private boolean containsListener(Lifecycle lifecycle)
   {
      for (LifecycleListener listener: lifecycle.findLifecycleListeners())
      {
         if (listener.equals(this))
         {
            return true;
         }
      }
      
      return false;
   }
   
   private Server findServer() throws JMException
   {
      try
      {
         Service[] services = (Service[]) this.mbeanServer.invoke(this.serverObjectName, "findServices", null, null);
   
         return (services.length > 0) ? services[0].getServer() : null;
      }
      catch (InstanceNotFoundException e)
      {
         return null;
      }
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
            ((Container) child).addPropertyChangeListener(this);
            
            if (this.start.get())
            {
               this.eventHandler.add(new CatalinaContext((Context) child, this.mbeanServer));
            }
         }
         else if (container instanceof Engine)
         {
            // Deploying a host
            container.addContainerListener(this);
            
            if (child != null)
            {
               ((Host) child).addContainerListener(this);
            }
         }
      }
      else if (type.equals(Container.REMOVE_CHILD_EVENT))
      {
         if (container instanceof Host)
         {
            // Undeploying a webapp
            ((Lifecycle) child).removeLifecycleListener(this);
            ((Container) child).removePropertyChangeListener(this);
            
            if (this.start.get())
            {
               this.eventHandler.remove(new CatalinaContext((Context) child, this.mbeanServer));
            }
         }
         else if (container instanceof Engine)
         {
            // Undeploying a host
            if (child != null)
            {
               ((Host) child).removeContainerListener(this);
            }
            
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
      
      if (type.equals(Lifecycle.INIT_EVENT))
      {
         if (source instanceof Server)
         {
            if (this.init.compareAndSet(false, true))
            {
               Server server = (Server) source;

               init(server); 
            }
         }
      }
      else if (type.equals(Lifecycle.START_EVENT))
      {
         if (source instanceof Server)
         {
            if (this.init.compareAndSet(false, true))
            {
               Server server = (Server) source;

               init(server); 
            }
         }
      }
      else if (type.equals(Lifecycle.AFTER_START_EVENT))
      {
         if (source instanceof Server)
         {
            if (this.init.get() && this.start.compareAndSet(false, true))
            {
               this.eventHandler.start(new CatalinaServer((Server) source, this.mbeanServer));
            }
         }
      }
      else if (type.equals(Lifecycle.BEFORE_STOP_EVENT))
      {
         if (source instanceof Context)
         {
            if (this.start.get())
            {
               // Stop a webapp
               this.eventHandler.stop(new CatalinaContext((Context) source, this.mbeanServer));
            }
         }
         else if (source instanceof Server)
         {
            if (this.init.get() && this.start.compareAndSet(true, false))
            {
               this.eventHandler.stop(new CatalinaServer((Server) source, this.mbeanServer));
            }
         }
      }
      else if (type.equals(Lifecycle.DESTROY_EVENT))
      {
         if (source instanceof Server)
         {
            if (this.init.compareAndSet(true, false))
            {
               this.destroy((Server) source);
            }
         }
      }
      else if (type.equals(Lifecycle.PERIODIC_EVENT))
      {
         if (source instanceof Engine)
         {
            if (this.start.get())
            {
               this.eventHandler.status(new CatalinaEngine((Engine) source, this.mbeanServer));
            }
         }
      }
   }

   /**
    * initialize server stuff: in jbossweb-2.1.x the server can't be destroyed so
    * you could start (restart) one that needs initializations...
    */
   private void init(Server server)
   {
      this.eventHandler.init(new CatalinaServer(server, this.mbeanServer));
               
      this.addListeners(server);
               
      // Register for mbean notifications if JBoss Web server mbean exists
      if (this.mbeanServer.isRegistered(this.serviceObjectName))
      {
         try
         {
            this.mbeanServer.addNotificationListener(this.serviceObjectName, this, null, server);
         }
         catch (InstanceNotFoundException e)
         {
            throw new IllegalStateException(e);
         }
      }
   }
   
   private void destroy(Server server)
   {
      this.removeListeners(server);

      // Unregister for mbean notifications if JBoss Web server mbean exists
      if (this.mbeanServer.isRegistered(this.serviceObjectName))
      {
         try
         {
            this.mbeanServer.removeNotificationListener(this.serviceObjectName, this);
         }
         catch (InstanceNotFoundException e)
         {
            throw new IllegalStateException(e);
         }
         catch (ListenerNotFoundException e)
         {
            throw new IllegalStateException(e);
         }
      }
      
      this.eventHandler.shutdown();
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

   /**
    * {@inheritDoc}
    * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
    */
   public void handleNotification(Notification notification, Object object)
   {
      String type = notification.getType();
      
      if (type != null)
      {
         if (type.equals(this.connectorsStartedNotificationType))
         {
            // In JBoss AS, connectors are the last to start, so trigger a status event to reset the proxy
            if (this.start.get())
            {
               for (Service service: ((Server) object).findServices())
               {
                  this.eventHandler.status(new CatalinaEngine((Engine) service.getContainer(), this.mbeanServer));
               }
            }
         }
         else if (type.equals(this.connectorsStoppedNotificationType))
         {
            // In JBoss AS, connectors are the first to stop, so handle this notification as a server stop event.
            if (this.init.get() && this.start.compareAndSet(true, false))
            {
               this.eventHandler.stop(new CatalinaServer((Server) object, this.mbeanServer));
            }
         }
      }
   }

   /**
    * @param serviceObjectName the name to serverObjectName
    */
   public void setServiceObjectName(ObjectName serviceObjectName)
   {
      this.serviceObjectName = serviceObjectName;
   }

   /**
    * @param serverObjectName the serverObjectName to set
    */
   public void setServerObjectName(ObjectName serverObjectName)
   {
      this.serverObjectName = serverObjectName;
   }

   /**
    * @param notificationType the notificationType to set
    */
   public void setConnectorsStoppedNotificationType(String type)
   {
      this.connectorsStoppedNotificationType = type;
   }

   /**
    * @param connectorsStartedNotificationType the connectorsStartedNotificationType to set
    */
   public void setConnectorsStartedNotificationType(String type)
   {
      this.connectorsStartedNotificationType = type;
   }
   
   /**
    * {@inheritDoc}
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode()
   {
      // Hack to encourage this notification listener
      // to trigger *after* any listener with 0 hashCode.
      return 1;
   }

   private static ObjectName toObjectName(String name)
   {
      try
      {
         return ObjectName.getInstance(name);
      }
      catch (MalformedObjectNameException e)
      {
         throw new IllegalArgumentException(e);
      }
   }
   
   public void propertyChange(PropertyChangeEvent event) {
	   if (event.getSource() instanceof Context 
		   && "available".equals(event.getPropertyName())
		   && Boolean.FALSE.equals(event.getOldValue())
		   && Boolean.TRUE.equals(event.getNewValue())) {
		   this.eventHandler.start(new CatalinaContext((Context) event.getSource(), this.mbeanServer));
	   }
   }
}
