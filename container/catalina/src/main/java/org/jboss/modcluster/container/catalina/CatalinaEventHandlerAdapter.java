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
package org.jboss.modcluster.container.catalina;

import java.beans.PropertyChangeEvent;
import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
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
import org.apache.catalina.connector.Connector;
import org.jboss.modcluster.container.ContainerEventHandler;

/**
 * Adapts lifecycle and container listener events to the {@link ContainerEventHandler} interface.
 */
public class CatalinaEventHandlerAdapter implements CatalinaEventHandler {

    private static final int STATUS_FREQUENCY = Integer.parseInt(System.getProperty("org.jboss.modcluster.container.catalina.status-frequency", "1"));

    protected final ContainerEventHandler eventHandler;
    protected final ServerProvider serverProvider;
    protected final CatalinaFactory factory;

    // Flags used to ignore redundant or invalid events
    protected final AtomicBoolean init = new AtomicBoolean(false);
    protected final AtomicBoolean start = new AtomicBoolean(false);

    private volatile int statusCount = 0;

    /**
     * Constructs a new CatalinaEventHandlerAdapter using the specified event handler.
     * 
     * @param eventHandler an event handler
     */
    public CatalinaEventHandlerAdapter(ContainerEventHandler eventHandler) {
        this(eventHandler, ManagementFactory.getPlatformMBeanServer());
    }

    public CatalinaEventHandlerAdapter(ContainerEventHandler eventHandler, MBeanServer mbeanServer) {
        this(eventHandler, new JMXServerProvider(mbeanServer, toObjectName("Catalina:type=Server")), new AutoProxyConnectorProvider());
    }

    public CatalinaEventHandlerAdapter(ContainerEventHandler eventHandler, Server server) {
        this(eventHandler, new SimpleServerProvider(server), new AutoProxyConnectorProvider());
    }

    public CatalinaEventHandlerAdapter(ContainerEventHandler eventHandler, Server server, Connector connector) {
        this(eventHandler, new SimpleServerProvider(server), new SimpleProxyConnectorProvider(connector));
    }

    public CatalinaEventHandlerAdapter(ContainerEventHandler eventHandler, ServerProvider serverProvider, ProxyConnectorProvider connectorProvider) {
        this(eventHandler, serverProvider, new ServiceLoaderCatalinaFactory(connectorProvider));
    }

    public CatalinaEventHandlerAdapter(ContainerEventHandler eventHandler, ServerProvider serverProvider, CatalinaFactory factory) {
        this.eventHandler = eventHandler;
        this.serverProvider = serverProvider;
        this.factory = factory;
    }

    @Override
    public void start() {
        Server server = this.serverProvider.getServer();

        if (!(server instanceof Lifecycle)) throw new IllegalStateException();

        Lifecycle lifecycle = (Lifecycle) server;

        if (!this.containsListener(lifecycle)) {
            lifecycle.addLifecycleListener(this);
        }

        if (this.init.compareAndSet(false, true)) {
            this.init(server);
        }

        if (this.start.compareAndSet(false, true)) {
            this.eventHandler.start(this.factory.createServer(server));
        }
    }

    @Override
    public void stop() {
        Server server = this.serverProvider.getServer();

        if (!(server instanceof Lifecycle)) throw new IllegalStateException();

        Lifecycle lifecycle = (Lifecycle) server;

        lifecycle.removeLifecycleListener(this);

        if (this.init.get() && this.start.compareAndSet(true, false)) {
            this.eventHandler.stop(this.factory.createServer(server));
        }

        if (this.init.compareAndSet(true, false)) {
            this.destroy(server);
        }
    }

    private boolean containsListener(Lifecycle lifecycle) {
        for (LifecycleListener listener: lifecycle.findLifecycleListeners()) {
            if (listener.equals(this)) {
                return true;
            }
        }

        return false;
    }

    // ------------------------------------------------------------- Properties

    // ---------------------------------------------- LifecycleListener Methods

    /**
     * {@inhericDoc} Acknowledge the occurrence of the specified event. Note: Will never be called when the listener is
     * associated to a Server, since it is not a Container.
     * 
     * @see org.apache.catalina.ContainerListener#containerEvent(org.apache.catalina.ContainerEvent)
     */
    @Override
    public void containerEvent(ContainerEvent event) {
        Container container = event.getContainer();
        Object child = event.getData();
        String type = event.getType();

        if (type.equals(Container.ADD_CHILD_EVENT)) {
            if (container instanceof Host) {
                // Deploying a webapp
                ((Lifecycle) child).addLifecycleListener(this);
                ((Container) child).addPropertyChangeListener(this);

                if (this.start.get()) {
                    this.eventHandler.add(this.factory.createContext((Context) child));
                }
            } else if (container instanceof Engine) {
                // Deploying a host
                container.addContainerListener(this);

                if (child != null) {
                    ((Host) child).addContainerListener(this);
                }
            }
        } else if (type.equals(Container.REMOVE_CHILD_EVENT)) {
            if (container instanceof Host) {
                // Undeploying a webapp
                ((Lifecycle) child).removeLifecycleListener(this);
                ((Container) child).removePropertyChangeListener(this);

                if (this.start.get()) {
                    this.eventHandler.remove(this.factory.createContext((Context) child));
                }
            } else if (container instanceof Engine) {
                // Undeploying a host
                if (child != null) {
                    ((Host) child).removeContainerListener(this);
                }

                container.removeContainerListener(this);
            }
        }
    }

    /**
     * {@inhericDoc} Primary entry point for startup and shutdown events.
     * 
     * @see org.apache.catalina.LifecycleListener#lifecycleEvent(org.apache.catalina.LifecycleEvent)
     */
    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        Lifecycle source = event.getLifecycle();
        String type = event.getType();

        if (isAfterInit(event)) {
            if (source instanceof Server) {
                if (this.init.compareAndSet(false, true)) {
                    Server server = (Server) source;
                    init(server);
                }
            }
        } else if (type.equals(Lifecycle.START_EVENT)) {
            if (source instanceof Server) {
                if (this.init.compareAndSet(false, true)) {
                    Server server = (Server) source;
                    init(server);
                }
            }
        } else if (type.equals(Lifecycle.AFTER_START_EVENT)) {
            if (source instanceof Server) {
                if (this.init.get() && this.start.compareAndSet(false, true)) {
                    this.eventHandler.start(this.factory.createServer((Server) source));
                }
            }
        } else if (type.equals(Lifecycle.BEFORE_STOP_EVENT)) {
            if (source instanceof Context) {
                if (this.start.get()) {
                    // Stop a webapp
                    this.eventHandler.stop(this.factory.createContext((Context) source));
                }
            } else if (source instanceof Server) {
                if (this.init.get() && this.start.compareAndSet(true, false)) {
                    this.eventHandler.stop(this.factory.createServer((Server) source));
                }
            }
        } else if (isBeforeDestroy(event)) {
            if (source instanceof Server) {
                if (this.init.compareAndSet(true, false)) {
                    this.destroy((Server) source);
                }
            }
        } else if (type.equals(Lifecycle.PERIODIC_EVENT)) {
            if (source instanceof Engine) {
                Engine engine = (Engine) source;
                this.statusCount = (this.statusCount + 1) % STATUS_FREQUENCY;
                if (this.statusCount == 0) {
                    if (this.start.get()) {
                        this.eventHandler.status(this.factory.createEngine(engine));
                    }
                }
            }
        }
    }

    protected boolean isAfterInit(LifecycleEvent event) {
        return false;
    }

    protected boolean isBeforeDestroy(LifecycleEvent event) {
        return false;
    }

    /**
     * initialize server stuff: in jbossweb-2.1.x the server can't be destroyed so you could start (restart) one that needs
     * initializations...
     */
    protected void init(Server server) {
        this.eventHandler.init(this.factory.createServer(server));

        this.addListeners(server);
    }

    protected void destroy(Server server) {
        this.removeListeners(server);

        this.eventHandler.shutdown();
    }

    private void addListeners(Server server) {
        // Register ourself as a listener for child services
        for (Service service : server.findServices()) {
            Container engine = service.getContainer();
            engine.addContainerListener(this);
            ((Lifecycle) engine).addLifecycleListener(this);

            for (Container host : engine.findChildren()) {
                host.addContainerListener(this);

                for (Container context : host.findChildren()) {
                    ((Lifecycle) context).addLifecycleListener(this);
                    context.addPropertyChangeListener(this);
                }
            }
        }
    }

    private void removeListeners(Server server) {
        // Unregister ourself as a listener to child components
        for (Service service : server.findServices()) {
            Container engine = service.getContainer();
            engine.removeContainerListener(this);
            ((Lifecycle) engine).removeLifecycleListener(this);

            for (Container host : engine.findChildren()) {
                host.removeContainerListener(this);

                for (Container context : host.findChildren()) {
                    ((Lifecycle) context).removeLifecycleListener(this);
                    context.removePropertyChangeListener(this);
                }
            }
        }
    }

    protected static ObjectName toObjectName(String name) {
        try {
            return ObjectName.getInstance(name);
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getSource() instanceof Context && "available".equals(event.getPropertyName())
                && Boolean.FALSE.equals(event.getOldValue()) && Boolean.TRUE.equals(event.getNewValue())) {
            this.eventHandler.start(this.factory.createContext((Context) event.getSource()));
        }
    }
}
