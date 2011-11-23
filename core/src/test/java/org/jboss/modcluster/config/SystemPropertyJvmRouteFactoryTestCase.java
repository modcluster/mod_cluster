/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.modcluster.config;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.jboss.modcluster.config.impl.SystemPropertyJvmRouteFactory;
import org.jboss.modcluster.container.Engine;
import org.junit.Test;

public class SystemPropertyJvmRouteFactoryTestCase {
    @Test
    public void defaultSystemProperty() {
        String expected = "expected";
        System.setProperty("jboss.mod_cluster.jvmRoute", expected);

        Engine engine = mock(Engine.class);
        JvmRouteFactory defaultFactory = mock(JvmRouteFactory.class);
        JvmRouteFactory factory = new SystemPropertyJvmRouteFactory(defaultFactory);

        when(defaultFactory.createJvmRoute(engine)).thenReturn("unexpected");

        String result = factory.createJvmRoute(engine);

        assertSame(expected, result);
    }

    @Test
    public void customSystemProperty() {
        String expected = "expected";
        System.setProperty("jboss.jvmRoute", expected);

        Engine engine = mock(Engine.class);
        JvmRouteFactory defaultFactory = mock(JvmRouteFactory.class);
        JvmRouteFactory factory = new SystemPropertyJvmRouteFactory(defaultFactory, "jboss.jvmRoute");

        when(defaultFactory.createJvmRoute(engine)).thenReturn("unexpected");

        String result = factory.createJvmRoute(engine);

        assertSame(expected, result);
    }

    @Test
    public void defaultJvmRoute() {
        String expected = "expected";
        System.clearProperty("jboss.mod_cluster.jvmRoute");

        Engine engine = mock(Engine.class);
        JvmRouteFactory defaultFactory = mock(JvmRouteFactory.class);
        JvmRouteFactory factory = new SystemPropertyJvmRouteFactory(defaultFactory);

        when(defaultFactory.createJvmRoute(engine)).thenReturn(expected);

        String result = factory.createJvmRoute(engine);

        assertSame(expected, result);
    }

    @Test
    public void customDefaultJvmRoute() {
        String expected = "expected";
        System.clearProperty("jboss.jvmRoute");

        Engine engine = mock(Engine.class);
        JvmRouteFactory defaultFactory = mock(JvmRouteFactory.class);
        JvmRouteFactory factory = new SystemPropertyJvmRouteFactory(defaultFactory, "jboss.jvmRoute");

        when(defaultFactory.createJvmRoute(engine)).thenReturn(expected);

        String result = factory.createJvmRoute(engine);

        assertSame(expected, result);
    }
}
