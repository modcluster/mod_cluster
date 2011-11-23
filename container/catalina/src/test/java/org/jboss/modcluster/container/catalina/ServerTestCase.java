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
package org.jboss.modcluster.container.catalina;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.util.Iterator;

import javax.management.MBeanServer;

import org.apache.catalina.Service;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Server;
import org.junit.Test;

/**
 * @author Paul Ferraro
 * 
 */
public class ServerTestCase {
    private org.apache.catalina.Server server = mock(org.apache.catalina.Server.class);
    private MBeanServer mbeanServer = mock(MBeanServer.class);

    private Server catalinaServer = this.createServer(this.server, this.mbeanServer);

    protected Server createServer(org.apache.catalina.Server server, MBeanServer mbeanServer) {
        return new CatalinaServer(this.server, this.mbeanServer);
    }
    
    @Test
    public void getEngines() {
        Service service = mock(Service.class);
        org.apache.catalina.Engine engine = mock(org.apache.catalina.Engine.class);

        when(this.server.findServices()).thenReturn(new Service[] { service });

        Iterable<Engine> result = this.catalinaServer.getEngines();

        Iterator<Engine> engines = result.iterator();

        assertTrue(engines.hasNext());

        when(service.getContainer()).thenReturn(engine);

        engines.next();

        assertFalse(engines.hasNext());
    }

    @Test
    public void getMBeanServer() {
        MBeanServer result = this.catalinaServer.getMBeanServer();

        assertSame(this.mbeanServer, result);
    }

    @Test
    public void getDomain() {
        when(this.mbeanServer.getDefaultDomain()).thenReturn("domain");

        String result = this.catalinaServer.getDomain();

        assertEquals("domain", result);

        DomainServer server = mock(DomainServer.class);
        Server catalinaServer = new CatalinaServer(server, this.mbeanServer);

        when(server.getDomain()).thenReturn("domain");

        result = catalinaServer.getDomain();

        assertEquals("domain", result);
    }

    private interface DomainServer extends org.apache.catalina.Server {
        String getDomain();
    }
}
