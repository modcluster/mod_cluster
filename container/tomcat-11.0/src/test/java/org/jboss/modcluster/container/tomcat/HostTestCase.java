/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.container.tomcat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Iterator;
import java.util.Set;

import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Host;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link TomcatHost}.
 *
 * @author Paul Ferraro
 */
class HostTestCase {
    protected final TomcatRegistry registry = mock(TomcatRegistry.class);
    protected final org.apache.catalina.Host host = mock(org.apache.catalina.Host.class);
    protected Engine engine;
    protected Host catalinaHost;

    @BeforeEach
    void setup() {
        Service serviceMock = mock(Service.class);
        when(serviceMock.getServer()).thenReturn(mock(Server.class));

        org.apache.catalina.Engine engineMock = mock(org.apache.catalina.Engine.class);
        when(engineMock.getService()).thenReturn(serviceMock);

        when(this.host.getParent()).thenReturn(engineMock);

        engine = new TomcatEngine(registry, engineMock);
        catalinaHost = new TomcatHost(this.registry, this.host);
    }

    @Test
    void getAliases() {
        when(this.host.getName()).thenReturn("host");
        when(this.host.findAliases()).thenReturn(new String[] { "alias" });

        Set<String> result = this.catalinaHost.getAliases();

        assertEquals(2, result.size());

        Iterator<String> aliases = result.iterator();
        assertEquals("host", aliases.next());
        assertEquals("alias", aliases.next());
    }

    @Test
    void getEngine() {
        Engine result = this.catalinaHost.getEngine();

        assertEquals(this.engine, result);
    }

    @Test
    void getName() {
        String expected = "name";

        when(this.host.getName()).thenReturn(expected);

        String result = this.catalinaHost.getName();

        assertSame(expected, result);
    }
}
