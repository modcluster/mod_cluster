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

import java.beans.PropertyChangeEvent;

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
import org.easymock.IMocksControl;
import org.jboss.modcluster.ContainerEventHandler;
import org.junit.After;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
public class CatalinaEventHandlerAdapterTestCase
{
   private IMocksControl control = EasyMock.createStrictControl();
   private ContainerEventHandler eventHandler = this.control.createMock(ContainerEventHandler.class);
   private MBeanServer mbeanServer = this.control.createMock(MBeanServer.class);

   @After
   public void after()
   {
      this.control.reset();
   }
   
   @Test
   public void start() throws JMException
   {
      Service service = this.control.createMock(Service.class);
      LifecycleServer server = this.control.createMock(LifecycleServer.class);
      LifecycleListener listener = this.control.createMock(LifecycleListener.class);
      LifecycleEngine engine = this.control.createMock(LifecycleEngine.class);
      Container container = this.control.createMock(Container.class);
      LifecycleContainer childContainer = this.control.createMock(LifecycleContainer.class);
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
      
      engine.addContainerListener(EasyMock.same(adapter));
      engine.addLifecycleListener(EasyMock.same(adapter));
      
      EasyMock.expect(engine.findChildren()).andReturn(new Container[] { container });
      
      container.addContainerListener(adapter);
      
      EasyMock.expect(container.findChildren()).andReturn(new Container[] { childContainer });
      
      childContainer.addLifecycleListener(adapter);
      
      EasyMock.expect(this.mbeanServer.isRegistered(EasyMock.capture(capturedName))).andReturn(true);
      
      this.mbeanServer.addNotificationListener(EasyMock.capture(capturedName), EasyMock.same(adapter), EasyMock.<NotificationFilter>isNull(), EasyMock.same(server));

      this.eventHandler.start(EasyMock.capture(capturedServer));
      
      this.control.replay();
      
      adapter.start();

      this.control.verify();
      
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
   }

   @Test
   public void startNoServer() throws JMException
   {
      Capture<ObjectName> capturedName = new Capture<ObjectName>();
      
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);

      EasyMock.expect(this.mbeanServer.invoke(EasyMock.capture(capturedName), EasyMock.eq("findServices"), EasyMock.<Object[]>isNull(), EasyMock.<String[]>isNull())).andThrow(new InstanceNotFoundException());
      
      this.control.replay();
      
      adapter.start();

      this.control.verify();
      
      ObjectName name = capturedName.getValue();
      Assert.assertEquals("jboss.web", name.getDomain());
      Assert.assertEquals(1, name.getKeyPropertyList().size());
      Assert.assertEquals("Server", name.getKeyProperty("type"));
   }

   @Test
   public void startAlreadyRegistered() throws Exception
   {
      Service service = this.control.createMock(Service.class);
      LifecycleServer server = this.control.createMock(LifecycleServer.class);
      LifecycleListener listener = this.control.createMock(LifecycleListener.class);
      Capture<ObjectName> capturedName = new Capture<ObjectName>();
      
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);

      this.initServer(adapter, server);
      this.startServer(adapter, server);
      
      EasyMock.expect(this.mbeanServer.invoke(EasyMock.capture(capturedName), EasyMock.eq("findServices"), EasyMock.<Object[]>isNull(), EasyMock.<String[]>isNull())).andReturn(new Service[] { service });
      EasyMock.expect(service.getServer()).andReturn(server);
      EasyMock.expect(server.findLifecycleListeners()).andReturn(new LifecycleListener[] { listener, adapter });
      
      this.control.replay();
      
      adapter.start();
      
      this.control.verify();
      
      ObjectName name = capturedName.getValue();
      Assert.assertEquals("jboss.web", name.getDomain());
      Assert.assertEquals(1, name.getKeyPropertyList().size());
      Assert.assertEquals("Server", name.getKeyProperty("type"));
   }
   
   @Test
   public void stop() throws Exception
   {
      Service service = this.control.createMock(Service.class);
      LifecycleServer server = this.control.createMock(LifecycleServer.class);
      LifecycleEngine engine = this.control.createMock(LifecycleEngine.class);
      Container container = this.control.createMock(Container.class);
      LifecycleContainer childContainer = this.control.createMock(LifecycleContainer.class);
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
      
      engine.removeContainerListener(EasyMock.same(adapter));
      engine.removeLifecycleListener(EasyMock.same(adapter));
      
      EasyMock.expect(engine.findChildren()).andReturn(new Container[] { container });
      
      container.removeContainerListener(EasyMock.same(adapter));
      
      EasyMock.expect(container.findChildren()).andReturn(new Container[] { childContainer });
      
      childContainer.removeLifecycleListener(EasyMock.same(adapter));

      EasyMock.expect(this.mbeanServer.isRegistered(EasyMock.capture(capturedName))).andReturn(true);
      this.mbeanServer.removeNotificationListener(EasyMock.capture(capturedName), EasyMock.same(adapter));
      
      this.eventHandler.shutdown();
      
      this.control.replay();
      
      adapter.stop();

      this.control.verify();
      
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
   }

   @Test
   public void stopNoServer() throws JMException
   {
      Capture<ObjectName> capturedName = new Capture<ObjectName>();
      
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);

      EasyMock.expect(this.mbeanServer.invoke(EasyMock.capture(capturedName), EasyMock.eq("findServices"), EasyMock.<Object[]>isNull(), EasyMock.<String[]>isNull())).andThrow(new InstanceNotFoundException());
      
      this.control.replay();
      
      adapter.stop();

      this.control.verify();
      
      ObjectName name = capturedName.getValue();
      Assert.assertEquals("jboss.web", name.getDomain());
      Assert.assertEquals(1, name.getKeyPropertyList().size());
      Assert.assertEquals("Server", name.getKeyProperty("type"));
   }
   
   @Test
   public void deployWebApp() throws Exception
   {
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);

      Host host = this.control.createMock(Host.class);
      LifecycleContext context = this.control.createMock(LifecycleContext.class);
      
      ContainerEvent event = new ContainerEvent(host, Container.ADD_CHILD_EVENT, context);
      
      context.addLifecycleListener(EasyMock.same(adapter));
      context.addPropertyChangeListener(EasyMock.same(adapter));

      this.control.replay();
      
      adapter.containerEvent(event);

      this.control.verify();
      this.control.reset();

      LifecycleServer server = this.control.createMock(LifecycleServer.class);
      
      this.initServer(adapter, server);
      
      context.addLifecycleListener(EasyMock.same(adapter));
      context.addPropertyChangeListener(EasyMock.same(adapter));

      this.control.replay();
      
      adapter.containerEvent(event);

      this.control.verify();
      this.control.reset();
      
      this.startServer(adapter, server);
      
      Engine engine = this.control.createMock(Engine.class);
      Service service = this.control.createMock(Service.class);
      Capture<CatalinaContext> capturedContext = new Capture<CatalinaContext>();
      
      context.addLifecycleListener(EasyMock.same(adapter));
      context.addPropertyChangeListener(EasyMock.same(adapter));
      
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getParent()).andReturn(engine);
      
      EasyMock.expect(engine.getService()).andReturn(service);
      EasyMock.expect(service.getServer()).andReturn(server);
      this.eventHandler.add(EasyMock.capture(capturedContext));

      this.control.replay();
      
      adapter.containerEvent(event);

      this.control.verify();
      
      Assert.assertSame(this.mbeanServer, capturedContext.getValue().getHost().getEngine().getServer().getMBeanServer());
   }

   @Test
   public void deployHost()
   {
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);

      Engine engine = this.control.createMock(Engine.class);

      ContainerEvent event = new ContainerEvent(engine, Container.ADD_CHILD_EVENT, null);

      engine.addContainerListener(EasyMock.same(adapter));
      
      this.control.replay();
      
      adapter.containerEvent(event);

      this.control.verify();
   }

   @Test
   public void undeployWebApp() throws Exception
   {
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);

      Host host = this.control.createMock(Host.class);
      LifecycleContext context = this.control.createMock(LifecycleContext.class);

      ContainerEvent event = new ContainerEvent(host, Container.REMOVE_CHILD_EVENT, context);
      
      context.removeLifecycleListener(EasyMock.same(adapter));
      context.removePropertyChangeListener(EasyMock.same(adapter));

      this.control.replay();
      
      adapter.containerEvent(event);

      this.control.verify();
      this.control.reset();
      
      LifecycleServer server = this.control.createMock(LifecycleServer.class);
      
      this.initServer(adapter, server);
      
      context.removeLifecycleListener(EasyMock.same(adapter));
      context.removePropertyChangeListener(EasyMock.same(adapter));

      this.control.replay();
      
      adapter.containerEvent(event);

      this.control.verify();
      this.control.reset();
      
      this.startServer(adapter, server);
      
      Engine engine = this.control.createMock(Engine.class);
      Service service = this.control.createMock(Service.class);
      Capture<CatalinaContext> capturedContext = new Capture<CatalinaContext>();
            
      context.removeLifecycleListener(EasyMock.same(adapter));
      context.removePropertyChangeListener(EasyMock.same(adapter));
      
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getParent()).andReturn(engine);
      
      EasyMock.expect(engine.getService()).andReturn(service);
      EasyMock.expect(service.getServer()).andReturn(server);
      this.eventHandler.remove(EasyMock.capture(capturedContext));
      
      this.control.replay();
      
      adapter.containerEvent(event);

      this.control.verify();
      
      Assert.assertSame(this.mbeanServer, capturedContext.getValue().getHost().getEngine().getServer().getMBeanServer());
   }

   @Test
   public void undeployHost()
   {
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);

      Engine engine = EasyMock.createStrictMock(Engine.class);

      ContainerEvent event = new ContainerEvent(engine, Container.REMOVE_CHILD_EVENT, null);

      engine.removeContainerListener(EasyMock.same(adapter));

      this.control.replay();
      
      adapter.containerEvent(event);

      this.control.verify();
   }
   
   @Test
   public void initServer() throws Exception
   {
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);
      LifecycleServer server = this.control.createMock(LifecycleServer.class);
      
      this.initServer(adapter, server);
      
      this.control.replay();
      
      adapter.lifecycleEvent(new LifecycleEvent(server, Lifecycle.INIT_EVENT));

      this.control.verify();
   }
   
   private void initServer(CatalinaEventHandlerAdapter adapter, LifecycleServer server) throws Exception
   {
      Service service = this.control.createMock(Service.class);
      LifecycleEngine engine = this.control.createMock(LifecycleEngine.class);
      Container container = this.control.createMock(Container.class);
      LifecycleContainer childContainer = this.control.createMock(LifecycleContainer.class);
      Capture<ObjectName> capturedName = new Capture<ObjectName>(CaptureType.ALL);
      Capture<CatalinaServer> capturedServer = new Capture<CatalinaServer>();
      
      this.eventHandler.init(EasyMock.capture(capturedServer));
      
      EasyMock.expect(server.findServices()).andReturn(new Service[] { service });
      
      EasyMock.expect(service.getContainer()).andReturn(engine);
      
      engine.addContainerListener(EasyMock.same(adapter));
      engine.addLifecycleListener(EasyMock.same(adapter));
      
      EasyMock.expect(engine.findChildren()).andReturn(new Container[] { container });
      
      container.addContainerListener(EasyMock.same(adapter));
      
      EasyMock.expect(container.findChildren()).andReturn(new Container[] { childContainer });
      
      childContainer.addLifecycleListener(adapter);
      
      EasyMock.expect(this.mbeanServer.isRegistered(EasyMock.capture(capturedName))).andReturn(true);
      this.mbeanServer.addNotificationListener(EasyMock.capture(capturedName), EasyMock.same(adapter), EasyMock.<NotificationFilter>isNull(), EasyMock.same(server));
      
      this.control.replay();
      
      adapter.lifecycleEvent(new LifecycleEvent(server, Lifecycle.INIT_EVENT));

      this.control.verify();
      
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
      
      this.control.reset();
   }
   
   @Test
   public void startServer() throws Exception
   {
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);
      LifecycleServer server = this.control.createMock(LifecycleServer.class);

      this.initServer(adapter, server);
      
      this.startServer(adapter, server);
      
      this.control.replay();
      
      adapter.lifecycleEvent(new LifecycleEvent(server, Lifecycle.AFTER_START_EVENT));

      this.control.verify();
   }
   
   private void startServer(CatalinaEventHandlerAdapter adapter, LifecycleServer server)
   {
      Capture<CatalinaServer> capturedServer = new Capture<CatalinaServer>();
      
      this.eventHandler.start(EasyMock.capture(capturedServer));
      
      this.control.replay();
      
      adapter.lifecycleEvent(new LifecycleEvent(server, Lifecycle.AFTER_START_EVENT));

      this.control.verify();

      CatalinaServer catalinaServer = capturedServer.getValue();
      Assert.assertSame(this.mbeanServer, catalinaServer.getMBeanServer());
      Assert.assertSame(server, catalinaServer.getServer());
      
      this.control.reset();
   }
   
   @Test
   public void stopServer() throws Exception
   {
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);
      
      LifecycleServer server = this.control.createMock(LifecycleServer.class);
      LifecycleEvent event = new LifecycleEvent(server, Lifecycle.BEFORE_STOP_EVENT);
      Capture<CatalinaServer> capturedServer = new Capture<CatalinaServer>();

      // Test not yet initialized
      this.control.replay();
      
      adapter.lifecycleEvent(event);

      this.control.verify();
      this.control.reset();

      // Test initialized, but not yet started
      this.initServer(adapter, server);

      this.control.replay();
      
      adapter.lifecycleEvent(event);

      this.control.verify();
      this.control.reset();

      // Test started
      this.startServer(adapter, server);

      this.eventHandler.stop(EasyMock.capture(capturedServer));

      this.control.replay();
      
      adapter.lifecycleEvent(event);

      this.control.verify();
      
      CatalinaServer catalinaServer = capturedServer.getValue();
      Assert.assertSame(this.mbeanServer, catalinaServer.getMBeanServer());
      Assert.assertSame(server, catalinaServer.getServer());

      this.control.reset();

      // Test already stopped
      this.control.replay();
      
      adapter.lifecycleEvent(event);

      this.control.verify();
   }
   
   @Test
   public void destroyServer() throws Exception
   {
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);
      
      LifecycleServer server = this.control.createMock(LifecycleServer.class);
      LifecycleEvent event = new LifecycleEvent(server, Lifecycle.DESTROY_EVENT);

      // Test not yet initialized
      this.control.replay();
      
      adapter.lifecycleEvent(event);

      this.control.verify();
      this.control.reset();
      
      // Test initialized
      this.initServer(adapter, server);

      Service service = this.control.createMock(Service.class);
      LifecycleEngine engine = this.control.createMock(LifecycleEngine.class);
      Container container = this.control.createMock(Container.class);
      LifecycleContainer childContainer = this.control.createMock(LifecycleContainer.class);
      Capture<ObjectName> capturedName = new Capture<ObjectName>(CaptureType.ALL);
      
      EasyMock.expect(server.findServices()).andReturn(new Service[] { service });
      
      EasyMock.expect(service.getContainer()).andReturn(engine);
      
      engine.removeContainerListener(EasyMock.same(adapter));
      engine.removeLifecycleListener(EasyMock.same(adapter));
      
      EasyMock.expect(engine.findChildren()).andReturn(new Container[] { container });
      
      container.removeContainerListener(EasyMock.same(adapter));
      
      EasyMock.expect(container.findChildren()).andReturn(new Container[] { childContainer });
      
      childContainer.removeLifecycleListener(EasyMock.same(adapter));

      EasyMock.expect(this.mbeanServer.isRegistered(EasyMock.capture(capturedName))).andReturn(true);
      this.mbeanServer.removeNotificationListener(EasyMock.capture(capturedName), EasyMock.same(adapter));
      
      this.eventHandler.shutdown();
      
      this.control.replay();
      
      adapter.lifecycleEvent(event);
      
      this.control.verify();
      
      List<ObjectName> names = capturedName.getValues();
      Assert.assertEquals(2, names.size());
      ObjectName name = names.get(0);
      Assert.assertSame(name, names.get(1));
      Assert.assertEquals("jboss.web", name.getDomain());
      Assert.assertEquals(1, name.getKeyPropertyList().size());
      Assert.assertEquals("WebServer", name.getKeyProperty("service"));

      this.control.reset();

      // Test already destroyed
      this.control.replay();
      
      adapter.lifecycleEvent(event);

      this.control.verify();
   }
   
   @Test
   public void periodicEvent() throws Exception
   {
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);
      
      LifecycleEngine engine = this.control.createMock(LifecycleEngine.class);
      
      LifecycleEvent event = new LifecycleEvent(engine, Lifecycle.PERIODIC_EVENT);
      
      this.control.replay();
      
      adapter.lifecycleEvent(event);

      this.control.verify();
      this.control.reset();

      LifecycleServer server = this.control.createMock(LifecycleServer.class);
      
      this.initServer(adapter, server);
      
      this.control.replay();
      
      adapter.lifecycleEvent(event);

      this.control.verify();
      this.control.reset();
      
      this.startServer(adapter, server);
      
      Service service = this.control.createMock(Service.class);
      Capture<CatalinaEngine> capturedEngine = new Capture<CatalinaEngine>();

      EasyMock.expect(engine.getService()).andReturn(service);
      EasyMock.expect(service.getServer()).andReturn(server);
      
      this.eventHandler.status(EasyMock.capture(capturedEngine));
      
      this.control.replay();
      
      adapter.lifecycleEvent(event);

      this.control.verify();
      
      Assert.assertSame(this.mbeanServer, capturedEngine.getValue().getServer().getMBeanServer());
   }
   
   @Test
   public void handleConnectorsStartedNotification() throws Exception
   {
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);
      LifecycleServer server = this.control.createMock(LifecycleServer.class);
      
      Notification notification = new Notification("jboss.tomcat.connectors.started", new Object(), 1);
      
      this.control.replay();
      
      adapter.handleNotification(notification, server);

      this.control.verify();
      this.control.reset();
      
      this.initServer(adapter, server);
      
      this.control.replay();
      
      adapter.handleNotification(notification, server);

      this.control.verify();
      this.control.reset();

      this.startServer(adapter, server);
      
      Service service = this.control.createMock(Service.class);
      Engine engine = this.control.createMock(Engine.class);
      Capture<CatalinaEngine> capturedEngine = new Capture<CatalinaEngine>();

      EasyMock.expect(server.findServices()).andReturn(new Service[] { service });
      EasyMock.expect(service.getContainer()).andReturn(engine);
      EasyMock.expect(engine.getService()).andReturn(service);
      EasyMock.expect(service.getServer()).andReturn(server);
      
      this.eventHandler.status(EasyMock.capture(capturedEngine));
      
      this.control.replay();
      
      adapter.handleNotification(notification, server);

      this.control.verify();
      
      Assert.assertSame(this.mbeanServer, capturedEngine.getValue().getServer().getMBeanServer());
   }
   
   @Test
   public void handleConnectorsStoppedNotification() throws Exception
   {
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);
      LifecycleServer server = this.control.createMock(LifecycleServer.class);
      
      Notification notification = new Notification("jboss.tomcat.connectors.stopped", new Object(), 1);
      
      this.control.replay();
      
      adapter.handleNotification(notification, server);

      this.control.verify();
      this.control.reset();
      
      this.initServer(adapter, server);

      this.control.replay();
      
      adapter.handleNotification(notification, server);

      this.control.verify();
      this.control.reset();

      this.startServer(adapter, server);
      
      Capture<CatalinaServer> capturedServer = new Capture<CatalinaServer>();

      this.eventHandler.stop(EasyMock.capture(capturedServer));
      
      this.control.replay();
      
      adapter.handleNotification(notification, server);

      this.control.verify();
      
      Assert.assertSame(this.mbeanServer, capturedServer.getValue().getMBeanServer());
   }
   
   @Test
   public void handleOtherNotification()
   {
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);
      LifecycleServer server = this.control.createMock(LifecycleServer.class);
      
      Notification notification = new Notification("blah", new Object(), 1);
      
      this.control.replay();
      
      adapter.handleNotification(notification, server);

      this.control.verify();
   }
   
   @Test
   public void propertyChanged() throws Exception
   {
      CatalinaEventHandlerAdapter adapter = new CatalinaEventHandlerAdapter(this.eventHandler, this.mbeanServer);
      
      // Test other event source
      PropertyChangeEvent event = new PropertyChangeEvent(new Object(), "available", Boolean.FALSE, Boolean.TRUE);
      
      this.control.replay();
      
      adapter.propertyChange(event);
      
      this.control.verify();
      this.control.reset();

      Context context = this.control.createMock(Context.class);
      
      // Test other event property
      event = new PropertyChangeEvent(context, "other", Boolean.FALSE, Boolean.TRUE);
      
      this.control.replay();
      
      adapter.propertyChange(event);
      
      this.control.verify();
      this.control.reset();
      
      // Test not yet initialized
      event = new PropertyChangeEvent(context, "available", Boolean.FALSE, Boolean.TRUE);
      
      this.control.replay();
      
      adapter.propertyChange(event);
      
      this.control.verify();
      this.control.reset();

      LifecycleServer server = this.control.createMock(LifecycleServer.class);

      // Test initialized, but not yet started
      this.initServer(adapter, server);
      
      this.control.replay();
      
      adapter.propertyChange(event);
      
      this.control.verify();
      this.control.reset();

      // Test start app
      this.startServer(adapter, server);
      
      Host host = this.control.createMock(Host.class);
      Engine engine = this.control.createMock(Engine.class);
      Service service = this.control.createMock(Service.class);
      Capture<CatalinaContext> capturedContext = new Capture<CatalinaContext>();
      
      event = new PropertyChangeEvent(context, "available", Boolean.FALSE, Boolean.TRUE);
      
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getParent()).andReturn(engine);
      
      EasyMock.expect(engine.getService()).andReturn(service);
      EasyMock.expect(service.getServer()).andReturn(server);
      this.eventHandler.start(EasyMock.capture(capturedContext));
      
      this.control.replay();
      
      adapter.propertyChange(event);
      
      this.control.verify();
      
      Assert.assertSame(this.mbeanServer, capturedContext.getValue().getHost().getEngine().getServer().getMBeanServer());
      
      this.control.reset();
      
      capturedContext.reset();
      // Test stop app
      event = new PropertyChangeEvent(context, "available", Boolean.TRUE, Boolean.FALSE);
      
      EasyMock.expect(context.getParent()).andReturn(host);
      EasyMock.expect(host.getParent()).andReturn(engine);
      
      EasyMock.expect(engine.getService()).andReturn(service);
      EasyMock.expect(service.getServer()).andReturn(server);
      this.eventHandler.stop(EasyMock.capture(capturedContext));
      
      this.control.replay();
      
      adapter.propertyChange(event);
      
      this.control.verify();
      
      Assert.assertSame(this.mbeanServer, capturedContext.getValue().getHost().getEngine().getServer().getMBeanServer());
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
