/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.jboss.modcluster.container.tomcat8;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Manager;
import org.apache.catalina.SessionIdGenerator;
import org.jboss.modcluster.container.Server;
import org.jboss.modcluster.container.tomcat.TomcatFactoryRegistry;

/**
 * @author Radoslav Husar
 */
public class TomcatEngine extends org.jboss.modcluster.container.tomcat.TomcatEngine {

    public TomcatEngine(TomcatFactoryRegistry registry, Engine engine, Server server) {
        super(registry, engine, server);
    }

    /**
     * Propagates jvm-route configuration to contexts.
     *
     * @see <a href="https://issues.jboss.org/browse/MODCLUSTER-469">MODCLUSTER-469</a>
     */
    @Override
    public void setJvmRoute(String jvmRoute) {
        super.setJvmRoute(jvmRoute);

        for (Container hostAsContainer : this.engine.findChildren()) {
            Host host = (Host) hostAsContainer;
            for (Container contextAsContainer : host.findChildren()) {
                Context context = (Context) contextAsContainer;
                Manager contextManager = context.getManager();
                if (contextManager != null) {
                    SessionIdGenerator sessionIdGenerator = contextManager.getSessionIdGenerator();
                    if (sessionIdGenerator != null) {
                        sessionIdGenerator.setJvmRoute(jvmRoute);
                    }
                }
            }
        }
    }
}
