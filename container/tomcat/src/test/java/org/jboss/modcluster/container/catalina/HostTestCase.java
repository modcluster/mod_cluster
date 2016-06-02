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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Iterator;
import java.util.Set;

import org.apache.catalina.Container;
import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Host;
import org.jboss.modcluster.container.tomcat.TomcatFactoryRegistry;
import org.jboss.modcluster.container.tomcat.TomcatHost;
import org.jboss.modcluster.container.tomcat.ContextFactory;
import org.junit.Test;

/**
 * @author Paul Ferraro
 * 
 */
public class HostTestCase {
    protected final TomcatFactoryRegistry registry = mock(TomcatFactoryRegistry.class);
    protected final org.apache.catalina.Host host = mock(org.apache.catalina.Host.class);
    protected final Engine engine = mock(Engine.class);

    protected final Host catalinaHost = this.createHost();

    protected Host createHost() {
        return new TomcatHost(this.registry, this.host, this.engine);
    }
    
    @Test
    public void findContext() {
        org.apache.catalina.Context context = mock(org.apache.catalina.Context.class);
        Context expected = mock(Context.class);
        ContextFactory contextFactory = mock(ContextFactory.class);
        
        when(this.host.findChild("path")).thenReturn(context);
        when(this.registry.getContextFactory()).thenReturn(contextFactory);
        when(contextFactory.createContext(same(context), same(this.catalinaHost))).thenReturn(expected);
        
        Context result = this.catalinaHost.findContext("path");

        assertSame(expected, result);
    }

    @Test
    public void getAliases() {
        when(this.host.getName()).thenReturn("host");
        when(this.host.findAliases()).thenReturn(new String[] { "alias" });

        Set<String> result = this.catalinaHost.getAliases();

        assertEquals(2, result.size());

        Iterator<String> aliases = result.iterator();
        assertEquals("host", aliases.next());
        assertEquals("alias", aliases.next());
    }

    @Test
    public void getContexts() {
        org.apache.catalina.Context context = mock(org.apache.catalina.Context.class);
        Context expected = mock(Context.class);
        ContextFactory contextFactory = mock(ContextFactory.class);

        when(this.host.findChildren()).thenReturn(new Container[] { context });
        when(this.registry.getContextFactory()).thenReturn(contextFactory);
        when(contextFactory.createContext(same(context), same(this.catalinaHost))).thenReturn(expected);

        Iterable<Context> result = this.catalinaHost.getContexts();

        Iterator<Context> contexts = result.iterator();
        assertTrue(contexts.hasNext());
        assertSame(expected, contexts.next());
        assertFalse(contexts.hasNext());
    }

    @Test
    public void getEngine() {
        Engine result = this.catalinaHost.getEngine();

        assertSame(this.engine, result);
    }

    @Test
    public void getName() {
        String expected = "name";

        when(this.host.getName()).thenReturn(expected);

        String result = this.catalinaHost.getName();

        assertSame(expected, result);
    }
}
