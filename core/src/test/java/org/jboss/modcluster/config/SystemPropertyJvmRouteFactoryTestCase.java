/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
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
