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

import org.apache.catalina.Service;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.tomcat.TomcatFactoryRegistry;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Needs to recompile against Tomcat 8.5/9 Jars due to {@link Service#getContainer()} signature change.
 *
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public class TomcatServer extends org.jboss.modcluster.container.tomcat.TomcatServer {

    public TomcatServer(TomcatFactoryRegistry registry, org.apache.catalina.Server server) {
        super(registry, server);
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
                return TomcatServer.this.registry.getEngineFactory().createEngine(TomcatServer.this.registry, services.next().getContainer(), TomcatServer.this);
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
}
