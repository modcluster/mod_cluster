/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.container.tomcat;

import java.util.Iterator;

import org.apache.catalina.Container;
import org.apache.catalina.Service;
import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Host;
import org.jboss.modcluster.container.Server;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test case for {@link TomcatEngine}.
 *
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public class EngineTestCase {

    protected Server server;
    protected Engine engine;

    protected final TomcatRegistry registry = mock(TomcatRegistry.class);
    protected final org.apache.catalina.Server serverMock = mock(org.apache.catalina.Server.class);
    protected final Service serviceMock = mock(Service.class);
    protected final org.apache.catalina.Engine engineMock = mock(org.apache.catalina.Engine.class);
    protected final org.apache.catalina.Host hostMock = mock(org.apache.catalina.Host.class);

    @Before
    public void before() {
        when(this.serviceMock.getServer()).thenReturn(this.serverMock);
        when(this.engineMock.getService()).thenReturn(this.serviceMock);
        when(this.hostMock.getParent()).thenReturn(this.engineMock);

        this.engine = new TomcatEngine(this.registry, this.engineMock);
        this.server = new TomcatServer(registry, serverMock);
    }

    @Test
    public void getDefaultHost() {
        String expected = "localhost";
        when(this.engineMock.getDefaultHost()).thenReturn(expected);

        String result = this.engine.getDefaultHost();

        assertSame(expected, result);
    }

    @Test
    public void findHost() {
        Host expected = new TomcatHost(registry, hostMock);

        when(this.engineMock.findChild("host")).thenReturn(hostMock);

        Host result = this.engine.findHost("host");

        assertEquals(expected, result);
    }

    @Test
    public void getHosts() {
        Host expected = new TomcatHost(registry, hostMock);

        when(this.engineMock.findChildren()).thenReturn(new Container[] { hostMock });

        Iterable<Host> result = this.engine.getHosts();

        Iterator<Host> hosts = result.iterator();
        assertTrue(hosts.hasNext());
        assertEquals(expected, hosts.next());
        assertFalse(hosts.hasNext());
    }

    @Test
    public void getJvmRoute() {
        String expected = "route";

        when(this.engineMock.getJvmRoute()).thenReturn(expected);

        String result = this.engine.getJvmRoute();

        assertSame(expected, result);
    }

    @Test
    public void getName() {
        String expected = "name";

        when(this.engineMock.getName()).thenReturn(expected);

        String result = this.engine.getName();

        assertSame(expected, result);
    }

    @Test
    public void getProxyConnector() throws Exception {
        ProxyConnectorProvider provider = mock(ProxyConnectorProvider.class);
        Connector expected = mock(Connector.class);

        when(this.registry.getProxyConnectorProvider()).thenReturn(provider);
        when(provider.createProxyConnector(this.engineMock)).thenReturn(expected);

        Connector result = this.engine.getProxyConnector();

        assertEquals(expected, result);
    }

    @Test
    public void setJvmRoute() {
        this.engine.setJvmRoute("route");

        verify(this.engineMock).setJvmRoute("route");
    }

    @Test
    public void getServer() {
        Server result = this.engine.getServer();

        assertEquals(this.server, result);
    }

    @Test
    public void getSessionCookieName() {
        assertEquals("JSESSIONID", this.engine.getSessionCookieName());
    }

    @Test
    public void getSessionParameterName() {
        assertEquals("jsessionid", this.engine.getSessionParameterName());
    }
}
