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

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.Server;
import org.jboss.modcluster.container.ContainerEventHandler;
import org.jboss.modcluster.container.catalina.CatalinaEventHandlerAdapter;
import org.jboss.modcluster.container.catalina.CatalinaFactory;
import org.jboss.modcluster.container.catalina.ServerProvider;

public class TomcatEventHandlerAdapter extends CatalinaEventHandlerAdapter {

    public TomcatEventHandlerAdapter(ContainerEventHandler eventHandler) {
        super(eventHandler);
    }

    public TomcatEventHandlerAdapter(ContainerEventHandler eventHandler, Server server) {
        super(eventHandler, server);
    }

    public TomcatEventHandlerAdapter(ContainerEventHandler eventHandler, ServerProvider provider) {
        super(eventHandler, provider);
    }

    public TomcatEventHandlerAdapter(ContainerEventHandler eventHandler, ServerProvider provider, CatalinaFactory factory) {
        super(eventHandler, provider, factory);
    }

    @Override
    protected boolean isAfterInit(LifecycleEvent event) {
        return event.getType().equals(Lifecycle.INIT_EVENT);
    }
    @Override
    protected boolean isBeforeDestroy(LifecycleEvent event) {
        return event.getType().equals(Lifecycle.DESTROY_EVENT);
    }
}
