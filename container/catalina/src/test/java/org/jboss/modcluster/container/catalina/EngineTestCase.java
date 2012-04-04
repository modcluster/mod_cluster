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

import java.util.Iterator;

import org.apache.catalina.Container;
import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Host;
import org.jboss.modcluster.container.Server;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Paul Ferraro
 * 
 */
public class EngineTestCase {
    protected final CatalinaFactoryRegistry registry = mock(CatalinaFactoryRegistry.class);
    protected final org.apache.catalina.Engine engine = mock(org.apache.catalina.Engine.class);
    protected final Server server = mock(Server.class);

    protected final Engine catalinaEngine = this.createEngine();

    protected Engine createEngine() {
        return new CatalinaEngine(this.registry, this.engine, this.server);
    }

    @Test
    public void findHost() {
        org.apache.catalina.Host host = mock(org.apache.catalina.Host.class);
        HostFactory hostFactory = mock(HostFactory.class);
        Host expected = mock(Host.class);
        
        when(this.engine.findChild("host")).thenReturn(host);
        when(this.registry.getHostFactory()).thenReturn(hostFactory);
        when(hostFactory.createHost(same(this.registry), same(host), same(this.catalinaEngine))).thenReturn(expected);
        
        Host result = this.catalinaEngine.findHost("host");

        assertSame(expected, result);
    }

    @Test
    public void getHosts() {
        org.apache.catalina.Host host = mock(org.apache.catalina.Host.class);
        HostFactory hostFactory = mock(HostFactory.class);
        Host expected = mock(Host.class);

        when(this.engine.findChildren()).thenReturn(new Container[] { host });
        when(this.registry.getHostFactory()).thenReturn(hostFactory);
        when(hostFactory.createHost(same(this.registry), same(host), same(this.catalinaEngine))).thenReturn(expected);

        Iterable<Host> result = this.catalinaEngine.getHosts();

        Iterator<Host> hosts = result.iterator();
        assertTrue(hosts.hasNext());
        assertSame(expected, hosts.next());
        assertFalse(hosts.hasNext());
    }

    @Test
    public void getJvmRoute() {
        String expected = "route";

        when(this.engine.getJvmRoute()).thenReturn(expected);

        String result = this.catalinaEngine.getJvmRoute();

        assertSame(expected, result);
    }

    @Test
    public void getName() {
        String expected = "name";

        when(this.engine.getName()).thenReturn(expected);

        String result = this.catalinaEngine.getName();

        assertSame(expected, result);
    }

    @Test
    public void getProxyConnector() throws Exception {
        ProxyConnectorProvider provider = mock(ProxyConnectorProvider.class);
        Connector expected = mock(Connector.class);
        ConnectorFactory factory = mock(ConnectorFactory.class);
        
        when(this.registry.getProxyConnectorProvider()).thenReturn(provider);
        when(this.registry.getConnectorFactory()).thenReturn(factory);
        when(provider.createProxyConnector(factory,  this.engine)).thenReturn(expected);
        
        Connector result = this.catalinaEngine.getProxyConnector();

        assertSame(expected, result);
    }

    @Test
    public void setJvmRoute() {
        this.catalinaEngine.setJvmRoute("route");

        verify(this.engine).setJvmRoute("route");
    }

    @Test
    public void getServer() {
        Server result = this.catalinaEngine.getServer();

        assertSame(this.server, result);
    }
    
    @Test
    public void getSessionCookieName() {
        assertEquals("JSESSIONID", this.catalinaEngine.getSessionCookieName());
    }
    
    @Test
    public void getSessionParameterName() {
        assertEquals("jsessionid", this.catalinaEngine.getSessionParameterName());
    }
}
