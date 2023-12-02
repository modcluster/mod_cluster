/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.config;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.jboss.modcluster.config.impl.SimpleJvmRouteFactory;
import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.Engine;
import org.junit.Test;

public class SimpleJvmRouteFactoryTestCase {
    @Test
    public void defaultPattern() throws UnknownHostException {
        Engine engine = mock(Engine.class);
        Connector connector = mock(Connector.class);
        InetAddress address = InetAddress.getLocalHost();

        JvmRouteFactory factory = new SimpleJvmRouteFactory();

        when(engine.getProxyConnector()).thenReturn(connector);
        when(connector.getAddress()).thenReturn(address);
        when(connector.getPort()).thenReturn(8000);
        when(engine.getName()).thenReturn("engine");

        String result = factory.createJvmRoute(engine);

        assertEquals(result, address.getHostAddress() + ":8000:engine");
    }

    @Test
    public void customPattern() throws UnknownHostException {
        Engine engine = mock(Engine.class);
        Connector connector = mock(Connector.class);
        InetAddress address = InetAddress.getLocalHost();

        JvmRouteFactory factory = new SimpleJvmRouteFactory("{2}-{1}-{0}");

        when(engine.getProxyConnector()).thenReturn(connector);
        when(connector.getAddress()).thenReturn(address);
        when(connector.getPort()).thenReturn(8000);
        when(engine.getName()).thenReturn("engine");

        String result = factory.createJvmRoute(engine);

        assertEquals(result, "engine-8000-" + address.getHostAddress());
    }
}
