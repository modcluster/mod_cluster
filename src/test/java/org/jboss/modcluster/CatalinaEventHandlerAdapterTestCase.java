/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.modcluster;

import org.apache.catalina.Container;
import org.apache.catalina.ContainerEvent;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.easymock.EasyMock;
import org.junit.Test;

import java.beans.PropertyChangeEvent;

/**
 * @author Paul Ferraro
 *
 */
public class CatalinaEventHandlerAdapterTestCase
{
   @SuppressWarnings("unchecked")
   private ContainerEventHandler<Server, Engine, Context> eventHandler = EasyMock.createStrictMock(ContainerEventHandler.class);
   
   private CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler);

   @Test
   public void deployWebApp()
   {
      Host host = EasyMock.createStrictMock(Host.class);
      LifecycleContext context = EasyMock.createStrictMock(LifecycleContext.class);

      ContainerEvent event = new ContainerEvent(host, Container.ADD_CHILD_EVENT, context);
      
      context.addLifecycleListener(this.adapter);
      context.addPropertyChangeListener(this.adapter);
      this.eventHandler.addContext(context);
      
      EasyMock.replay(this.eventHandler, host, context);
      
      this.adapter.containerEvent(event);
      
      EasyMock.verify(this.eventHandler, host, context);
      EasyMock.reset(this.eventHandler, host, context);
   }

   @Test
   public void deployHost()
   {
      Engine engine = EasyMock.createStrictMock(Engine.class);

      ContainerEvent event = new ContainerEvent(engine, Container.ADD_CHILD_EVENT, null);

      engine.addContainerListener(this.adapter);
      
      EasyMock.replay(engine);
      
      this.adapter.containerEvent(event);
      
      EasyMock.verify(engine);
      EasyMock.reset(engine);
   }

   @Test
   public void undeployWebApp()
   {
      Host host = EasyMock.createStrictMock(Host.class);
      LifecycleContext context = EasyMock.createStrictMock(LifecycleContext.class);

      ContainerEvent event = new ContainerEvent(host, Container.REMOVE_CHILD_EVENT, context);
      
      context.removeLifecycleListener(this.adapter);
      context.removePropertyChangeListener(this.adapter);
      this.eventHandler.removeContext(context);
      
      EasyMock.replay(this.eventHandler, host, context);
      
      this.adapter.containerEvent(event);
      
      EasyMock.verify(this.eventHandler, host, context);
      EasyMock.reset(this.eventHandler, host, context);
   }

   @Test
   public void undeployHost()
   {
      Engine engine = EasyMock.createStrictMock(Engine.class);

      ContainerEvent event = new ContainerEvent(engine, Container.REMOVE_CHILD_EVENT, null);

      engine.removeContainerListener(this.adapter);
      
      EasyMock.replay(engine);
      
      this.adapter.containerEvent(event);
      
      EasyMock.verify(engine);
      EasyMock.reset(engine);
   }
   
   @Test
   public void startWebApp()
   {
      LifecycleContext context = EasyMock.createStrictMock(LifecycleContext.class);
      
      LifecycleEvent event = new LifecycleEvent(context, Lifecycle.START_EVENT);
      PropertyChangeEvent prop = new PropertyChangeEvent(context, "available", Boolean.FALSE, Boolean.TRUE);
      
      this.eventHandler.startContext(context);
      
      EasyMock.replay(this.eventHandler);
      
      // this.adapter.lifecycleEvent(event);
      this.adapter.propertyChange(prop);
      
      EasyMock.verify(this.eventHandler);
      EasyMock.reset(this.eventHandler);
   }
   
   @Test
   public void startServer()
   {
      LifecycleServer server = EasyMock.createStrictMock(LifecycleServer.class);
      Service service = EasyMock.createStrictMock(Service.class);
      LifecycleEngine engine = EasyMock.createStrictMock(LifecycleEngine.class);
      Container container = EasyMock.createStrictMock(Container.class);
      LifecycleContainer childContainer = EasyMock.createStrictMock(LifecycleContainer.class);
      
      LifecycleEvent event = new LifecycleEvent(server, Lifecycle.AFTER_START_EVENT);
      
      this.eventHandler.init(server);
      
      EasyMock.expect(server.findServices()).andReturn(new Service[] { service });
      EasyMock.expect(service.getContainer()).andReturn(engine);
      
      engine.addContainerListener(this.adapter);
      engine.addLifecycleListener(this.adapter);
      
      EasyMock.expect(engine.findChildren()).andReturn(new Container[] { container });
      
      container.addContainerListener(this.adapter);
      
      EasyMock.expect(container.findChildren()).andReturn(new Container[] { childContainer });
      
      childContainer.addLifecycleListener(this.adapter);
      childContainer.addPropertyChangeListener(this.adapter);
      
      this.eventHandler.startServer(server);
      
      EasyMock.replay(this.eventHandler, server, service, engine, container, childContainer);
      
      this.adapter.lifecycleEvent(event);
      
      EasyMock.verify(this.eventHandler, server, service, engine, container, childContainer);
      EasyMock.reset(this.eventHandler, server, service, engine, container, childContainer);
   }
   
   @Test
   public void stopWebApp()
   {
      LifecycleContext context = EasyMock.createStrictMock(LifecycleContext.class);
      
      LifecycleEvent event = new LifecycleEvent(context, Lifecycle.BEFORE_STOP_EVENT);
      
      this.eventHandler.stopContext(context);
      
      EasyMock.replay(this.eventHandler);
      
      this.adapter.lifecycleEvent(event);
      
      EasyMock.verify(this.eventHandler);
      EasyMock.reset(this.eventHandler);
   }
   
   @Test
   public void stopServer()
   {
      LifecycleServer server = EasyMock.createStrictMock(LifecycleServer.class);
      Service service = EasyMock.createStrictMock(Service.class);
      LifecycleEngine engine = EasyMock.createStrictMock(LifecycleEngine.class);
      Container container = EasyMock.createStrictMock(Container.class);
      LifecycleContainer childContainer = EasyMock.createStrictMock(LifecycleContainer.class);
      
      LifecycleEvent event = new LifecycleEvent(server, Lifecycle.BEFORE_STOP_EVENT);
      
      EasyMock.expect(server.findServices()).andReturn(new Service[] { service });
      EasyMock.expect(service.getContainer()).andReturn(engine);
      
      engine.removeContainerListener(this.adapter);
      engine.removeLifecycleListener(this.adapter);
      
      EasyMock.expect(engine.findChildren()).andReturn(new Container[] { container });
      
      container.removeContainerListener(this.adapter);
      
      EasyMock.expect(container.findChildren()).andReturn(new Container[] { childContainer });
      
      childContainer.removeLifecycleListener(this.adapter);
      childContainer.removePropertyChangeListener(this.adapter);
      
      this.eventHandler.stopServer(server);
      this.eventHandler.shutdown();
      
      EasyMock.replay(this.eventHandler, server, service, engine, container, childContainer);
      
      this.adapter.lifecycleEvent(event);
      
      EasyMock.verify(this.eventHandler, server, service, engine, container, childContainer);
      EasyMock.reset(this.eventHandler, server, service, engine, container, childContainer);
   }
   
   @Test
   public void periodicEvent()
   {
      LifecycleEngine engine = EasyMock.createStrictMock(LifecycleEngine.class);
      
      LifecycleEvent event = new LifecycleEvent(engine, Lifecycle.PERIODIC_EVENT);
      
      // Test uninitialized
      EasyMock.replay(this.eventHandler);
      
      this.adapter.lifecycleEvent(event);
      
      EasyMock.verify(this.eventHandler);
      EasyMock.reset(this.eventHandler);
      
      // Init
      this.startServer();
      
      // Test initialized
      this.eventHandler.status(engine);
      
      EasyMock.replay(this.eventHandler);
      
      this.adapter.lifecycleEvent(event);
      
      EasyMock.verify(this.eventHandler);
      EasyMock.reset(this.eventHandler);
   }
   
   interface LifecycleContext extends Lifecycle, Context
   {
   }
   
   interface LifecycleServer extends Lifecycle, Server
   {
   }
   
   interface LifecycleEngine extends Lifecycle, Engine
   {
   }
   
   interface LifecycleContainer extends Lifecycle, Container
   {
   }
}
