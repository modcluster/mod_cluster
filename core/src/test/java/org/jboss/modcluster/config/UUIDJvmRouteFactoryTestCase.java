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

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import org.jboss.modcluster.config.impl.UUIDJvmRouteFactory;
import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.Engine;
import org.junit.Test;

public class UUIDJvmRouteFactoryTestCase {
    @Test
    public void test() throws IOException {
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
