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
package org.jboss.modcluster.container.jbossweb;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.ObjectName;

import org.apache.catalina.Container;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.Service;
import org.jboss.modcluster.container.ContainerEventHandler;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Server;
import org.jboss.modcluster.container.catalina.CatalinaEventHandler;
import org.jboss.modcluster.container.catalina.CatalinaFactory;
import org.jboss.modcluster.container.catalina.ServerProvider;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;


/**
 * @author Paul Ferraro
 *
 */
public class ContainerEventHandlerAdapterTestCase extends org.jboss.modcluster.container.catalina.ContainerEventHandlerAdapterTestCase {
    private final MBeanServer mbeanServer = mock(MBeanServer.class);

    @Override
    protected CatalinaEventHandler createEventHandler(ContainerEventHandler eventHandler, ServerProvider provider, CatalinaFactory factory) {
        return new JBossWebEventHandlerAdapter(eventHandler, provider, factory);
    }
    
    @Override
    public void initServer() throws Exception {
        JBossWebEventHandlerAdapter handler = new JBossWebEventHandlerAdapter(this.eventHandler, this.mbeanServer, this.provider, this.factory);
        LifecycleServer server = mock(LifecycleServer.class);
        Service service = mock(Service.class);
        LifecycleEngine engine = mock(LifecycleEngine.class);
        Container container = mock(Container.class);
        LifecycleContainer childContainer = mock(LifecycleContainer.class);
        Server catalinaServer = mock(Server.class);
        ArgumentCaptor<ObjectName> capturedName = ArgumentCaptor.forClass(ObjectName.class);
        
        when(server.findServices()).thenReturn(new Service[] { service });
        when(service.getContainer()).thenReturn(engine);
        when(engine.findChildren()).thenReturn(new Container[] { container });
        when(container.findChildren()).thenReturn(new Container[] { childContainer });
        when(this.factory.createServer(server)).thenReturn(catalinaServer);
        
        when(this.mbeanServer.isRegistered(capturedName.capture())).thenReturn(true);
        
        handler.lifecycleEvent(this.createAfterInitEvent(server));

        ObjectName name = capturedName.getValue();
        assertEquals("jboss.web", name.getDomain());
        assertEquals(1, name.getKeyPropertyList().size());
        assertEquals("WebServer", name.getKeyProperty("service"));
        
        verify(this.eventHandler).init(same(catalinaServer));
        verify(engine).addContainerListener(handler);
        verify(engine).addLifecycleListener(handler);
        verify(container).addContainerListener(handler);
        verify(childContainer).addLifecycleListener(handler);
        verify(this.mbeanServer).addNotificationListener(same(name), same(handler), (NotificationFilter) Matchers.isNull(), same(server));
        
        reset(this.eventHandler);

        handler.lifecycleEvent(this.createAfterInitEvent(server));
    }

    @Override
    public void destroyServer() throws Exception {
        JBossWebEventHandlerAdapter handler = new JBossWebEventHandlerAdapter(this.eventHandler, this.mbeanServer, this.provider, this.factory);

        LifecycleServer server = mock(LifecycleServer.class);
        LifecycleEvent event = new LifecycleEvent(server, Lifecycle.DESTROY_EVENT, null);

        handler.lifecycleEvent(event);

        Mockito.verifyZeroInteractions(this.eventHandler);

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

        ObjectName name = capturedName.getValue();
        assertEquals("jboss.web", name.getDomain());
        assertEquals(1, name.getKeyPropertyList().size());
        assertEquals("WebServer", name.getKeyProperty("service"));
        
        verify(engine).removeContainerListener(handler);
        verify(engine).removeLifecycleListener(handler);
        verify(container).removeContainerListener(handler);
        verify(childContainer).removeLifecycleListener(handler);
        verify(this.eventHandler).shutdown();
        verify(this.mbeanServer).removeNotificationListener(same(name), same(handler));
        reset(this.eventHandler);
        
        handler.lifecycleEvent(event);
        
        Mockito.verifyZeroInteractions(this.eventHandler);
    }

    @Test
    public void handleConnectorsStartedNotification() throws Exception {
        JBossWebEventHandlerAdapter handler = new JBossWebEventHandlerAdapter(this.eventHandler, this.mbeanServer, this.provider, this.factory);
        LifecycleServer server = mock(LifecycleServer.class);

        Notification notification = new Notification("jboss.tomcat.connectors.started", new Object(), 1);

        handler.handleNotification(notification, server);

        this.initServer(handler, server);

        handler.handleNotification(notification, server);

        this.startServer(handler, server);

        Service service = mock(Service.class);
        org.apache.catalina.Engine engine = mock(org.apache.catalina.Engine.class);
        Engine catalinaEngine = mock(Engine.class);

        when(server.findServices()).thenReturn(new Service[] { service });
        when(service.getContainer()).thenReturn(engine);
        when(this.factory.createEngine(same(engine))).thenReturn(catalinaEngine);

        handler.handleNotification(notification, server);

        verify(this.eventHandler).status(same(catalinaEngine));
    }

    @Test
    public void handleConnectorsStoppedNotification() throws Exception {
        JBossWebEventHandlerAdapter handler = new JBossWebEventHandlerAdapter(this.eventHandler, this.mbeanServer, this.provider, this.factory);
        LifecycleServer server = mock(LifecycleServer.class);

        Notification notification = new Notification("jboss.tomcat.connectors.stopped", new Object(), 1);

        handler.handleNotification(notification, server);

        this.initServer(handler, server);

        handler.handleNotification(notification, server);

        this.startServer(handler, server);

        Server catalinaServer = mock(Server.class);

        when(this.factory.createServer(same(server))).thenReturn(catalinaServer);
        
        handler.handleNotification(notification, server);

        verify(this.eventHandler).stop(same(catalinaServer));
    }

    @Test
    public void handleOtherNotification() {
        JBossWebEventHandlerAdapter handler = new JBossWebEventHandlerAdapter(this.eventHandler, this.mbeanServer, this.provider, this.factory);
        LifecycleServer server = mock(LifecycleServer.class);

        Notification notification = new Notification("blah", new Object(), 1);

        handler.handleNotification(notification, server);
    }
}
