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

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.jboss.modcluster.container.ContainerEventHandler;
import org.jboss.modcluster.container.catalina.CatalinaEventHandler;
import org.jboss.modcluster.container.catalina.CatalinaFactory;
import org.jboss.modcluster.container.catalina.ServerProvider;

import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
public class ContainerEventHandlerAdapterTestCase extends org.jboss.modcluster.container.catalina.ContainerEventHandlerAdapterTestCase {
    
    @Override
    protected CatalinaEventHandler createEventHandler(ContainerEventHandler eventHandler, ServerProvider provider, CatalinaFactory factory) {
        return new TomcatEventHandlerAdapter(eventHandler, provider, factory);
    }

    @Override
    protected LifecycleEvent createAfterInitEvent(Lifecycle lifecycle) {
        return new LifecycleEvent(lifecycle, Lifecycle.AFTER_INIT_EVENT, null);
    }
    @Override
    protected LifecycleEvent createBeforeDestroyInitEvent(Lifecycle lifecycle) {
        return new LifecycleEvent(lifecycle, Lifecycle.BEFORE_DESTROY_EVENT, null);
    }

    /* From catalina ContainerEventHandlerAdapterTestCase.java */
    @Test
    public void start() {
        super.start();
    }
    @Test
    public void stop() throws Exception {
        super.stop();
    }
    @Test
    public void deployWebApp() throws Exception {
        super.deployWebApp();
    }
    @Test
    public void deployHost() {
        super.deployHost();
    }
    @Test
    public void undeployWebApp() throws Exception {
        super.undeployWebApp();
    }
    @Test
    public void undeployHost() {
        super.undeployHost();
    }
    @Test
    public void startWebApp() throws Exception {
        super.startWebApp();
    }
    @Test
    public void initServer() throws Exception {
        super.initServer();
    }
    @Test
    public void startServer() throws Exception {
        super.startServer();
    }
    @Test
    public void stopWebApp() throws Exception {
        super.stopWebApp();
    }
    @Test
    public void stopServer() throws Exception {
        super.stopServer();
    }
    @Test
    public void destroyServer() throws Exception {
        super.destroyServer();
    }
    @Test
    public void periodicEvent() throws Exception {
        super.periodicEvent();
    }
    
}
