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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

import java.util.Iterator;

import org.apache.catalina.Service;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Server;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class ServerTestCase {
    protected final TomcatFactoryRegistry registry = mock(TomcatFactoryRegistry.class);
    protected final org.apache.catalina.Server server = mock(org.apache.catalina.Server.class);

    protected final Server catalinaServer = this.createServer(this.server);

    protected Server createServer(org.apache.catalina.Server server) {
        return new TomcatServer(this.registry, server);
    }
    
    @Test
    public void getEngines() {
        Service service = mock(Service.class);
        org.apache.catalina.Engine engine = mock(org.apache.catalina.Engine.class);
        EngineFactory engineFactory = mock(EngineFactory.class);
        Engine expected = mock(Engine.class);
        
        when(this.server.findServices()).thenReturn(new Service[] { service });
        when(service.getContainer()).thenReturn(engine);
        when(this.registry.getEngineFactory()).thenReturn(engineFactory);
        when(engineFactory.createEngine(same(this.registry), same(engine), same(this.catalinaServer))).thenReturn(expected);
        Iterable<Engine> result = this.catalinaServer.getEngines();

        Iterator<Engine> engines = result.iterator();

        assertTrue(engines.hasNext());
        assertSame(expected, engines.next());
        assertFalse(engines.hasNext());
    }
}
