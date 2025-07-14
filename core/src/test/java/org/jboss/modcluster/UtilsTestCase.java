/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;

class UtilsTestCase {
    @Test
    void parseParseSocketAddress() throws UnknownHostException {
        InetSocketAddress address = Utils.parseSocketAddress("127.0.0.1", 0);

        assertEquals("127.0.0.1", address.getAddress().getHostAddress());
        assertEquals(0, address.getPort());

        address = Utils.parseSocketAddress("127.0.0.1:80", 0);

        assertEquals("127.0.0.1", address.getAddress().getHostAddress());
        assertEquals(80, address.getPort());

        address = Utils.parseSocketAddress("localhost", 0);

        assertEquals("localhost", address.getAddress().getHostName());
        assertEquals(0, address.getPort());

        address = Utils.parseSocketAddress("localhost:80", 0);

        assertEquals("localhost", address.getAddress().getHostName());
        assertEquals(80, address.getPort());

        address = Utils.parseSocketAddress("0:0:0:0:0:0:0:1", 0);

        assertEquals("0:0:0:0:0:0:0:1", address.getAddress().getHostAddress());
        assertEquals(0, address.getPort());

        address = Utils.parseSocketAddress("::1", 0);

        assertEquals("0:0:0:0:0:0:0:1", address.getAddress().getHostAddress());
        assertEquals(0, address.getPort());

        address = Utils.parseSocketAddress("[0:0:0:0:0:0:0:1]:80", 0);

        assertEquals("0:0:0:0:0:0:0:1", address.getAddress().getHostAddress());
        assertEquals(80, address.getPort());

        address = Utils.parseSocketAddress("[::1]:80", 0);

        assertEquals("0:0:0:0:0:0:0:1", address.getAddress().getHostAddress());
        assertEquals(80, address.getPort());

        address = Utils.parseSocketAddress("", 0);

        assertEquals(InetAddress.getLocalHost().getHostName(), address.getHostName());
        assertEquals(0, address.getPort());

        address = Utils.parseSocketAddress(null, 0);

        assertEquals(InetAddress.getLocalHost().getHostName(), address.getHostName());
        assertEquals(0, address.getPort());
    }
}
