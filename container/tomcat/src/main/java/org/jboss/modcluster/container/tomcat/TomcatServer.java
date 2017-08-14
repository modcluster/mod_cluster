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
    protected final TomcatFactoryRegistry registry;
    protected final org.apache.catalina.Server server;

    /**
     * Constructs a new CatalinaServer wrapping the specified catalina server.
     *
     * @param host a catalina server
     */
    public TomcatServer(TomcatFactoryRegistry registry, org.apache.catalina.Server server) {
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
                return TomcatServer.this.registry.getEngineFactory().createEngine(TomcatServer.this.registry, (org.apache.catalina.Engine) services.next().getContainer(), TomcatServer.this);
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
        if ((object == null) || !(object instanceof TomcatServer)) return false;

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
