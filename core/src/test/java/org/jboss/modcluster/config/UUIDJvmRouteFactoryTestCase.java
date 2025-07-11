/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import org.jboss.modcluster.config.impl.UUIDJvmRouteFactory;
import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.Engine;
import org.junit.jupiter.api.Test;

class UUIDJvmRouteFactoryTestCase {
    @Test
    void test() throws IOException {
        Engine engine = mock(Engine.class);
        Connector connector = mock(Connector.class);
        InetAddress address = InetAddress.getLocalHost();

        JvmRouteFactory factory = new UUIDJvmRouteFactory();

        when(engine.getProxyConnector()).thenReturn(connector);
        when(connector.getAddress()).thenReturn(address);
        when(connector.getPort()).thenReturn(8000);
        when(engine.getName()).thenReturn("engine");

        String result = factory.createJvmRoute(engine);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutput output = new DataOutputStream(bytes);
        output.write(address.getAddress());
        output.writeInt(8000);
        output.write("engine".getBytes());

        assertEquals(result, UUID.nameUUIDFromBytes(bytes.toByteArray()).toString());
    }
}
