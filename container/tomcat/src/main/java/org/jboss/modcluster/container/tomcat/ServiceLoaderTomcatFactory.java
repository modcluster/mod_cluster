/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.modcluster.container.tomcat;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ServiceLoader;

import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Host;
import org.jboss.modcluster.container.Server;

/**
 * @author Paul Ferraro
 */
public class ServiceLoaderTomcatFactory implements TomcatFactory, TomcatFactoryRegistry {
    private final ServerFactory serverFactory;
    private final EngineFactory engineFactory;
    private final HostFactory hostFactory;
    private final ContextFactory contextFactory;
    private final ConnectorFactory connectorFactory;
    private final ProxyConnectorProvider provider;

    private static <T> T load(final Class<T> targetClass, final Class<? extends T> defaultClass) {
        PrivilegedAction<T> action = new PrivilegedAction<T>() {
            @Override
            public T run() {
                for (T value : ServiceLoader.load(targetClass, targetClass.getClassLoader())) {
                    return value;
                }
                try {
                    return defaultClass.newInstance();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        };
        return AccessController.doPrivileged(action);
    }

    public ServiceLoaderTomcatFactory(ProxyConnectorProvider provider) {
        this.serverFactory = load(ServerFactory.class, TomcatServerFactory.class);
        this.engineFactory = load(EngineFactory.class, TomcatEngineFactory.class);
        this.hostFactory = load(HostFactory.class, TomcatHostFactory.class);
        this.contextFactory = load(ContextFactory.class, TomcatContextFactory.class);
        this.connectorFactory = load(ConnectorFactory.class, TomcatConnectorFactory.class);
        this.provider = provider;
    }

    public ServiceLoaderTomcatFactory(ServerFactory serverFactory, EngineFactory engineFactory, HostFactory hostFactory, ContextFactory contextFactory, ConnectorFactory connectorFactory, ProxyConnectorProvider provider) {
        this.serverFactory = serverFactory;
        this.engineFactory = engineFactory;
        this.hostFactory = hostFactory;
        this.contextFactory = contextFactory;
        this.connectorFactory = connectorFactory;
        this.provider = provider;
    }
    
    @Override
    public Server createServer(org.apache.catalina.Server server) {
        return this.serverFactory.createServer(this, server);
    }
    
    @Override
    public Engine createEngine(org.apache.catalina.Engine engine) {
        return this.engineFactory.createEngine(this, engine, this.createServer(engine.getService().getServer()));
    }
    
    @Override
    public Host createHost(org.apache.catalina.Host host) {
        return this.hostFactory.createHost(this, host, this.createEngine((org.apache.catalina.Engine) host.getParent()));
    }
    
    @Override
    public Context createContext(org.apache.catalina.Context context) {
        return this.contextFactory.createContext(context, this.createHost((org.apache.catalina.Host) context.getParent()));
    }

    @Override
    public ProxyConnectorProvider getProxyConnectorProvider() {
        return this.provider;
    }

    @Override
    public ServerFactory getServerFactory() {
        return this.serverFactory;
    }

    @Override
    public EngineFactory getEngineFactory() {
        return this.engineFactory;
    }

    @Override
    public HostFactory getHostFactory() {
        return this.hostFactory;
    }

    @Override
    public ContextFactory getContextFactory() {
        return this.contextFactory;
    }

    @Override
    public ConnectorFactory getConnectorFactory() {
        return this.connectorFactory;
    }
}
