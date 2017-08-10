/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.modcluster.container.tomcat85;

import org.apache.catalina.Container;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Service;
import org.jboss.modcluster.container.ContainerEventHandler;
import org.jboss.modcluster.container.Server;
import org.jboss.modcluster.container.tomcat.ContainerEventHandlerAdapterTestCase;
import org.jboss.modcluster.container.tomcat.ServerProvider;
import org.jboss.modcluster.container.tomcat.TomcatEventHandler;
import org.jboss.modcluster.container.tomcat.TomcatFactory;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Paul Ferraro
 * @author Radoslav Husar
 * @version May 2016
 */
public class TomcatEventHandlerAdapterTestCase extends ContainerEventHandlerAdapterTestCase {

    @Override
    protected TomcatEventHandler createEventHandler(ContainerEventHandler eventHandler, ServerProvider provider, TomcatFactory factory) {
        return new org.jboss.modcluster.container.tomcat85.TomcatEventHandlerAdapter(eventHandler, provider, factory);
    }

    @Override
    protected void initServer(TomcatEventHandler handler, LifecycleServer server) {
        Service service = mock(Service.class);
        LifecycleEngine engine = mock(LifecycleEngine.class);
        Container container = mock(Container.class);
        LifecycleContainer childContainer = mock(LifecycleContainer.class);
        Server catalinaServer = mock(Server.class);

        when(server.findServices()).thenReturn(new Service[] { service });
        when(service.getContainer()).thenReturn(engine);
        when(engine.findChildren()).thenReturn(new Container[] { container });
        when(container.findChildren()).thenReturn(new Container[] { childContainer });
        when(this.factory.createServer(server)).thenReturn(catalinaServer);

        handler.lifecycleEvent(this.createAfterInitEvent(server));

        verify(this.eventHandler).init(same(catalinaServer));
        verify(engine).addContainerListener(handler);
        verify(engine).addLifecycleListener(handler);
        verify(container).addContainerListener(handler);
        verify(childContainer).addLifecycleListener(handler);

        reset(this.eventHandler);
    }


    @Override
    public void start() {
        Service service = mock(Service.class);
        LifecycleListener listener = mock(LifecycleListener.class);
        LifecycleEngine engine = mock(LifecycleEngine.class);
        Container container = mock(Container.class);
        LifecycleContainer childContainer = mock(LifecycleContainer.class);
        Server server = mock(Server.class);

        TomcatEventHandler handler = this.createEventHandler(this.eventHandler, this.provider, this.factory);

        when(this.provider.getServer()).thenReturn(this.server);
        when(this.server.findLifecycleListeners()).thenReturn(new LifecycleListener[] { listener });
        when(this.server.findServices()).thenReturn(new Service[] { service });
        when(service.getContainer()).thenReturn(engine);
        when(engine.findChildren()).thenReturn(new Container[] { container });
        when(container.findChildren()).thenReturn(new Container[] { childContainer });
        when(this.factory.createServer(same(this.server))).thenReturn(server);

        handler.start();

        verify(this.server).addLifecycleListener(same(handler));
        verify(this.eventHandler).init(same(server));
        verify(engine).addContainerListener(handler);
        verify(engine).addLifecycleListener(handler);
        verify(container).addContainerListener(handler);
        verify(childContainer).addLifecycleListener(handler);
        verify(childContainer).addPropertyChangeListener(handler);
        verify(this.eventHandler).start(same(server));
    }

    @Override
    public void stop() throws Exception {
        Server server = mock(Server.class);
        Service service = mock(Service.class);
        LifecycleEngine engine = mock(LifecycleEngine.class);
        Container container = mock(Container.class);
        LifecycleContainer childContainer = mock(LifecycleContainer.class);

        TomcatEventHandler handler = this.createEventHandler(this.eventHandler, this.provider, this.factory);

        this.initServer(handler, this.server);
        this.startServer(handler, this.server);

        when(this.provider.getServer()).thenReturn(this.server);
        when(this.server.findServices()).thenReturn(new Service[] { service });
        when(service.getContainer()).thenReturn(engine);
        when(engine.findChildren()).thenReturn(new Container[] { container });
        when(container.findChildren()).thenReturn(new Container[] { childContainer });
        when(this.factory.createServer(this.server)).thenReturn(server);

        handler.stop();

        verify(this.server).removeLifecycleListener(same(handler));
        verify(this.eventHandler).stop(same(server));
        verify(engine).removeContainerListener(handler);
        verify(engine).removeLifecycleListener(handler);
        verify(container).removeContainerListener(handler);
        verify(childContainer).removeLifecycleListener(handler);
        verify(childContainer).removePropertyChangeListener(handler);
        verify(this.eventHandler).shutdown();
    }

    @Override
    public void destroyServer() throws Exception {
        TomcatEventHandler handler = this.createEventHandler(this.eventHandler, this.provider, this.factory);

        LifecycleServer server = mock(LifecycleServer.class);
        LifecycleEvent event = createBeforeDestroyInitEvent(server);

        handler.lifecycleEvent(event);

        Mockito.verifyZeroInteractions(this.eventHandler);

        this.initServer(handler, server);

        Service service = mock(Service.class);
        LifecycleEngine engine = mock(LifecycleEngine.class);
        Container container = mock(Container.class);
        LifecycleContainer childContainer = mock(LifecycleContainer.class);

        when(server.findServices()).thenReturn(new Service[] { service });
        when(service.getContainer()).thenReturn(engine);
        when(engine.findChildren()).thenReturn(new Container[] { container });
        when(container.findChildren()).thenReturn(new Container[] { childContainer });

        handler.lifecycleEvent(event);

        verify(engine).removeContainerListener(handler);
        verify(engine).removeLifecycleListener(handler);
        verify(container).removeContainerListener(handler);
        verify(childContainer).removeLifecycleListener(handler);
        verify(this.eventHandler).shutdown();
        reset(this.eventHandler);

        handler.lifecycleEvent(event);

        Mockito.verifyZeroInteractions(this.eventHandler);
    }
}
