/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.modcluster.advertise;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.Arrays;

import org.jboss.modcluster.advertise.impl.MulticastSocketFactoryImpl;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests {@link MulticastSocketFactoryImpl}.
 * 
 * @author Brian Stansberry
 * @author Paul Ferraro
 */
public class MulticastSocketFactoryImplTestCase {
    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
    }
    private static final String GROUP1 = "224.0.1.106";
    private static final String GROUP2 = "224.0.1.107";
    private static final int PORT = 23364;

    @Test
    public void testMulticastSocketNoCrossTalk() throws IOException {
        this.testMulticastSocket(false);
    }

    public void testMulticastSocket(boolean allowCrossTalking) throws IOException {
        InetAddress address = InetAddress.getByName(GROUP1);

        MulticastSocket socket = new MulticastSocketFactoryImpl().createMulticastSocket(address, PORT);

        socket.setSoTimeout(1000);

        try {
            socket.joinGroup(address);

            this.testMulticastSocket(socket, address, true);
            // Test for cross-talking
            this.testMulticastSocket(socket, InetAddress.getByName(GROUP2), allowCrossTalking);

            socket.leaveGroup(address);
        } finally {
            socket.close();
        }
    }

    public void testMulticastSocket(MulticastSocket receiveSocket, InetAddress sendAddress, boolean expectSuccessfulRead)
            throws IOException {
        MulticastSocket sendSocket =  new MulticastSocket();

        try {
            String data = "1234567890";

            byte[] buffer = data.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, sendAddress, PORT);

            sendSocket.send(packet);

            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            Arrays.fill(buffer, (byte) 0);

            packet = new DatagramPacket(buffer, buffer.length);

            try {
                receiveSocket.receive(packet);

                Assert.assertTrue(expectSuccessfulRead);
                Assert.assertArrayEquals(buffer, data.getBytes());
            } catch (SocketTimeoutException e) {
                Assert.assertFalse(expectSuccessfulRead);
            }

        } finally {
            sendSocket.close();
        }
    }
}
