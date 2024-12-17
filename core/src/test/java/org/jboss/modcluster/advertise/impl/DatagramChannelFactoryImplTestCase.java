/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.advertise.impl;

import static org.jboss.modcluster.advertise.impl.AdvertiseListenerImpl.flipBuffer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.util.Random;
import java.util.concurrent.Executors;

import org.jboss.modcluster.TestUtils;
import org.jboss.modcluster.advertise.DatagramChannelFactory;
import org.jboss.modcluster.config.AdvertiseConfiguration;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests {@link DatagramChannelFactoryImpl}.
 * <p>
 * Verify that the datagram channels created by the {@link DatagramChannelFactory} can receive multicast messages and
 * do not cross-talk (the case when both receive datagrams while using different multicast groups).
 *
 * @author Brian Stansberry
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public class DatagramChannelFactoryImplTestCase {

    private static final String GROUP1 = System.getProperty("multicast.address1", "224.0.1.106");
    private static final String GROUP2 = System.getProperty("multicast.address2", "224.0.1.107");
    private static final int PORT = AdvertiseConfiguration.DEFAULT_SOCKET_ADDRESS.getPort();

    /**
     * Verify that the created channel can receive datagrams; sender and receiver address are the same.
     */
    @Test
    public void testDatagramChannelRead() throws Exception {
        this.testDatagramChannel(InetAddress.getByName(GROUP1));
    }

    /**
     * Verify that cross-talking problem does not happen anymore.
     *
     * @see <a href="https://developer.jboss.org/docs/DOC-9469">Cross talking between clusters with same multicast ports but different multicast addresses</a>
     * @see <a href="https://issues.redhat.com/browse/JGRP-777">JGRP-777 Revisit multicast socket creation code</a>
     * @see <a href="https://issues.redhat.com/browse/JGRP-836">JGRP-836 Eliminate Linux cross-talk in MPING</a>
     */
    @Test
    public void testDatagramChannelNoCrossTalking() throws Exception {
        this.testDatagramChannel(InetAddress.getByName(GROUP2));
    }

    private void testDatagramChannel(InetAddress sendAddress) throws IOException {
        final InetAddress address = InetAddress.getByName(GROUP1);
        final NetworkInterface iface = TestUtils.getAdvertiseInterface();

        try (DatagramChannel receiveChannel = new DatagramChannelFactoryImpl().createDatagramChannel(new InetSocketAddress(address, PORT))) {
            final MembershipKey membershipKey = receiveChannel.join(address, iface);

            this.test(receiveChannel, sendAddress, address.equals(sendAddress));

            membershipKey.drop();
        }
    }

    private void test(final DatagramChannel receiveChannel, InetAddress sendAddress, boolean expectSuccessfulRead) throws IOException {
        try (DatagramChannel sendChannel = new SendingDatagramChannelFactoryImpl().createDatagramChannel(new InetSocketAddress(sendAddress, PORT))) {
            byte[] stringData = new byte[128];
            new Random().nextBytes(stringData);

            ByteBuffer sendBuffer = ByteBuffer.allocate(256);
            sendBuffer.put(stringData);
            flipBuffer(sendBuffer);

            InetSocketAddress group = new InetSocketAddress(sendAddress, PORT);
            sendChannel.send(sendBuffer, group);

            // Note: setting channel.socket().setSoTimeout(int) has no effect
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
                flipBuffer(receiveBuffer);

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
