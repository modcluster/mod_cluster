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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.util.concurrent.Executors;

import org.jboss.modcluster.TestUtils;
import org.jboss.modcluster.advertise.impl.AdvertiseListenerImpl;
import org.jboss.modcluster.advertise.impl.DatagramChannelFactoryImpl;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests {@link DatagramChannelFactoryImpl}.
 *
 * @author Brian Stansberry
 * @author Paul Ferraro
 * @author Radoslav Husar
 * @version May 2016
 */
public class DatagramChannelFactoryImplTestCase {

    private static final String GROUP1 = System.getProperty("multicast.address1", "224.0.1.106");
    private static final String GROUP2 = System.getProperty("multicast.address2", "224.0.1.107");
    private static final int PORT = 23364;

    @Test
    public void testDatagramChannel() throws IOException {
        final InetAddress address = InetAddress.getByName(GROUP1);
        final NetworkInterface iface = TestUtils.getAdvertiseInterface();

        try (DatagramChannel receiveChannel = new DatagramChannelFactoryImpl().createDatagramChannel(address, PORT)) {
            final MembershipKey membershipKey = receiveChannel.join(address, iface);

            this.test(receiveChannel, iface, address, true);
            // Verify cross-talking problem does not happen any more
            this.test(receiveChannel, iface, InetAddress.getByName(GROUP2), false);

            membershipKey.drop();
        }
    }

    private void test(final DatagramChannel receiveChannel, NetworkInterface iface, InetAddress sendAddress, boolean expectSuccessfulRead) throws IOException {

        try (DatagramChannel sendChannel = new DatagramChannelFactoryImpl().createDatagramChannel(sendAddress, PORT)) {
            sendChannel.join(sendAddress, iface);

            String data = "1234567890";

            byte[] stringData = data.getBytes(AdvertiseListenerImpl.DEFAULT_ENCODING);
            ByteBuffer sendBuffer = ByteBuffer.allocate(256);
            sendBuffer.put(stringData);
            sendBuffer.flip();

            InetSocketAddress group = new InetSocketAddress(sendAddress, PORT);
            sendChannel.send(sendBuffer, group);


            // Setting channel.socket().setSoTimeout(int) has no effect
            // https://bugs.openjdk.java.net/browse/JDK-4614802
            Executors.newSingleThreadExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                        if (receiveChannel != null && receiveChannel.isOpen()) {
                            receiveChannel.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            try {
                ByteBuffer receiveBuffer = ByteBuffer.allocate(256);

                receiveChannel.receive(receiveBuffer);
                receiveBuffer.flip();

                byte[] packetData = new byte[receiveBuffer.remaining()];
                receiveBuffer.get(packetData);

                Assert.assertTrue(expectSuccessfulRead);
                Assert.assertArrayEquals(stringData, packetData);
            } catch (AsynchronousCloseException e) {
                Assert.assertFalse(expectSuccessfulRead);
            }
        }
    }
}
