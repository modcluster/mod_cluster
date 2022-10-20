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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Iterator;

import org.apache.catalina.Service;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Server;
import org.junit.Before;
import org.junit.Test;

/**
 * Test case for {@link TomcatServer}.
 *
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public class ServerTestCase {
    protected final org.apache.catalina.Server server = mock(org.apache.catalina.Server.class);
    protected final Server catalinaServer = new TomcatServer(this.registry, server);

    protected final TomcatRegistry registry = mock(TomcatRegistry.class);
    protected org.apache.catalina.Server serverMock = mock(org.apache.catalina.Server.class);
    protected Service serviceMock = mock(Service.class);
    protected org.apache.catalina.Engine engineMock = mock(org.apache.catalina.Engine.class);
    protected org.apache.catalina.Host hostMock = mock(org.apache.catalina.Host.class);

    @Before
    public void before() {
        when(this.serviceMock.getServer()).thenReturn(this.serverMock);
        when(this.engineMock.getService()).thenReturn(this.serviceMock);
        when(this.hostMock.getParent()).thenReturn(this.engineMock);
    }

    @Test
    public void getEngines() {
        Service service = mock(Service.class);
        Engine expected = new TomcatEngine(registry, engineMock);

        when(this.server.findServices()).thenReturn(new Service[] { service });
        when(service.getContainer()).thenReturn(engineMock);
        Iterable<Engine> result = this.catalinaServer.getEngines();

        Iterator<Engine> engines = result.iterator();

        assertTrue(engines.hasNext());
        assertEquals(expected, engines.next());
        assertFalse(engines.hasNext());
    }
}
