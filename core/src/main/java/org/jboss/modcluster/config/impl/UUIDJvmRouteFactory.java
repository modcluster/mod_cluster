/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.config.impl;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.jboss.modcluster.config.JvmRouteFactory;
import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.Engine;

/**
 * Generates a jvm route using a UUID constructed from the connector address/port and engine name.
 *
 * @author Paul Ferraro
 */
public class UUIDJvmRouteFactory implements JvmRouteFactory {
    private static final int INT_SIZE = Integer.SIZE / Byte.SIZE;

    @Override
    public String createJvmRoute(Engine engine) {
        Connector connector = engine.getProxyConnector();
        byte[] address = connector.getAddress().getAddress();
        byte[] name = engine.getName().getBytes();

        ByteBuffer buffer = ByteBuffer.allocate(address.length + INT_SIZE + name.length);

        buffer.put(address).putInt(connector.getPort()).put(name);

        return UUID.nameUUIDFromBytes(buffer.array()).toString();
    }
}
