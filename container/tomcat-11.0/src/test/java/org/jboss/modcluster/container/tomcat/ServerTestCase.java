/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.container.tomcat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Iterator;

import org.apache.catalina.Service;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link TomcatServer}.
 *
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
class ServerTestCase {
    protected final org.apache.catalina.Server server = mock(org.apache.catalina.Server.class);
    protected final Server catalinaServer = new TomcatServer(this.registry, server);

    protected final TomcatRegistry registry = mock(TomcatRegistry.class);
    protected org.apache.catalina.Server serverMock = mock(org.apache.catalina.Server.class);
    protected Service serviceMock = mock(Service.class);
    protected org.apache.catalina.Engine engineMock = mock(org.apache.catalina.Engine.class);
    protected org.apache.catalina.Host hostMock = mock(org.apache.catalina.Host.class);

    @BeforeEach
    void before() {
        when(this.serviceMock.getServer()).thenReturn(this.serverMock);
        when(this.engineMock.getService()).thenReturn(this.serviceMock);
        when(this.hostMock.getParent()).thenReturn(this.engineMock);
    }

    @Test
    void getEngines() {
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
