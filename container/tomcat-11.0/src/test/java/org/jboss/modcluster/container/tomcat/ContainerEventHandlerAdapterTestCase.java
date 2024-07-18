/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.container.tomcat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.beans.PropertyChangeEvent;

import org.apache.catalina.Container;
import org.apache.catalina.ContainerEvent;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Service;
import org.jboss.modcluster.container.ContainerEventHandler;
import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Server;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Test case for {@link TomcatEventHandlerAdapter}.
 *
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public class ContainerEventHandlerAdapterTestCase {
    protected final TomcatRegistry registry = mock(TomcatRegistry.class);
    protected final ContainerEventHandler eventHandler = mock(ContainerEventHandler.class);
    protected final LifecycleServer server = mock(LifecycleServer.class);
    protected final ServerProvider provider = mock(ServerProvider.class);

    protected TomcatEventHandler createEventHandler() {
        return new TomcatEventHandlerAdapter(this.eventHandler, this.provider, this.registry);
    }

    @After
    public void init() {
        Mockito.reset(this.eventHandler, this.server, this.provider);
    }

    @Test
    public void start() {
        Service service = mock(Service.class);
        LifecycleListener listener = mock(LifecycleListener.class);
        LifecycleEngine engine = mock(LifecycleEngine.class);
        Container container = mock(Container.class);
        LifecycleContainer childContainer = mock(LifecycleContainer.class);
        Server server = new TomcatServer(registry, this.server);

        TomcatEventHandler handler = new TomcatEventHandlerAdapter(eventHandler, provider, registry);

        when(this.provider.getServer()).thenReturn(this.server);
        when(this.server.findLifecycleListeners()).thenReturn(new LifecycleListener[] { listener });
        when(this.server.findServices()).thenReturn(new Service[] { service });
        when(service.getContainer()).thenReturn(engine);
        when(engine.findChildren()).thenReturn(new Container[] { container });
        when(container.findChildren()).thenReturn(new Container[] { childContainer });

        handler.start();

        verify(this.server).addLifecycleListener(same(handler));
        verify(this.eventHandler).init(eq(server));
        verify(engine).addContainerListener(handler);
        verify(engine).addLifecycleListener(handler);
        verify(container).addContainerListener(handler);
        verify(childContainer).addLifecycleListener(handler);
        verify(childContainer).addPropertyChangeListener(handler);
        verify(this.eventHandler).start(eq(server));
    }

    @Test
    public void stop() throws Exception {
        Server server = new TomcatServer(registry, this.server);
        Service service = mock(Service.class);
        LifecycleEngine engine = mock(LifecycleEngine.class);
        Container container = mock(Container.class);
        LifecycleContainer childContainer = mock(LifecycleContainer.class);

        TomcatEventHandler handler = this.createEventHandler();

        this.initServer(handler, this.server);
        this.startServer(handler, this.server);

        when(this.provider.getServer()).thenReturn(this.server);
        when(this.server.findServices()).thenReturn(new Service[] { service });
        when(service.getContainer()).thenReturn(engine);
        when(engine.findChildren()).thenReturn(new Container[] { container });
        when(container.findChildren()).thenReturn(new Container[] { childContainer });

        handler.stop();

        verify(this.server).removeLifecycleListener(same(handler));
        verify(this.eventHandler).stop(eq(server));
        verify(engine).removeContainerListener(handler);
        verify(engine).removeLifecycleListener(handler);
        verify(container).removeContainerListener(handler);
        verify(childContainer).removeLifecycleListener(handler);
        verify(childContainer).removePropertyChangeListener(handler);
        verify(this.eventHandler).shutdown();
    }

    @Test
    public void deployWebApp() throws Exception {
        TomcatEventHandler handler = this.createEventHandler();

        org.apache.catalina.Host host = mock(org.apache.catalina.Host.class);
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

        org.apache.catalina.Engine engine = mock(org.apache.catalina.Engine.class);
        Service service = mock(Service.class);

        when(context.getParent()).thenReturn(host);
        when(host.getParent()).thenReturn(engine);
        when(engine.getService()).thenReturn(service);
        when(service.getServer()).thenReturn(server);

        Context catalinaContext = new TomcatContext(registry, context);

        handler.containerEvent(event);

        verify(context).addLifecycleListener(handler);
        verify(context).addPropertyChangeListener(handler);

        LifecycleEvent lifecycleEvent = new LifecycleEvent(context, Lifecycle.AFTER_START_EVENT, context);
        handler.lifecycleEvent(lifecycleEvent);
        verify(this.eventHandler).start(eq(catalinaContext));
    }

    @Test
    public void deployHost() {
        TomcatEventHandler handler = this.createEventHandler();

        org.apache.catalina.Engine engine = mock(org.apache.catalina.Engine.class);

        ContainerEvent event = new ContainerEvent(engine, Container.ADD_CHILD_EVENT, null);

        handler.containerEvent(event);

        verify(engine).addContainerListener(handler);
    }

    @Test
    public void undeployWebApp() throws Exception {
        TomcatEventHandler handler = this.createEventHandler();

        org.apache.catalina.Host host = mock(org.apache.catalina.Host.class);
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

        org.apache.catalina.Engine engine = mock(org.apache.catalina.Engine.class);
        Service service = mock(Service.class);

        when(context.getParent()).thenReturn(host);
        when(host.getParent()).thenReturn(engine);

        when(engine.getService()).thenReturn(service);
        when(service.getServer()).thenReturn(server);
        Context catalinaContext = new TomcatContext(registry, context);

        handler.containerEvent(event);

        verify(context).removeLifecycleListener(handler);
        verify(context).removePropertyChangeListener(handler);
        verify(this.eventHandler).remove(eq(catalinaContext));
    }

    @Test
    public void undeployHost() {
        TomcatEventHandler handler = this.createEventHandler();

        org.apache.catalina.Engine engine = mock(org.apache.catalina.Engine.class);

        ContainerEvent event = new ContainerEvent(engine, Container.REMOVE_CHILD_EVENT, null);

        handler.containerEvent(event);

        verify(engine).removeContainerListener(handler);
    }

    @Test
    public void startWebApp() throws Exception {
        TomcatEventHandler handler = this.createEventHandler();

        LifecycleContext context = mock(LifecycleContext.class);
        LifecycleEvent event = new LifecycleEvent(context, Lifecycle.START_EVENT, null);
        PropertyChangeEvent prop = new PropertyChangeEvent(context, "available", Boolean.FALSE, Boolean.TRUE);

        handler.lifecycleEvent(event);

        LifecycleServer server = mock(LifecycleServer.class);

        this.initServer(handler, server);

        handler.lifecycleEvent(event);

        this.startServer(handler, server);

        org.apache.catalina.Host host = mock(org.apache.catalina.Host.class);
        org.apache.catalina.Engine engine = mock(org.apache.catalina.Engine.class);
        Service service = mock(Service.class);

        when(context.getParent()).thenReturn(host);
        when(host.getParent()).thenReturn(engine);
        when(engine.getService()).thenReturn(service);
        when(service.getServer()).thenReturn(server);

        Context catalinaContext = new TomcatContext(registry, context);

        handler.propertyChange(prop);

        verify(this.eventHandler).start(eq(catalinaContext));
    }

    @Test
    public void initServer() throws Exception {
        TomcatEventHandler handler = this.createEventHandler();
        LifecycleServer server = mock(LifecycleServer.class);

        this.initServer(handler, server);

        handler.lifecycleEvent(this.createAfterInitEvent(server));

        verifyNoInteractions(this.eventHandler);
    }

    protected void initServer(TomcatEventHandler handler, LifecycleServer server) {
        Service service = mock(Service.class);
        LifecycleEngine engine = mock(LifecycleEngine.class);
        Container container = mock(Container.class);
        LifecycleContainer childContainer = mock(LifecycleContainer.class);

        when(server.findServices()).thenReturn(new Service[] { service });
        when(service.getContainer()).thenReturn(engine);
        when(engine.findChildren()).thenReturn(new Container[] { container });
        when(container.findChildren()).thenReturn(new Container[] { childContainer });

        handler.lifecycleEvent(this.createAfterInitEvent(server));

        verify(this.eventHandler).init(notNull());
        verify(engine).addContainerListener(handler);
        verify(engine).addLifecycleListener(handler);
        verify(container).addContainerListener(handler);
        verify(childContainer).addLifecycleListener(handler);

        reset(this.eventHandler);
    }

    protected LifecycleEvent createAfterInitEvent(Lifecycle lifecycle) {
        return new LifecycleEvent(lifecycle, Lifecycle.AFTER_INIT_EVENT, null);
    }

    protected LifecycleEvent createBeforeDestroyInitEvent(Lifecycle lifecycle) {
        return new LifecycleEvent(lifecycle, Lifecycle.BEFORE_DESTROY_EVENT, null);
    }

    @Test
    public void initServerFromService() throws Exception {
        TomcatEventHandler handler = this.createEventHandler();
        LifecycleService service = mock(LifecycleService.class);

        this.initServerFromService(handler, service);

        handler.lifecycleEvent(this.createAfterInitEvent(service));

        verifyNoInteractions(this.eventHandler);
    }

    protected void initServerFromService(TomcatEventHandler handler, LifecycleService service) {
        org.apache.catalina.Server server = mock(org.apache.catalina.Server.class);
        LifecycleEngine engine = mock(LifecycleEngine.class);
        Container container = mock(Container.class);
        LifecycleContainer childContainer = mock(LifecycleContainer.class);

        when(service.getServer()).thenReturn(server);
        when(service.getName()).thenReturn("Catalina");
        when(server.findServices()).thenReturn(new Service[] { service });
        when(service.getContainer()).thenReturn(engine);
        when(engine.findChildren()).thenReturn(new Container[] { container });
        when(container.findChildren()).thenReturn(new Container[] { childContainer });

        Server catalinaServer = new TomcatServer(registry, new SingleServiceServer(service));

        handler.lifecycleEvent(this.createAfterInitEvent(service));

        verify(this.eventHandler).init(refEq(catalinaServer));
        verify(engine).addContainerListener(handler);
        verify(engine).addLifecycleListener(handler);
        verify(container).addContainerListener(handler);
        verify(childContainer).addLifecycleListener(handler);

        reset(this.eventHandler);
    }

    @Test
    public void startServer() throws Exception {
        TomcatEventHandler handler = this.createEventHandler();
        LifecycleServer server = mock(LifecycleServer.class);

        this.initServer(handler, server);

        this.startServer(handler, server);

        handler.lifecycleEvent(new LifecycleEvent(server, Lifecycle.AFTER_START_EVENT, null));

        verifyNoInteractions(this.eventHandler);
    }

    protected void startServer(TomcatEventHandler handler, LifecycleServer server) {
        Server catalinaServer = new TomcatServer(registry, server);

        handler.lifecycleEvent(new LifecycleEvent(server, Lifecycle.AFTER_START_EVENT, null));

        verify(this.eventHandler).start(eq(catalinaServer));
        reset(this.eventHandler);
    }

    @Test
    public void startServerFromService() throws Exception {
        TomcatEventHandler handler = this.createEventHandler();
        LifecycleService service = mock(LifecycleService.class);

        this.initServerFromService(handler, service);

        this.startServerFromService(handler, service);

        handler.lifecycleEvent(new LifecycleEvent(service, Lifecycle.AFTER_START_EVENT, null));

        verifyNoInteractions(this.eventHandler);
    }

    protected void startServerFromService(TomcatEventHandler handler, LifecycleService service) {
        Server catalinaServer = new TomcatServer(registry, new SingleServiceServer(service));

        handler.lifecycleEvent(new LifecycleEvent(service, Lifecycle.AFTER_START_EVENT, null));

        verify(this.eventHandler).start(refEq(catalinaServer));
        reset(this.eventHandler);
    }

    @Test
    public void stopWebApp() throws Exception {
        TomcatEventHandler handler = this.createEventHandler();

        LifecycleContext context = mock(LifecycleContext.class);

        LifecycleEvent event = new LifecycleEvent(context, Lifecycle.BEFORE_STOP_EVENT, null);

        handler.lifecycleEvent(event);

        LifecycleServer server = mock(LifecycleServer.class);

        this.initServer(handler, server);

        handler.lifecycleEvent(event);

        this.startServer(handler, server);

        org.apache.catalina.Host host = mock(org.apache.catalina.Host.class);
        org.apache.catalina.Engine engine = mock(org.apache.catalina.Engine.class);
        Service service = mock(Service.class);

        when(context.getParent()).thenReturn(host);
        when(host.getParent()).thenReturn(engine);
        when(engine.getService()).thenReturn(service);
        when(service.getServer()).thenReturn(server);

        Context catalinaContext = new TomcatContext(registry, context);

        handler.lifecycleEvent(event);

        verify(this.eventHandler).stop(eq(catalinaContext));
    }

    @Test
    public void stopServer() throws Exception {
        TomcatEventHandler handler = this.createEventHandler();

        LifecycleServer server = mock(LifecycleServer.class);
        LifecycleEvent event = new LifecycleEvent(server, Lifecycle.BEFORE_STOP_EVENT, null);

        handler.lifecycleEvent(event);

        verifyNoInteractions(this.eventHandler);

        this.initServer(handler, server);

        handler.lifecycleEvent(event);

        verifyNoInteractions(this.eventHandler);

        this.startServer(handler, server);

        Server catalinaServer = new TomcatServer(registry, server);

        handler.lifecycleEvent(event);

        verify(this.eventHandler).stop(eq(catalinaServer));
        reset(this.eventHandler);

        handler.lifecycleEvent(event);

        verifyNoInteractions(this.eventHandler);
    }

    @Test
    public void stopServerFromService() throws Exception {
        TomcatEventHandler handler = this.createEventHandler();

        LifecycleService service = mock(LifecycleService.class);
        LifecycleEvent event = new LifecycleEvent(service, Lifecycle.BEFORE_STOP_EVENT, null);

        handler.lifecycleEvent(event);

        verifyNoInteractions(this.eventHandler);

        this.initServerFromService(handler, service);

        handler.lifecycleEvent(event);

        verifyNoInteractions(this.eventHandler);

        this.startServerFromService(handler, service);

        Server catalinaServer = new TomcatServer(registry, new SingleServiceServer(service));

        handler.lifecycleEvent(event);

        verify(this.eventHandler).stop(refEq(catalinaServer));
        reset(this.eventHandler);

        handler.lifecycleEvent(event);

        verifyNoInteractions(this.eventHandler);
    }

    @Test
    public void destroyServer() throws Exception {
        TomcatEventHandler handler = this.createEventHandler();

        LifecycleServer server = mock(LifecycleServer.class);
        LifecycleEvent event = createBeforeDestroyInitEvent(server);

        handler.lifecycleEvent(event);

        verifyNoInteractions(this.eventHandler);

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

        verifyNoInteractions(this.eventHandler);
    }

    @Test
    public void destroyServerFromService() throws Exception {
        TomcatEventHandler handler = this.createEventHandler();

        LifecycleService service = mock(LifecycleService.class);
        org.apache.catalina.Server server = mock(org.apache.catalina.Server.class);
        LifecycleEvent event = createBeforeDestroyInitEvent(service);

        handler.lifecycleEvent(event);

        verifyNoInteractions(this.eventHandler);

        this.initServerFromService(handler, service);

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

        verifyNoInteractions(this.eventHandler);
    }

    @Test
    public void periodicEvent() throws Exception {
        TomcatEventHandler handler = this.createEventHandler();

        LifecycleEngine engine = mock(LifecycleEngine.class);

        LifecycleEvent event = new LifecycleEvent(engine, Lifecycle.PERIODIC_EVENT, null);

        handler.lifecycleEvent(event);

        verifyNoInteractions(this.eventHandler);

        LifecycleServer server = mock(LifecycleServer.class);

        this.initServer(handler, server);

        handler.lifecycleEvent(event);

        verifyNoInteractions(this.eventHandler);

        this.startServer(handler, server);

        Service service = mock(Service.class);

        when(engine.getService()).thenReturn(service);
        when(service.getServer()).thenReturn(server);
        Engine catalinaEngine = new TomcatEngine(null, engine);

        handler.lifecycleEvent(event);

        verify(this.eventHandler).status(eq(catalinaEngine));
    }

    protected interface LifecycleContext extends Lifecycle, org.apache.catalina.Context {
    }

    protected interface LifecycleServer extends Lifecycle, org.apache.catalina.Server {
    }

    protected interface LifecycleService extends Lifecycle, Service {
    }

    protected interface LifecycleEngine extends Lifecycle, org.apache.catalina.Engine {
    }

    protected interface LifecycleContainer extends Lifecycle, Container {
    }
}
