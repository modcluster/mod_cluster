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
package org.jboss.modcluster.container.catalina;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.beans.PropertyChangeEvent;
import java.util.List;

import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.ObjectName;

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
import org.jboss.modcluster.container.ContainerEventHandler;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Paul Ferraro
 * 
 */
public class ContainerEventHandlerAdapterTestCase {
    private final ContainerEventHandler eventHandler = mock(ContainerEventHandler.class);
    private final MBeanServer mbeanServer = mock(MBeanServer.class);
    private final LifecycleServer server = mock(LifecycleServer.class);

    protected CatalinaEventHandler createEventHandler(ContainerEventHandler eventHandler, Server server, MBeanServer mbeanServer) {
        return new CatalinaEventHandlerAdapter(eventHandler, server, mbeanServer);
    }

    protected CatalinaEventHandler createEventHandler(ContainerEventHandler eventHandler, MBeanServer mbeanServer) {
        return new CatalinaEventHandlerAdapter(eventHandler, mbeanServer);
    }

    @Test
    public void start() throws JMException {
        Service service = mock(Service.class);
        LifecycleListener listener = mock(LifecycleListener.class);
        LifecycleEngine engine = mock(LifecycleEngine.class);
        Container container = mock(Container.class);
        LifecycleContainer childContainer = mock(LifecycleContainer.class);
        ArgumentCaptor<ObjectName> capturedName = ArgumentCaptor.forClass(ObjectName.class);
        ArgumentCaptor<CatalinaServer> capturedServer = ArgumentCaptor.forClass(CatalinaServer.class);

        CatalinaEventHandler handler = this.createEventHandler(this.eventHandler, this.server, this.mbeanServer);

        when(this.server.findLifecycleListeners()).thenReturn(new LifecycleListener[] { listener });
        when(this.server.findServices()).thenReturn(new Service[] { service });
        when(service.getContainer()).thenReturn(engine);
        when(engine.findChildren()).thenReturn(new Container[] { container });
        when(container.findChildren()).thenReturn(new Container[] { childContainer });
        when(this.mbeanServer.isRegistered(capturedName.capture())).thenReturn(true);

        handler.start();

        verify(this.server).addLifecycleListener(same(handler));
        verify(this.eventHandler).init(capturedServer.capture());
        verify(engine).addContainerListener(handler);
        verify(engine).addLifecycleListener(handler);
        verify(container).addContainerListener(handler);
        verify(childContainer).addLifecycleListener(handler);
        verify(this.mbeanServer).addNotificationListener(capturedName.capture(), same(handler), (NotificationFilter) isNull(),
                same(this.server));
        verify(this.eventHandler).start(capturedServer.capture());

        List<ObjectName> names = capturedName.getAllValues();
        assertEquals(2, names.size());
        ObjectName name = names.get(0);
        assertSame(name, names.get(1));
        assertEquals("jboss.web", name.getDomain());
        assertEquals(1, name.getKeyPropertyList().size());
        assertEquals("WebServer", name.getKeyProperty("service"));

        List<CatalinaServer> servers = capturedServer.getAllValues();
        assertSame(this.mbeanServer, servers.get(0).getMBeanServer());
        assertSame(this.mbeanServer, servers.get(1).getMBeanServer());
    }

    @Test
    public void startNoServer() throws JMException {
        ArgumentCaptor<ObjectName> capturedName = ArgumentCaptor.forClass(ObjectName.class);

        CatalinaEventHandler handler = this.createEventHandler(this.eventHandler, this.mbeanServer);

        when(this.mbeanServer.invoke(capturedName.capture(), eq("findServices"), (Object[]) isNull(), (String[]) isNull()))
                .thenThrow(new InstanceNotFoundException());

        handler.start();

        ObjectName name = capturedName.getValue();
        assertEquals("jboss.web", name.getDomain());
        assertEquals(1, name.getKeyPropertyList().size());
        assertEquals("Server", name.getKeyProperty("type"));
    }

    @Test
    public void startAlreadyRegistered() throws Exception {
        Service service = mock(Service.class);
        LifecycleServer server = mock(LifecycleServer.class);
        LifecycleListener listener = mock(LifecycleListener.class);
        ArgumentCaptor<ObjectName> capturedName = ArgumentCaptor.forClass(ObjectName.class);

        CatalinaEventHandler handler = this.createEventHandler(this.eventHandler, this.mbeanServer);

        this.initServer(handler, server);
        this.startServer(handler, server);

        when(this.mbeanServer.invoke(capturedName.capture(), eq("findServices"), (Object[]) isNull(), (String[]) isNull()))
                .thenReturn(new Service[] { service });
        when(service.getServer()).thenReturn(server);
        when(server.findLifecycleListeners()).thenReturn(new LifecycleListener[] { listener, handler });

        handler.start();

        ObjectName name = capturedName.getValue();
        assertEquals("jboss.web", name.getDomain());
        assertEquals(1, name.getKeyPropertyList().size());
        assertEquals("Server", name.getKeyProperty("type"));
    }

    @Test
    public void stop() throws Exception {
        Service service = mock(Service.class);
        LifecycleEngine engine = mock(LifecycleEngine.class);
        Container container = mock(Container.class);
        LifecycleContainer childContainer = mock(LifecycleContainer.class);
        ArgumentCaptor<ObjectName> capturedName = ArgumentCaptor.forClass(ObjectName.class);
        ArgumentCaptor<CatalinaServer> capturedServer = ArgumentCaptor.forClass(CatalinaServer.class);

        CatalinaEventHandler handler = this.createEventHandler(this.eventHandler, this.server, this.mbeanServer);

        this.initServer(handler, this.server);
        this.startServer(handler, this.server);

        when(this.server.findServices()).thenReturn(new Service[] { service });
        when(service.getContainer()).thenReturn(engine);
        when(engine.findChildren()).thenReturn(new Container[] { container });
        when(container.findChildren()).thenReturn(new Container[] { childContainer });
        when(this.mbeanServer.isRegistered(capturedName.capture())).thenReturn(true);

        handler.stop();

        verify(this.server).removeLifecycleListener(same(handler));
        verify(this.eventHandler).stop(capturedServer.capture());
        verify(engine).removeContainerListener(handler);
        verify(engine).removeLifecycleListener(handler);
        verify(container).removeContainerListener(handler);
        verify(childContainer).removeLifecycleListener(handler);
        verify(this.eventHandler).shutdown();
        verify(this.mbeanServer).removeNotificationListener(capturedName.capture(), same(handler));

        List<ObjectName> names = capturedName.getAllValues();
        assertEquals(2, names.size());
        ObjectName name = names.get(0);
        assertSame(name, names.get(1));
        assertEquals("jboss.web", name.getDomain());
        assertEquals(1, name.getKeyPropertyList().size());
        assertEquals("WebServer", name.getKeyProperty("service"));

        CatalinaServer catalinaServer = capturedServer.getValue();
        assertSame(this.mbeanServer, catalinaServer.getMBeanServer());
    }

    @Test
    public void stopNoServer() throws JMException {
        CatalinaEventHandler handler = this.createEventHandler(this.eventHandler, this.server, this.mbeanServer);

        handler.stop();
    }

    @Test
    public void deployWebApp() throws Exception {
        CatalinaEventHandler handler = this.createEventHandler(this.eventHandler, this.server, this.mbeanServer);

        Host host = mock(Host.class);
        LifecycleContext context = mock(LifecycleContext.class);

        ContainerEvent event = new ContainerEvent(host, Container.ADD_CHILD_EVENT, context);

        handler.containerEvent(event);

        verify(context).addLifecycleListener(handler);
        verify(context).addPropertyChangeListener(handler);
        reset(context);

        LifecycleServer server = mock(LifecycleServer.class);

        this.initServer(handler, server);

        handler.containerEvent(event);

        verify(context).addLifecycleListener(handler);
        verify(context).addPropertyChangeListener(handler);
        reset(context);

        this.startServer(handler, server);

        Engine engine = mock(Engine.class);
        Service service = mock(Service.class);
        ArgumentCaptor<CatalinaContext> capturedContext = ArgumentCaptor.forClass(CatalinaContext.class);

        when(context.getParent()).thenReturn(host);
        when(host.getParent()).thenReturn(engine);
        when(engine.getService()).thenReturn(service);
        when(service.getServer()).thenReturn(server);

        handler.containerEvent(event);

        verify(context).addLifecycleListener(handler);
        verify(context).addPropertyChangeListener(handler);
        verify(this.eventHandler).add(capturedContext.capture());

        assertSame(this.mbeanServer, capturedContext.getValue().getHost().getEngine().getServer().getMBeanServer());
    }

    @Test
    public void deployHost() {
        CatalinaEventHandler handler = this.createEventHandler(this.eventHandler, this.server, this.mbeanServer);

        Engine engine = mock(Engine.class);

        ContainerEvent event = new ContainerEvent(engine, Container.ADD_CHILD_EVENT, null);

        handler.containerEvent(event);

        verify(engine).addContainerListener(handler);
    }

    @Test
    public void undeployWebApp() throws Exception {
        CatalinaEventHandler handler = this.createEventHandler(this.eventHandler, this.server, this.mbeanServer);

        Host host = mock(Host.class);
        LifecycleContext context = mock(LifecycleContext.class);

        ContainerEvent event = new ContainerEvent(host, Container.REMOVE_CHILD_EVENT, context);

        handler.containerEvent(event);

        verify(context).removeLifecycleListener(handler);
        verify(context).removePropertyChangeListener(handler);
        reset(context);

        LifecycleServer server = mock(LifecycleServer.class);

        this.initServer(handler, server);

        handler.containerEvent(event);

        verify(context).removeLifecycleListener(handler);
        verify(context).removePropertyChangeListener(handler);
        reset(context);

        this.startServer(handler, server);

        Engine engine = mock(Engine.class);
        Service service = mock(Service.class);
        ArgumentCaptor<CatalinaContext> capturedContext = ArgumentCaptor.forClass(CatalinaContext.class);

        when(context.getParent()).thenReturn(host);
        when(host.getParent()).thenReturn(engine);

        when(engine.getService()).thenReturn(service);
        when(service.getServer()).thenReturn(server);

        handler.containerEvent(event);

        verify(context).removeLifecycleListener(handler);
        verify(context).removePropertyChangeListener(handler);
        verify(this.eventHandler).remove(capturedContext.capture());

        assertSame(this.mbeanServer, capturedContext.getValue().getHost().getEngine().getServer().getMBeanServer());
    }

    @Test
    public void undeployHost() {
        CatalinaEventHandler handler = this.createEventHandler(this.eventHandler, this.server, this.mbeanServer);

        Engine engine = mock(Engine.class);

        ContainerEvent event = new ContainerEvent(engine, Container.REMOVE_CHILD_EVENT, null);

        handler.containerEvent(event);

        verify(engine).removeContainerListener(handler);
    }

    @Test
    public void startWebApp() throws Exception {
        CatalinaEventHandler handler = this.createEventHandler(this.eventHandler, this.server, this.mbeanServer);

        LifecycleContext context = mock(LifecycleContext.class);
        LifecycleEvent event = new LifecycleEvent(context, Lifecycle.START_EVENT, null);
        PropertyChangeEvent prop = new PropertyChangeEvent(context, "available", Boolean.FALSE, Boolean.TRUE);

        handler.lifecycleEvent(event);

        LifecycleServer server = mock(LifecycleServer.class);

        this.initServer(handler, server);

        handler.lifecycleEvent(event);

        this.startServer(handler, server);

        Host host = mock(Host.class);
        Engine engine = mock(Engine.class);
        Service service = mock(Service.class);
        ArgumentCaptor<CatalinaContext> capturedContext = ArgumentCaptor.forClass(CatalinaContext.class);

        when(context.getParent()).thenReturn(host);
        when(host.getParent()).thenReturn(engine);
        when(engine.getService()).thenReturn(service);
        when(service.getServer()).thenReturn(server);

        // handler.lifecycleEvent(event);
        handler.propertyChange(prop);

        verify(this.eventHandler).start(capturedContext.capture());

        assertSame(this.mbeanServer, capturedContext.getValue().getHost().getEngine().getServer().getMBeanServer());
    }

    @Test
    public void initServer() throws Exception {
        CatalinaEventHandler handler = this.createEventHandler(this.eventHandler, this.server, this.mbeanServer);
        LifecycleServer server = mock(LifecycleServer.class);

        this.initServer(handler, server);

        handler.lifecycleEvent(this.createAfterInitEvent(server));
    }

    private void initServer(CatalinaEventHandler handler, LifecycleServer server) throws Exception {
        Service service = mock(Service.class);
        LifecycleEngine engine = mock(LifecycleEngine.class);
        Container container = mock(Container.class);
        LifecycleContainer childContainer = mock(LifecycleContainer.class);
        ArgumentCaptor<ObjectName> capturedName = ArgumentCaptor.forClass(ObjectName.class);
        ArgumentCaptor<CatalinaServer> capturedServer = ArgumentCaptor.forClass(CatalinaServer.class);

        when(server.findServices()).thenReturn(new Service[] { service });
        when(service.getContainer()).thenReturn(engine);
        when(engine.findChildren()).thenReturn(new Container[] { container });
        when(container.findChildren()).thenReturn(new Container[] { childContainer });
        when(this.mbeanServer.isRegistered(capturedName.capture())).thenReturn(true);

        handler.lifecycleEvent(this.createAfterInitEvent(server));

        verify(this.eventHandler).init(capturedServer.capture());
        verify(engine).addContainerListener(handler);
        verify(engine).addLifecycleListener(handler);
        verify(container).addContainerListener(handler);
        verify(childContainer).addLifecycleListener(handler);
        verify(this.mbeanServer).addNotificationListener(capturedName.capture(), same(handler), (NotificationFilter) isNull(), same(server));

        List<ObjectName> names = capturedName.getAllValues();
        assertEquals(2, names.size());
        ObjectName name = names.get(0);
        assertSame(name, names.get(1));
        assertEquals("jboss.web", name.getDomain());
        assertEquals(1, name.getKeyPropertyList().size());
        assertEquals("WebServer", name.getKeyProperty("service"));

        CatalinaServer catalinaServer = capturedServer.getValue();
        assertSame(this.mbeanServer, catalinaServer.getMBeanServer());
    }

    protected LifecycleEvent createAfterInitEvent(Lifecycle lifecycle) {
        return new LifecycleEvent(lifecycle, Lifecycle.INIT_EVENT, null);
    }

    @Test
    public void startServer() throws Exception {
        CatalinaEventHandler handler = this.createEventHandler(this.eventHandler, this.server, this.mbeanServer);
        LifecycleServer server = mock(LifecycleServer.class);

        this.initServer(handler, server);

        this.startServer(handler, server);

        handler.lifecycleEvent(new LifecycleEvent(server, Lifecycle.AFTER_START_EVENT, null));
    }

    private void startServer(CatalinaEventHandler handler, LifecycleServer server) {
        ArgumentCaptor<CatalinaServer> capturedServer = ArgumentCaptor.forClass(CatalinaServer.class);

        handler.lifecycleEvent(new LifecycleEvent(server, Lifecycle.AFTER_START_EVENT, null));

        verify(this.eventHandler).start(capturedServer.capture());

        CatalinaServer catalinaServer = capturedServer.getValue();
        assertSame(this.mbeanServer, catalinaServer.getMBeanServer());
    }

    @Test
    public void stopWebApp() throws Exception {
        CatalinaEventHandler handler = this.createEventHandler(this.eventHandler, this.server, this.mbeanServer);

        LifecycleContext context = mock(LifecycleContext.class);

        LifecycleEvent event = new LifecycleEvent(context, Lifecycle.BEFORE_STOP_EVENT, null);

        handler.lifecycleEvent(event);

        LifecycleServer server = mock(LifecycleServer.class);

        this.initServer(handler, server);

        handler.lifecycleEvent(event);

        this.startServer(handler, server);

        Host host = mock(Host.class);
        Engine engine = mock(Engine.class);
        Service service = mock(Service.class);
        ArgumentCaptor<CatalinaContext> capturedContext = ArgumentCaptor.forClass(CatalinaContext.class);

        when(context.getParent()).thenReturn(host);
        when(host.getParent()).thenReturn(engine);
        when(engine.getService()).thenReturn(service);
        when(service.getServer()).thenReturn(server);

        handler.lifecycleEvent(event);

        verify(this.eventHandler).stop(capturedContext.capture());

        assertSame(this.mbeanServer, capturedContext.getValue().getHost().getEngine().getServer().getMBeanServer());
    }

    @Test
    public void stopServer() throws Exception {
        CatalinaEventHandler handler = this.createEventHandler(this.eventHandler, this.server, this.mbeanServer);

        LifecycleServer server = mock(LifecycleServer.class);
        LifecycleEvent event = new LifecycleEvent(server, Lifecycle.BEFORE_STOP_EVENT, null);
        ArgumentCaptor<CatalinaServer> capturedServer = ArgumentCaptor.forClass(CatalinaServer.class);

        handler.lifecycleEvent(event);

        this.initServer(handler, server);

        handler.lifecycleEvent(event);

        this.startServer(handler, server);

        handler.lifecycleEvent(event);

        verify(this.eventHandler).stop(capturedServer.capture());

        CatalinaServer catalinaServer = capturedServer.getValue();
        assertSame(this.mbeanServer, catalinaServer.getMBeanServer());

        handler.lifecycleEvent(event);
    }

    @Test
    public void destroyServer() throws Exception {
        CatalinaEventHandler handler = this.createEventHandler(this.eventHandler, this.server, this.mbeanServer);

        LifecycleServer server = mock(LifecycleServer.class);
        LifecycleEvent event = new LifecycleEvent(server, Lifecycle.DESTROY_EVENT, null);

        handler.lifecycleEvent(event);

        this.initServer(handler, server);

        Service service = mock(Service.class);
        LifecycleEngine engine = mock(LifecycleEngine.class);
        Container container = mock(Container.class);
        LifecycleContainer childContainer = mock(LifecycleContainer.class);
        ArgumentCaptor<ObjectName> capturedName = ArgumentCaptor.forClass(ObjectName.class);

        when(server.findServices()).thenReturn(new Service[] { service });
        when(service.getContainer()).thenReturn(engine);
        when(engine.findChildren()).thenReturn(new Container[] { container });
        when(container.findChildren()).thenReturn(new Container[] { childContainer });

        when(this.mbeanServer.isRegistered(capturedName.capture())).thenReturn(true);

        handler.lifecycleEvent(event);

        verify(engine).removeContainerListener(handler);
        verify(engine).removeLifecycleListener(handler);
        verify(container).removeContainerListener(handler);
        verify(childContainer).removeLifecycleListener(handler);
        verify(this.eventHandler).shutdown();
        verify(this.mbeanServer).removeNotificationListener(capturedName.capture(), same(handler));

        List<ObjectName> names = capturedName.getAllValues();
        assertEquals(2, names.size());
        ObjectName name = names.get(0);
        assertSame(name, names.get(1));
        assertEquals("jboss.web", name.getDomain());
        assertEquals(1, name.getKeyPropertyList().size());
        assertEquals("WebServer", name.getKeyProperty("service"));

        handler.lifecycleEvent(event);
    }

    @Test
    public void periodicEvent() throws Exception {
        CatalinaEventHandler handler = this.createEventHandler(this.eventHandler, this.server, this.mbeanServer);

        LifecycleEngine engine = mock(LifecycleEngine.class);

        LifecycleEvent event = new LifecycleEvent(engine, Lifecycle.PERIODIC_EVENT, null);

        handler.lifecycleEvent(event);

        LifecycleServer server = mock(LifecycleServer.class);

        this.initServer(handler, server);

        handler.lifecycleEvent(event);

        this.startServer(handler, server);

        Service service = mock(Service.class);
        ArgumentCaptor<CatalinaEngine> capturedEngine = ArgumentCaptor.forClass(CatalinaEngine.class);

        when(engine.getService()).thenReturn(service);
        when(service.getServer()).thenReturn(server);

        handler.lifecycleEvent(event);

        verify(this.eventHandler).status(capturedEngine.capture());

        assertSame(this.mbeanServer, capturedEngine.getValue().getServer().getMBeanServer());
    }

    @Test
    public void handleConnectorsStartedNotification() throws Exception {
        CatalinaEventHandler handler = this.createEventHandler(this.eventHandler, this.server, this.mbeanServer);
        LifecycleServer server = mock(LifecycleServer.class);

        Notification notification = new Notification("jboss.tomcat.connectors.started", new Object(), 1);

        handler.handleNotification(notification, server);

        this.initServer(handler, server);

        handler.handleNotification(notification, server);

        this.startServer(handler, server);

        Service service = mock(Service.class);
        Engine engine = mock(Engine.class);
        ArgumentCaptor<CatalinaEngine> capturedEngine = ArgumentCaptor.forClass(CatalinaEngine.class);

        when(server.findServices()).thenReturn(new Service[] { service });
        when(service.getContainer()).thenReturn(engine);
        when(engine.getService()).thenReturn(service);
        when(service.getServer()).thenReturn(server);

        handler.handleNotification(notification, server);

        verify(this.eventHandler).status(capturedEngine.capture());

        assertSame(this.mbeanServer, capturedEngine.getValue().getServer().getMBeanServer());
    }

    @Test
    public void handleConnectorsStoppedNotification() throws Exception {
        CatalinaEventHandler handler = this.createEventHandler(this.eventHandler, this.server, this.mbeanServer);
        LifecycleServer server = mock(LifecycleServer.class);

        Notification notification = new Notification("jboss.tomcat.connectors.stopped", new Object(), 1);

        handler.handleNotification(notification, server);

        this.initServer(handler, server);

        handler.handleNotification(notification, server);

        this.startServer(handler, server);

        ArgumentCaptor<CatalinaServer> capturedServer = ArgumentCaptor.forClass(CatalinaServer.class);

        handler.handleNotification(notification, server);

        verify(this.eventHandler).stop(capturedServer.capture());

        assertSame(this.mbeanServer, capturedServer.getValue().getMBeanServer());
    }

    @Test
    public void handleOtherNotification() {
        CatalinaEventHandler handler = this.createEventHandler(this.eventHandler, this.server, this.mbeanServer);
        LifecycleServer server = mock(LifecycleServer.class);

        Notification notification = new Notification("blah", new Object(), 1);

        handler.handleNotification(notification, server);
    }

    interface LifecycleContext extends Lifecycle, Context {
    }

    interface LifecycleServer extends Lifecycle, Server {
    }

    interface LifecycleEngine extends Lifecycle, Engine {
    }

    interface LifecycleContainer extends Lifecycle, Container {
    }
}
