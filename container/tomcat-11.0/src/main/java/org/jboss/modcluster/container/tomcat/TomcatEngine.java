/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.container.tomcat;

import java.util.Arrays;
import java.util.Iterator;

import org.apache.catalina.Container;
import org.apache.catalina.util.SessionConfig;
import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Host;
import org.jboss.modcluster.container.Server;

/**
 * {@link Engine} implementation that wraps a {@link org.apache.catalina.Context}.
 *
 * @author Paul Ferraro
 */
public class TomcatEngine implements Engine {
    protected final TomcatRegistry registry;
    protected final org.apache.catalina.Engine engine;
    protected final Server server;

    /**
     * Constructs a new CatalinaEngine that wraps the specified catalina engine
     *
     * @param engine a catalina engine
     */
    public TomcatEngine(TomcatRegistry registry, org.apache.catalina.Engine engine) {
        this.registry = registry;
        this.engine = engine;
        this.server = new TomcatServer(registry, engine.getService().getServer());
    }

    @Override
    public Server getServer() {
        return this.server;
    }

    @Override
    public Iterable<Host> getHosts() {
        final Iterator<Container> children = Arrays.asList(this.engine.findChildren()).iterator();

        final Iterator<Host> hosts = new Iterator<Host>() {
            @Override
            public boolean hasNext() {
                return children.hasNext();
            }

            @Override
            public Host next() {
                return new TomcatHost(registry, (org.apache.catalina.Host) children.next());
            }

            @Override
            public void remove() {
                children.remove();
            }
        };

        return new Iterable<Host>() {
            @Override
            public Iterator<Host> iterator() {
                return hosts;
            }
        };
    }

    @Override
    public String getDefaultHost() {
        return this.engine.getDefaultHost();
    }

    @Override
    public String getJvmRoute() {
        return this.engine.getJvmRoute();
    }

    @Override
    public void setJvmRoute(String jvmRoute) {
        this.engine.setJvmRoute(jvmRoute);
    }

    @Override
    public String getName() {
        return this.engine.getName();
    }

    @Override
    public Iterable<Connector> getConnectors() {
        final Iterator<org.apache.catalina.connector.Connector> connectors = Arrays.asList(this.engine.getService().findConnectors()).iterator();

        final Iterator<Connector> iterator = new Iterator<Connector>() {
            @Override
            public boolean hasNext() {
                return connectors.hasNext();
            }

            @Override
            public Connector next() {
                return new TomcatConnector(connectors.next());
            }

            @Override
            public void remove() {
                connectors.remove();
            }
        };

        return new Iterable<Connector>() {
            @Override
            public Iterator<Connector> iterator() {
                return iterator;
            }
        };
    }

    @Override
    public Connector getProxyConnector() {
        return this.registry.getProxyConnectorProvider().createProxyConnector(this.engine);
    }

    @Override
    public Host findHost(String name) {
        org.apache.catalina.Host host = (org.apache.catalina.Host) this.engine.findChild(name);

        return (host != null) ? new TomcatHost(registry, host) : null;
    }

    // TODO MODCLUSTER-477 Broken design: cookie-name should be specified on the Context level instead of only on the Engine level
    @Override
    public String getSessionCookieName() {
        return SessionConfig.getSessionCookieName(null);
    }

    // TODO MODCLUSTER-477 Broken design: cookie-name should be specified on the Context level instead of only on the Engine level
    @Override
    public String getSessionParameterName() {
        return SessionConfig.getSessionUriParamName(null);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof TomcatEngine)) return false;

        TomcatEngine engine = (TomcatEngine) object;

        return this.engine == engine.engine;
    }

    @Override
    public int hashCode() {
        return this.engine.hashCode();
    }

    @Override
    public String toString() {
        return this.engine.getName();
    }
}
