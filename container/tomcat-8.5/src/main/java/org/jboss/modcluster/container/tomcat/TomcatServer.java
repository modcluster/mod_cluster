/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.container.tomcat;

import java.util.Arrays;
import java.util.Iterator;

import org.apache.catalina.Service;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Server;

/**
 * {@link Server} implementation that wraps a {@link org.apache.catalina.Server}.
 *
 * @author Paul Ferraro
 */
public class TomcatServer implements Server {
    protected final TomcatRegistry registry;
    protected final org.apache.catalina.Server server;

    /**
     * Constructs a new {@link org.apache.catalina.Server} wrapping the specified catalina server.
     *
     * @param server a catalina server
     */
    public TomcatServer(TomcatRegistry registry, org.apache.catalina.Server server) {
        this.registry = registry;
        this.server = server;
    }

    @Override
    public Iterable<Engine> getEngines() {
        final Iterator<Service> services = Arrays.asList(this.server.findServices()).iterator();

        final Iterator<Engine> engines = new Iterator<Engine>() {
            @Override
            public boolean hasNext() {
                return services.hasNext();
            }

            @Override
            public Engine next() {
                return new TomcatEngine(registry, services.next().getContainer());
            }

            @Override
            public void remove() {
                services.remove();
            }
        };

        return new Iterable<Engine>() {
            @Override
            public Iterator<Engine> iterator() {
                return engines;
            }
        };
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof TomcatServer)) return false;

        TomcatServer server = (TomcatServer) object;

        return this.server == server.server;
    }

    @Override
    public int hashCode() {
        return this.server.hashCode();
    }

    @Override
    public String toString() {
        return this.server.getClass().getName();
    }
}
