/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

import javax.management.MBeanServer;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.Server;
import org.jboss.modcluster.container.ContainerEventHandler;
import org.jboss.modcluster.container.catalina.CatalinaEventHandlerAdapter;


/**
 * @author Paul Ferraro
 *
 */
public class ContainerEventHandlerAdapterTestCase extends org.jboss.modcluster.container.catalina.ContainerEventHandlerAdapterTestCase {
    
    @Override
    protected CatalinaEventHandlerAdapter createEventHandler(ContainerEventHandler eventHandler, Server server, MBeanServer mbeanServer) {
        return new TomcatEventHandlerAdapter(eventHandler, server, mbeanServer);
    }
    
    @Override
    protected CatalinaEventHandlerAdapter createEventHandler(ContainerEventHandler eventHandler, MBeanServer mbeanServer) {
        return new TomcatEventHandlerAdapter(eventHandler, mbeanServer);
    }

    @Override
    protected LifecycleEvent createAfterInitEvent(Lifecycle lifecycle) {
        return new LifecycleEvent(lifecycle, Lifecycle.AFTER_INIT_EVENT, null);
    }
}
