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
package org.jboss.modcluster.catalina;

import java.util.List;

import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.ObjectName;

import junit.framework.Assert;

import org.apache.catalina.Container;
import org.apache.catalina.ContainerEvent;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.jboss.modcluster.ContainerEventHandler;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
public class CatalinaEventHandlerAdapterTestCase
{
   private ContainerEventHandler eventHandler = EasyMock.createStrictMock(ContainerEventHandler.class);
   private MBeanServer mbeanServer = EasyMock.createStrictMock(MBeanServer.class);

   @Test
   public void start() throws JMException
   {
      Service service = EasyMock.createStrictMock(Service.class);
      LifecycleServer server = EasyMock.createStrictMock(LifecycleServer.class);
      LifecycleListener listener = EasyMock.createStrictMock(LifecycleListener.class);
      LifecycleEngine engine = EasyMock.createStrictMock(LifecycleEngine.class);
      Container container = EasyMock.createStrictMock(Container.class);
      LifecycleContainer childContainer = EasyMock.createStrictMock(LifecycleContainer.class);
      Capture<ObjectName> capturedName = new Capture<ObjectName>(CaptureType.ALL);
      Capture<CatalinaServer> capturedServer = new Capture<CatalinaServer>(CaptureType.ALL);
      
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);

      EasyMock.expect(this.mbeanServer.invoke(EasyMock.capture(capturedName), EasyMock.eq("findServices"), EasyMock.<Object[]>isNull(), EasyMock.<String[]>isNull())).andReturn(new Service[] { service });
      EasyMock.expect(service.getServer()).andReturn(server);
      EasyMock.expect(server.findLifecycleListeners()).andReturn(new LifecycleListener[] { listener });
      server.addLifecycleListener(EasyMock.same(adapter));
      
      this.eventHandler.init(EasyMock.capture(capturedServer));
      
      EasyMock.expect(server.findServices()).andReturn(new Service[] { service });
      
      EasyMock.expect(service.getContainer()).andReturn(engine);
      
      engine.addContainerListener(adapter);
      engine.addLifecycleListener(adapter);
      
      EasyMock.expect(engine.findChildren()).andReturn(new Container[] { container });
      
      container.addContainerListener(adapter);
      
      EasyMock.expect(container.findChildren()).andReturn(new Container[] { childContainer });
      
      childContainer.addLifecycleListener(adapter);
      
      EasyMock.expect(this.mbeanServer.isRegistered(EasyMock.capture(capturedName))).andReturn(true);
      
      this.mbeanServer.addNotificationListener(EasyMock.capture(capturedName), EasyMock.same(adapter), EasyMock.<NotificationFilter>isNull(), EasyMock.same(server));

      this.eventHandler.start(EasyMock.capture(capturedServer));
      
      EasyMock.replay(this.eventHandler, this.mbeanServer, server, service, listener, engine, container, childContainer);
      
      adapter.start();
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, server, service, listener, engine, container, childContainer);
      
      List<ObjectName> names = capturedName.getValues();
      Assert.assertEquals(3, names.size());
      ObjectName name = names.get(0);
      Assert.assertEquals("jboss.web", name.getDomain());
      Assert.assertEquals(1, name.getKeyPropertyList().size());
      Assert.assertEquals("Server", name.getKeyProperty("type"));
      name = names.get(1);
      Assert.assertSame(name, names.get(2));
      Assert.assertEquals("jboss.web", name.getDomain());
      Assert.assertEquals(1, name.getKeyPropertyList().size());
      Assert.assertEquals("WebServer", name.getKeyProperty("service"));
      
      List<CatalinaServer> servers = capturedServer.getValues();
      Assert.assertSame(this.mbeanServer, servers.get(0).getMBeanServer());
      Assert.assertSame(server, servers.get(1).getServer());
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, server, service, listener, engine, container, childContainer);
   }

   @Test
   public void startNoServer() throws JMException
   {
      Capture<ObjectName> capturedName = new Capture<ObjectName>();
      
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);

      EasyMock.expect(this.mbeanServer.invoke(EasyMock.capture(capturedName), EasyMock.eq("findServices"), EasyMock.<Object[]>isNull(), EasyMock.<String[]>isNull())).andThrow(new InstanceNotFoundException());
      
      EasyMock.replay(this.eventHandler, this.mbeanServer);
      
      adapter.start();
      
      EasyMock.verify(this.eventHandler, this.mbeanServer);
      
      ObjectName name = capturedName.getValue();
      Assert.assertEquals("jboss.web", name.getDomain());
      Assert.assertEquals(1, name.getKeyPropertyList().size());
      Assert.assertEquals("Server", name.getKeyProperty("type"));
      
      EasyMock.verify(this.eventHandler, this.mbeanServer);
   }

   @Test
   public void startAlreadyRegistered() throws Exception
   {
      Service service = EasyMock.createStrictMock(Service.class);
      LifecycleServer server = EasyMock.createStrictMock(LifecycleServer.class);
      LifecycleListener listener = EasyMock.createStrictMock(LifecycleListener.class);
      Capture<ObjectName> capturedName = new Capture<ObjectName>();
      
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);

      this.initServer(adapter, server);
      this.startServer(adapter, server);
      
      EasyMock.expect(this.mbeanServer.invoke(EasyMock.capture(capturedName), EasyMock.eq("findServices"), EasyMock.<Object[]>isNull(), EasyMock.<String[]>isNull())).andReturn(new Service[] { service });
      EasyMock.expect(service.getServer()).andReturn(server);
      EasyMock.expect(server.findLifecycleListeners()).andReturn(new LifecycleListener[] { listener, adapter });
      
      EasyMock.replay(this.eventHandler, this.mbeanServer, server, service, listener);
      
      adapter.start();
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, server, service, listener);
      
      ObjectName name = capturedName.getValue();
      Assert.assertEquals("jboss.web", name.getDomain());
      Assert.assertEquals(1, name.getKeyPropertyList().size());
      Assert.assertEquals("Server", name.getKeyProperty("type"));
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, server, service, listener);
   }
   
   @Test
   public void stop() throws Exception
   {
      Service service = EasyMock.createStrictMock(Service.class);
      LifecycleServer server = EasyMock.createStrictMock(LifecycleServer.class);
      LifecycleListener listener = EasyMock.createStrictMock(LifecycleListener.class);
      LifecycleEngine engine = EasyMock.createStrictMock(LifecycleEngine.class);
      Container container = EasyMock.createStrictMock(Container.class);
      LifecycleContainer childContainer = EasyMock.createStrictMock(LifecycleContainer.class);
      Capture<ObjectName> capturedName = new Capture<ObjectName>(CaptureType.ALL);
      Capture<CatalinaServer> capturedServer = new Capture<CatalinaServer>();
      
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);

      this.initServer(adapter, server);
      this.startServer(adapter, server);
      
      EasyMock.expect(this.mbeanServer.invoke(EasyMock.capture(capturedName), EasyMock.eq("findServices"), EasyMock.<Object[]>isNull(), EasyMock.<String[]>isNull())).andReturn(new Service[] { service });
      EasyMock.expect(service.getServer()).andReturn(server);
      server.removeLifecycleListener(EasyMock.same(adapter));

      this.eventHandler.stop(EasyMock.capture(capturedServer));

      EasyMock.expect(server.findServices()).andReturn(new Service[] { service });
      
      EasyMock.expect(service.getContainer()).andReturn(engine);
      
      engine.removeContainerListener(adapter);
      engine.removeLifecycleListener(adapter);
      
      EasyMock.expect(engine.findChildren()).andReturn(new Container[] { container });
      
      container.removeContainerListener(adapter);
      
      EasyMock.expect(container.findChildren()).andReturn(new Container[] { childContainer });
      
      childContainer.removeLifecycleListener(adapter);
      
      this.eventHandler.shutdown();

      EasyMock.expect(this.mbeanServer.isRegistered(EasyMock.capture(capturedName))).andReturn(true);
      this.mbeanServer.removeNotificationListener(EasyMock.capture(capturedName), EasyMock.same(adapter));
      
      EasyMock.replay(this.eventHandler, this.mbeanServer, server, service, listener, engine, container, childContainer);
      
      adapter.stop();
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, server, service, listener, engine, container, childContainer);
      
      List<ObjectName> names = capturedName.getValues();
      Assert.assertEquals(3, names.size());
      ObjectName name = names.get(0);
      Assert.assertEquals("jboss.web", name.getDomain());
      Assert.assertEquals(1, name.getKeyPropertyList().size());
      Assert.assertEquals("Server", name.getKeyProperty("type"));
      name = names.get(1);
      Assert.assertSame(name, names.get(2));
      Assert.assertEquals("jboss.web", name.getDomain());
      Assert.assertEquals(1, name.getKeyPropertyList().size());
      Assert.assertEquals("WebServer", name.getKeyProperty("service"));
      
      CatalinaServer catalinaServer = capturedServer.getValue();
      Assert.assertSame(this.mbeanServer, catalinaServer.getMBeanServer());
      Assert.assertSame(server, catalinaServer.getServer());
      
      EasyMock.reset(this.eventHandler, this.mbeanServer, server, service, listener, engine, container, childContainer);
   }

   @Test
   public void stopNoServer() throws JMException
   {
      Capture<ObjectName> capturedName = new Capture<ObjectName>();
      
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);

      EasyMock.expect(this.mbeanServer.invoke(EasyMock.capture(capturedName), EasyMock.eq("findServices"), EasyMock.<Object[]>isNull(), EasyMock.<String[]>isNull())).andThrow(new InstanceNotFoundException());
      
      EasyMock.replay(this.eventHandler, this.mbeanServer);
      
      adapter.stop();
      
      EasyMock.verify(this.eventHandler, this.mbeanServer);
      
      ObjectName name = capturedName.getValue();
      Assert.assertEquals("jboss.web", name.getDomain());
      Assert.assertEquals(1, name.getKeyPropertyList().size());
      Assert.assertEquals("Server", name.getKeyProperty("type"));
      
      EasyMock.verify(this.eventHandler, this.mbeanServer);
   }
   
   @Test
   public void deployWebApp() throws Exception
   {
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);

      Host host = EasyMock.createStrictMock(Host.class);
      LifecycleContext context = EasyMock.createStrictMock(LifecycleContext.class);
      
      ContainerEvent event = new ContainerEvent(host, Container.ADD_CHILD_EVENT, context);
      
      context.addLifecycleListener(adapter);

      EasyMock.replay(this.eventHandler, this.mbeanServer, host, context);
      
      adapter.containerEvent(event);
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, host, context);
      EasyMock.reset(this.eventHandler, this.mbeanServer, host, context);

      LifecycleServer server = EasyMock.createStrictMock(LifecycleServer.class);
      
      this.initServer(adapter, server);
      
      context.addLifecycleListener(adapter);

      EasyMock.replay(this.eventHandler, this.mbeanServer, server, host, context);
      
      adapter.containerEvent(event);
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, server, host, context);
      EasyMock.reset(this.eventHandler, this.mbeanServer, server, host, context);
      
      this.startServer(adapter, server);
      
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Service service = EasyMock.createStrictMock(Service.class);
      Capture<CatalinaContext> capturedContext = new Capture<CatalinaContext>();
      
      context.addLifecycleListener(adapter);
      
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getParent()).andReturn(engine);
      
      EasyMock.expect(engine.getService()).andReturn(service);
      EasyMock.expect(service.getServer()).andReturn(server);
      this.eventHandler.add(EasyMock.capture(capturedContext));

      EasyMock.replay(this.eventHandler, this.mbeanServer, server, service, engine, host, context);
      
      adapter.containerEvent(event);
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, server, service, engine, host, context);
      
      Assert.assertSame(this.mbeanServer, capturedContext.getValue().getHost().getEngine().getServer().getMBeanServer());
      
      EasyMock.reset(this.eventHandler, this.mbeanServer, server, service, engine, host, context);
   }

   @Test
   public void deployHost()
   {
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);

      Engine engine = EasyMock.createStrictMock(Engine.class);

      ContainerEvent event = new ContainerEvent(engine, Container.ADD_CHILD_EVENT, null);

      engine.addContainerListener(adapter);
      
      EasyMock.replay(this.eventHandler, this.mbeanServer, engine);
      
      adapter.containerEvent(event);
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, engine);
      EasyMock.reset(this.eventHandler, this.mbeanServer, engine);
   }

   @Test
   public void undeployWebApp() throws Exception
   {
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);

      Host host = EasyMock.createStrictMock(Host.class);
      LifecycleContext context = EasyMock.createStrictMock(LifecycleContext.class);

      ContainerEvent event = new ContainerEvent(host, Container.REMOVE_CHILD_EVENT, context);
      
      context.removeLifecycleListener(adapter);
      
      EasyMock.replay(this.eventHandler, this.mbeanServer, host, context);
      
      adapter.containerEvent(event);
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, host, context);
      EasyMock.reset(this.eventHandler, this.mbeanServer, host, context);
      
      LifecycleServer server = EasyMock.createStrictMock(LifecycleServer.class);
      
      this.initServer(adapter, server);
      
      context.removeLifecycleListener(adapter);
      
      EasyMock.replay(this.eventHandler, this.mbeanServer, server, host, context);
      
      adapter.containerEvent(event);
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, server, host, context);
      EasyMock.reset(this.eventHandler, this.mbeanServer, server, host, context);
      
      this.startServer(adapter, server);
      
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Service service = EasyMock.createStrictMock(Service.class);
      Capture<CatalinaContext> capturedContext = new Capture<CatalinaContext>();
            
      context.removeLifecycleListener(adapter);
      
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getParent()).andReturn(engine);
      
      EasyMock.expect(engine.getService()).andReturn(service);
      EasyMock.expect(service.getServer()).andReturn(server);
      this.eventHandler.remove(EasyMock.capture(capturedContext));
      
      EasyMock.replay(this.eventHandler, this.mbeanServer, server, service, host, engine, context);
      
      adapter.containerEvent(event);
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, server, service, host, engine, context);
      
      Assert.assertSame(this.mbeanServer, capturedContext.getValue().getHost().getEngine().getServer().getMBeanServer());
      
      EasyMock.reset(this.eventHandler, this.mbeanServer, server, service, host, engine, context);
   }

   @Test
   public void undeployHost()
   {
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);

      Engine engine = EasyMock.createStrictMock(Engine.class);

      ContainerEvent event = new ContainerEvent(engine, Container.REMOVE_CHILD_EVENT, null);

      engine.removeContainerListener(adapter);
      
      EasyMock.replay(this.eventHandler, this.mbeanServer, engine);
      
      adapter.containerEvent(event);
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, engine);
      EasyMock.reset(this.eventHandler, this.mbeanServer, engine);
   }
   
   @Test
   public void startWebApp() throws Exception
   {
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);
      
      LifecycleContext context = EasyMock.createStrictMock(LifecycleContext.class);
      LifecycleEvent event = new LifecycleEvent(context, Lifecycle.START_EVENT);

      EasyMock.replay(this.eventHandler, this.mbeanServer);
      
      adapter.lifecycleEvent(event);
      
      EasyMock.verify(this.eventHandler, this.mbeanServer);
      EasyMock.reset(this.eventHandler, this.mbeanServer);

      LifecycleServer server = EasyMock.createStrictMock(LifecycleServer.class);
      
      this.initServer(adapter, server);
      
      EasyMock.replay(this.eventHandler, this.mbeanServer);
      
      adapter.lifecycleEvent(event);
      
      EasyMock.verify(this.eventHandler, this.mbeanServer);
      EasyMock.reset(this.eventHandler, this.mbeanServer);

      this.startServer(adapter, server);
      
      Host host = EasyMock.createStrictMock(Host.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Service service = EasyMock.createStrictMock(Service.class);
      Capture<CatalinaContext> capturedContext = new Capture<CatalinaContext>();
      
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getParent()).andReturn(engine);
      
      EasyMock.expect(engine.getService()).andReturn(service);
      EasyMock.expect(service.getServer()).andReturn(server);
      this.eventHandler.start(EasyMock.capture(capturedContext));
      
      EasyMock.replay(this.eventHandler, this.mbeanServer, server, service, engine, host, context);
      
      adapter.lifecycleEvent(event);
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, server, service, engine, host, context);
      
      Assert.assertSame(this.mbeanServer, capturedContext.getValue().getHost().getEngine().getServer().getMBeanServer());
      
      EasyMock.reset(this.eventHandler, this.mbeanServer, server, service, engine, host, context);
   }
   
   @Test
   public void initServer() throws Exception
   {
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);
      LifecycleServer server = EasyMock.createStrictMock(LifecycleServer.class);
      
      this.initServer(adapter, server);
      
      EasyMock.replay(this.eventHandler, this.mbeanServer, server);
      
      adapter.lifecycleEvent(new LifecycleEvent(server, Lifecycle.INIT_EVENT));
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, server);
      EasyMock.reset(this.eventHandler, this.mbeanServer, server);
   }
   
   private void initServer(CatalinaEventHandlerAdapter adapter, LifecycleServer server) throws Exception
   {
      Service service = EasyMock.createStrictMock(Service.class);
      LifecycleEngine engine = EasyMock.createStrictMock(LifecycleEngine.class);
      Container container = EasyMock.createStrictMock(Container.class);
      LifecycleContainer childContainer = EasyMock.createStrictMock(LifecycleContainer.class);
      Capture<ObjectName> capturedName = new Capture<ObjectName>(CaptureType.ALL);
      Capture<CatalinaServer> capturedServer = new Capture<CatalinaServer>();
      
      this.eventHandler.init(EasyMock.capture(capturedServer));
      
      EasyMock.expect(server.findServices()).andReturn(new Service[] { service });
      
      EasyMock.expect(service.getContainer()).andReturn(engine);
      
      engine.addContainerListener(adapter);
      engine.addLifecycleListener(adapter);
      
      EasyMock.expect(engine.findChildren()).andReturn(new Container[] { container });
      
      container.addContainerListener(adapter);
      
      EasyMock.expect(container.findChildren()).andReturn(new Container[] { childContainer });
      
      childContainer.addLifecycleListener(adapter);
      
      EasyMock.expect(this.mbeanServer.isRegistered(EasyMock.capture(capturedName))).andReturn(true);
      this.mbeanServer.addNotificationListener(EasyMock.capture(capturedName), EasyMock.same(adapter), EasyMock.<NotificationFilter>isNull(), EasyMock.same(server));
      
      EasyMock.replay(this.eventHandler, this.mbeanServer, server, service, engine, container, childContainer);
      
      adapter.lifecycleEvent(new LifecycleEvent(server, Lifecycle.INIT_EVENT));
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, server, service, engine, container, childContainer);
      
      List<ObjectName> names = capturedName.getValues();
      Assert.assertEquals(2, names.size());
      ObjectName name = names.get(0);
      Assert.assertSame(name, names.get(1));
      Assert.assertEquals("jboss.web", name.getDomain());
      Assert.assertEquals(1, name.getKeyPropertyList().size());
      Assert.assertEquals("WebServer", name.getKeyProperty("service"));
      
      CatalinaServer catalinaServer = capturedServer.getValue();
      Assert.assertSame(this.mbeanServer, catalinaServer.getMBeanServer());
      Assert.assertSame(server, catalinaServer.getServer());
      
      EasyMock.reset(this.eventHandler, this.mbeanServer, server, service, engine, container, childContainer);
   }
   
   @Test
   public void startServer() throws Exception
   {
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);
      LifecycleServer server = EasyMock.createStrictMock(LifecycleServer.class);

      this.initServer(adapter, server);
      
      this.startServer(adapter, server);
      
      EasyMock.replay(this.eventHandler, this.mbeanServer, server);
      
      adapter.lifecycleEvent(new LifecycleEvent(server, Lifecycle.AFTER_START_EVENT));
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, server);
      EasyMock.reset(this.eventHandler, this.mbeanServer, server);
   }
   
   private void startServer(CatalinaEventHandlerAdapter adapter, LifecycleServer server)
   {
      Capture<CatalinaServer> capturedServer = new Capture<CatalinaServer>();
      
      this.eventHandler.start(EasyMock.capture(capturedServer));
      
      EasyMock.replay(this.eventHandler, this.mbeanServer, server);
      
      adapter.lifecycleEvent(new LifecycleEvent(server, Lifecycle.AFTER_START_EVENT));
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, server);

      CatalinaServer catalinaServer = capturedServer.getValue();
      Assert.assertSame(this.mbeanServer, catalinaServer.getMBeanServer());
      Assert.assertSame(server, catalinaServer.getServer());
      
      EasyMock.reset(this.eventHandler, this.mbeanServer, server);
   }
   
   @Test
   public void stopWebApp() throws Exception
   {
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);

      LifecycleContext context = EasyMock.createStrictMock(LifecycleContext.class);
      
      LifecycleEvent event = new LifecycleEvent(context, Lifecycle.BEFORE_STOP_EVENT);
      
      EasyMock.replay(this.eventHandler, this.mbeanServer, context);
      
      adapter.lifecycleEvent(event);
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, context);
      EasyMock.reset(this.eventHandler, this.mbeanServer, context);
      
      LifecycleServer server = EasyMock.createStrictMock(LifecycleServer.class);
      
      this.initServer(adapter, server);
      
      EasyMock.replay(this.eventHandler, this.mbeanServer, context);
      
      adapter.lifecycleEvent(event);
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, context);
      EasyMock.reset(this.eventHandler, this.mbeanServer, context);
      
      this.startServer(adapter, server);
      
      Host host = EasyMock.createStrictMock(Host.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Service service = EasyMock.createStrictMock(Service.class);
      Capture<CatalinaContext> capturedContext = new Capture<CatalinaContext>();

      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getParent()).andReturn(engine);
      
      EasyMock.expect(engine.getService()).andReturn(service);
      EasyMock.expect(service.getServer()).andReturn(server);
      this.eventHandler.stop(EasyMock.capture(capturedContext));
      
      EasyMock.replay(this.eventHandler, this.mbeanServer, engine, host, context);
      
      adapter.lifecycleEvent(event);
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, engine, host, context);
      
      Assert.assertSame(this.mbeanServer, capturedContext.getValue().getHost().getEngine().getServer().getMBeanServer());
      
      EasyMock.reset(this.eventHandler, this.mbeanServer, engine, host, context);
   }
   
   @Test
   public void stopServer() throws Exception
   {
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);
      
      LifecycleServer server = EasyMock.createStrictMock(LifecycleServer.class);
      LifecycleEvent event = new LifecycleEvent(server, Lifecycle.BEFORE_STOP_EVENT);
      Capture<CatalinaServer> capturedServer = new Capture<CatalinaServer>();

      EasyMock.replay(this.eventHandler, this.mbeanServer, server);
      
      adapter.lifecycleEvent(event);
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, server);
      EasyMock.reset(this.eventHandler, this.mbeanServer, server);
      
      this.initServer(adapter, server);

      EasyMock.replay(this.eventHandler, this.mbeanServer, server);
      
      adapter.lifecycleEvent(event);
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, server);
      EasyMock.reset(this.eventHandler, this.mbeanServer, server);
      
      this.startServer(adapter, server);

      this.eventHandler.stop(EasyMock.capture(capturedServer));
      
      EasyMock.replay(this.eventHandler, this.mbeanServer, server);
      
      adapter.lifecycleEvent(event);
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, server);
      
      CatalinaServer catalinaServer = capturedServer.getValue();
      Assert.assertSame(this.mbeanServer, catalinaServer.getMBeanServer());
      Assert.assertSame(server, catalinaServer.getServer());
      
      EasyMock.reset(this.eventHandler, this.mbeanServer, server);
      
      EasyMock.replay(this.eventHandler, this.mbeanServer, server);
      
      adapter.lifecycleEvent(event);
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, server);
      EasyMock.reset(this.eventHandler, this.mbeanServer, server);
   }
   
   @Test
   public void destroyServer() throws Exception
   {
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);
      
      LifecycleServer server = EasyMock.createStrictMock(LifecycleServer.class);
      LifecycleEvent event = new LifecycleEvent(server, Lifecycle.DESTROY_EVENT);

      EasyMock.replay(this.eventHandler, this.mbeanServer, server);
      
      adapter.lifecycleEvent(event);
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, server);
      EasyMock.reset(this.eventHandler, this.mbeanServer, server);
      
      this.initServer(adapter, server);

      Service service = EasyMock.createStrictMock(Service.class);
      LifecycleEngine engine = EasyMock.createStrictMock(LifecycleEngine.class);
      Container container = EasyMock.createStrictMock(Container.class);
      LifecycleContainer childContainer = EasyMock.createStrictMock(LifecycleContainer.class);
      Capture<ObjectName> capturedName = new Capture<ObjectName>(CaptureType.ALL);
      
      EasyMock.expect(server.findServices()).andReturn(new Service[] { service });
      
      EasyMock.expect(service.getContainer()).andReturn(engine);
      
      engine.removeContainerListener(adapter);
      engine.removeLifecycleListener(adapter);
      
      EasyMock.expect(engine.findChildren()).andReturn(new Container[] { container });
      
      container.removeContainerListener(adapter);
      
      EasyMock.expect(container.findChildren()).andReturn(new Container[] { childContainer });
      
      childContainer.removeLifecycleListener(adapter);
      
      this.eventHandler.shutdown();

      EasyMock.expect(this.mbeanServer.isRegistered(EasyMock.capture(capturedName))).andReturn(true);
      this.mbeanServer.removeNotificationListener(EasyMock.capture(capturedName), EasyMock.same(adapter));
      
      EasyMock.replay(this.eventHandler, this.mbeanServer, server, service, engine, container, childContainer);
      
      adapter.lifecycleEvent(event);
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, server, service, engine, container, childContainer);
      
      List<ObjectName> names = capturedName.getValues();
      Assert.assertEquals(2, names.size());
      ObjectName name = names.get(0);
      Assert.assertSame(name, names.get(1));
      Assert.assertEquals("jboss.web", name.getDomain());
      Assert.assertEquals(1, name.getKeyPropertyList().size());
      Assert.assertEquals("WebServer", name.getKeyProperty("service"));
            
      EasyMock.reset(this.eventHandler, this.mbeanServer, server, service, engine, container, childContainer);
      
      EasyMock.replay(this.eventHandler, this.mbeanServer, server);
      
      adapter.lifecycleEvent(event);
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, server);
      EasyMock.reset(this.eventHandler, this.mbeanServer, server);
   }
   
   @Test
   public void periodicEvent() throws Exception
   {
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);
      
      LifecycleEngine engine = EasyMock.createStrictMock(LifecycleEngine.class);
      
      LifecycleEvent event = new LifecycleEvent(engine, Lifecycle.PERIODIC_EVENT);
      
      EasyMock.replay(this.eventHandler, this.mbeanServer, engine);
      
      adapter.lifecycleEvent(event);
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, engine);
      EasyMock.reset(this.eventHandler, this.mbeanServer, engine);

      LifecycleServer server = EasyMock.createStrictMock(LifecycleServer.class);
      
      this.initServer(adapter, server);
      
      EasyMock.replay(this.eventHandler, this.mbeanServer, engine, server);
      
      adapter.lifecycleEvent(event);
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, engine, server);
      EasyMock.reset(this.eventHandler, this.mbeanServer, engine, server);
      
      this.startServer(adapter, server);
      
      Service service = EasyMock.createStrictMock(Service.class);
      Capture<CatalinaEngine> capturedEngine = new Capture<CatalinaEngine>();

      EasyMock.expect(engine.getService()).andReturn(service);
      EasyMock.expect(service.getServer()).andReturn(server);
      
      this.eventHandler.status(EasyMock.capture(capturedEngine));
      
      EasyMock.replay(this.eventHandler, this.mbeanServer, engine, service, server);
      
      adapter.lifecycleEvent(event);
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, engine, service, server);
      
      Assert.assertSame(this.mbeanServer, capturedEngine.getValue().getServer().getMBeanServer());
      
      EasyMock.reset(this.eventHandler, this.mbeanServer, engine, service, server);
   }
   
   @Test
   public void handleConnectorsStartedNotification() throws Exception
   {
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);
      LifecycleServer server = EasyMock.createStrictMock(LifecycleServer.class);
      
      Notification notification = new Notification("jboss.tomcat.connectors.started", new Object(), 1);
      
      EasyMock.replay(this.eventHandler, this.mbeanServer);
      
      adapter.handleNotification(notification, server);
      
      EasyMock.verify(this.eventHandler, this.mbeanServer);
      EasyMock.reset(this.eventHandler, this.mbeanServer);
      
      this.initServer(adapter, server);
      
      EasyMock.replay(this.eventHandler, this.mbeanServer);
      
      adapter.handleNotification(notification, server);
      
      EasyMock.verify(this.eventHandler, this.mbeanServer);
      EasyMock.reset(this.eventHandler, this.mbeanServer);

      this.startServer(adapter, server);
      
      Service service = EasyMock.createStrictMock(Service.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Capture<CatalinaEngine> capturedEngine = new Capture<CatalinaEngine>();

      EasyMock.expect(server.findServices()).andReturn(new Service[] { service });
      EasyMock.expect(service.getContainer()).andReturn(engine);
      EasyMock.expect(engine.getService()).andReturn(service);
      EasyMock.expect(service.getServer()).andReturn(server);
      
      this.eventHandler.status(EasyMock.capture(capturedEngine));
      
      EasyMock.replay(this.eventHandler, this.mbeanServer, server, service, engine);
      
      adapter.handleNotification(notification, server);
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, server, service, engine);
      
      Assert.assertSame(this.mbeanServer, capturedEngine.getValue().getServer().getMBeanServer());
      
      EasyMock.reset(this.eventHandler, this.mbeanServer, server, service, engine);
   }
   
   @Test
   public void handleConnectorsStoppedNotification() throws Exception
   {
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);
      LifecycleServer server = EasyMock.createStrictMock(LifecycleServer.class);
      
      Notification notification = new Notification("jboss.tomcat.connectors.stopped", new Object(), 1);
      
      EasyMock.replay(this.eventHandler, this.mbeanServer);
      
      adapter.handleNotification(notification, server);
      
      EasyMock.verify(this.eventHandler, this.mbeanServer);
      EasyMock.reset(this.eventHandler, this.mbeanServer);
      
      this.initServer(adapter, server);
      
      EasyMock.replay(this.eventHandler, this.mbeanServer);
      
      adapter.handleNotification(notification, server);
      
      EasyMock.verify(this.eventHandler, this.mbeanServer);
      EasyMock.reset(this.eventHandler, this.mbeanServer);

      this.startServer(adapter, server);
      
      Capture<CatalinaServer> capturedServer = new Capture<CatalinaServer>();

      this.eventHandler.stop(EasyMock.capture(capturedServer));
      
      EasyMock.replay(this.eventHandler, this.mbeanServer, server);
      
      adapter.handleNotification(notification, server);
      
      EasyMock.verify(this.eventHandler, this.mbeanServer, server);
      
      Assert.assertSame(this.mbeanServer, capturedServer.getValue().getMBeanServer());
      
      EasyMock.reset(this.eventHandler, this.mbeanServer, server);
   }
   
   @Test
   public void handleOtherNotification()
   {
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);
      LifecycleServer server = EasyMock.createStrictMock(LifecycleServer.class);
      
      Notification notification = new Notification("blah", new Object(), 1);
      
      EasyMock.replay(this.eventHandler, this.mbeanServer);
      
      adapter.handleNotification(notification, server);
      
      EasyMock.verify(this.eventHandler, this.mbeanServer);
      EasyMock.reset(this.eventHandler, this.mbeanServer);
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
