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
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.jboss.modcluster.container.ContainerEventHandler;
import org.jboss.modcluster.container.tomcat.ServerProvider;
import org.jboss.modcluster.container.tomcat.TomcatFactory;

/**
 * @author Paul Ferraro
 * @author Radoslav Husar
 * @version Mar 2016
 */
public class TomcatEventHandlerAdapter extends org.jboss.modcluster.container.tomcat.TomcatEventHandlerAdapter {

    public TomcatEventHandlerAdapter(ContainerEventHandler eventHandler) {
        super(eventHandler);
    }

    public TomcatEventHandlerAdapter(ContainerEventHandler eventHandler, ServerProvider serverProvider, TomcatFactory factory) {
        super(eventHandler, serverProvider, factory);
    }

    @Override
    protected void addListeners(Server server) {
        // Register ourself as a listener for child services
        for (Service service : server.findServices()) {
            Container engine = service.getContainer();
            engine.addContainerListener(this);
            engine.addLifecycleListener(this);

            for (Container host : engine.findChildren()) {
                host.addContainerListener(this);

                for (Container context : host.findChildren()) {
                    context.addLifecycleListener(this);
                    context.addPropertyChangeListener(this);
                }
            }
        }
    }

    @Override
    protected void removeListeners(Server server) {
        // Unregister ourself as a listener to child components
        for (Service service : server.findServices()) {
            Container engine = service.getContainer();
            engine.removeContainerListener(this);
            engine.removeLifecycleListener(this);

            for (Container host : engine.findChildren()) {
                host.removeContainerListener(this);

                for (Container context : host.findChildren()) {
                    context.removeLifecycleListener(this);
                    context.removePropertyChangeListener(this);
                }
            }
        }
    }
}
