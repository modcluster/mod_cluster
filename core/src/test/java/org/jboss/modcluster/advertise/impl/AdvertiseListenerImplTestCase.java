/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.advertise.impl;

import static org.jboss.modcluster.advertise.impl.AdvertiseListenerImpl.DEFAULT_ENCODING;
import static org.jboss.modcluster.advertise.impl.AdvertiseListenerImpl.clearBuffer;
import static org.jboss.modcluster.advertise.impl.AdvertiseListenerImpl.flipBuffer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.modcluster.TestUtils;
import org.jboss.modcluster.advertise.AdvertiseListener;
import org.jboss.modcluster.advertise.DatagramChannelFactory;
import org.jboss.modcluster.config.AdvertiseConfiguration;
import org.jboss.modcluster.config.ProxyConfiguration;
import org.jboss.modcluster.mcmp.MCMPHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Tests {@link AdvertiseListenerImpl}.
 *
 * @author Brian Stansberry
 * @author Radoslav Husar
 */
class AdvertiseListenerImplTestCase {

    private static final String ADVERTISE_GROUP = System.getProperty("multicast.address1", "224.0.1.106");
    private static final int ADVERTISE_PORT = 23364;
    private static final String SERVER1 = "127.0.0.1";
    private static final String SERVER2 = "127.0.1.1";
    private static final int SERVER_PORT = 8989;
    private static final String SERVER1_ADDRESS = String.format("%s:%d", SERVER1, SERVER_PORT);
    private static final String SERVER2_ADDRESS = String.format("%s:%d", SERVER2, SERVER_PORT);
    private static final long TIMEOUT = 3_000; // time for advertise worker to process messages

    // Each test binds its own port, so that stray datagrams cannot leak from one test into the next
    private static final AtomicInteger PORT_SEQUENCE = new AtomicInteger(ADVERTISE_PORT);

    private MCMPHandler mcmpHandler = mock(MCMPHandler.class);
    private AdvertiseConfiguration config = mock(AdvertiseConfiguration.class);
    private DatagramChannelFactory channelFactory = mock(DatagramChannelFactory.class);

    private InetSocketAddress advertiseSocketAddress;
    private DatagramChannel channel;

    @BeforeEach
    void setup() throws Exception {
        this.advertiseSocketAddress = new InetSocketAddress(ADVERTISE_GROUP, PORT_SEQUENCE.getAndIncrement());

        when(this.config.getAdvertiseThreadFactory()).thenReturn(Executors.defaultThreadFactory());
        when(this.config.getAdvertiseSocketAddress()).thenReturn(this.advertiseSocketAddress);
        when(this.config.getAdvertiseSecurityKey()).thenReturn(null);
        when(this.config.getAdvertiseInterface()).thenReturn(TestUtils.getAdvertiseInterface());

        InetAddress groupAddress = InetAddress.getByName(ADVERTISE_GROUP);
        this.channel = new DatagramChannelFactoryImpl().createDatagramChannel(new InetSocketAddress(groupAddress, this.advertiseSocketAddress.getPort()));
    }

    @AfterEach
    void cleanup() throws IOException {
        // Tests normally close the channel via the listener; make sure it does not leak when a test fails early
        if (this.channel != null && this.channel.isOpen()) {
            this.channel.close();
        }
    }

    @Test
    void testBasicOperation() throws Exception {
        // Test using a separate sendChannel to test AdvertiseListenerImpl
        try (DatagramChannel sendChannel = new SendingDatagramChannelFactoryImpl().createDatagramChannel(this.advertiseSocketAddress)) {

            // Setup ArgumentCaptor before the listener is started by AdvertiseListenerImpl constructor
            ArgumentCaptor<InetSocketAddress> capturedAddress = ArgumentCaptor.forClass(InetSocketAddress.class);
            when(this.channelFactory.createDatagramChannel(capturedAddress.capture())).thenReturn(this.channel);

            AdvertiseListener listener = new AdvertiseListenerImpl(this.mcmpHandler, this.config, this.channelFactory);

            assertEquals(ADVERTISE_GROUP, capturedAddress.getValue().getAddress().getHostAddress());
            assertTrue(this.channel.isOpen());
            assertTrue(sendChannel.isOpen());

            ArgumentCaptor<ProxyConfiguration> capturedSocketAddress = ArgumentCaptor.forClass(ProxyConfiguration.class);

            ByteBuffer buffer = ByteBuffer.allocate(512);
            buffer.put(TestUtils.generateAdvertisePacketData(new Date(), 0, SERVER1, SERVER1_ADDRESS));
            flipBuffer(buffer);

            sendChannel.send(buffer, this.advertiseSocketAddress);
            flipBuffer(buffer);

            verify(this.mcmpHandler, timeout(TIMEOUT)).addProxy(capturedSocketAddress.capture());
            reset(this.mcmpHandler);

            InetSocketAddress socketAddress = capturedSocketAddress.getValue().getRemoteAddress();
            assertEquals(SERVER1, socketAddress.getAddress().getHostAddress());
            assertEquals(SERVER_PORT, socketAddress.getPort());

            capturedSocketAddress = ArgumentCaptor.forClass(ProxyConfiguration.class);
            clearBuffer(buffer);
            buffer.put(TestUtils.generateAdvertisePacketData(new Date(), 1, SERVER2, SERVER2_ADDRESS));
            flipBuffer(buffer);

            sendChannel.send(buffer, this.advertiseSocketAddress);
            flipBuffer(buffer);

            verify(this.mcmpHandler, timeout(TIMEOUT)).addProxy(capturedSocketAddress.capture());
            reset(this.mcmpHandler);

            socketAddress = capturedSocketAddress.getValue().getRemoteAddress();
            assertEquals(SERVER2, socketAddress.getAddress().getHostAddress());
            assertEquals(SERVER_PORT, socketAddress.getPort());

            assertFalse(this.channel.isConnected());

            closeListener(listener);

            assertFalse(this.channel.isOpen());
        }
    }

    /**
     * A malformed advertise message must be discarded rather than propagate an exception out of the worker and
     * silently terminate it, leaving the node deaf to all further advertisements; see MODCLUSTER-875.
     */
    @Test
    void testMalformedAdvertiseMessageDoesNotTerminateWorker() throws Exception {
        try (DatagramChannel sendChannel = new SendingDatagramChannelFactoryImpl().createDatagramChannel(this.advertiseSocketAddress)) {
            when(this.channelFactory.createDatagramChannel(any(InetSocketAddress.class))).thenReturn(this.channel);
            AdvertiseListenerImpl listener = new AdvertiseListenerImpl(this.mcmpHandler, this.config, this.channelFactory);

            try {
                // Non-numeric status code
                send(sendChannel, new String(TestUtils.generateAdvertisePacketData(new Date(), 0, SERVER1, SERVER1_ADDRESS), DEFAULT_ENCODING)
                        .replace("HTTP/1.1 200 OK", "HTTP/1.1 OK OK").getBytes(DEFAULT_ENCODING));

                // Non-numeric and out-of-range X-Manager-Address ports
                send(sendChannel, TestUtils.generateAdvertisePacketData(new Date(), 0, SERVER1, SERVER1 + ":port"));
                send(sendChannel, TestUtils.generateAdvertisePacketData(new Date(), 0, SERVER1, SERVER1 + ":99999"));

                // Headers the digest is computed over, each of them dereferenced while verifying it
                for (String omittedHeader : new String[]{"Date", "Sequence", "Digest"}) {
                    send(sendChannel, TestUtils.generateAdvertisePacketData(new Date(), 0, SERVER1, SERVER1_ADDRESS, omittedHeader));
                }

                // The worker must have survived every one of them and still be processing advertisements
                send(sendChannel, TestUtils.generateAdvertisePacketData(new Date(), 0, SERVER2, SERVER2_ADDRESS));

                ArgumentCaptor<ProxyConfiguration> capturedProxy = ArgumentCaptor.forClass(ProxyConfiguration.class);
                verify(this.mcmpHandler, timeout(TIMEOUT)).addProxy(capturedProxy.capture());

                assertEquals(SERVER2, capturedProxy.getValue().getRemoteAddress().getAddress().getHostAddress());
                assertEquals(SERVER_PORT, capturedProxy.getValue().getRemoteAddress().getPort());
            } finally {
                closeListener(listener);
            }
        }
    }

    /**
     * Not even an {@link Error} may terminate the worker, which would silently leave the node deaf to all further
     * advertisements.
     */
    @Test
    void testErrorDoesNotTerminateWorker() throws Exception {
        try (DatagramChannel sendChannel = new SendingDatagramChannelFactoryImpl().createDatagramChannel(this.advertiseSocketAddress)) {
            when(this.channelFactory.createDatagramChannel(any(InetSocketAddress.class))).thenReturn(this.channel);
            doThrow(new OutOfMemoryError("Simulated")).doNothing().when(this.mcmpHandler).addProxy(any(ProxyConfiguration.class));
            AdvertiseListenerImpl listener = new AdvertiseListenerImpl(this.mcmpHandler, this.config, this.channelFactory);

            try {
                send(sendChannel, TestUtils.generateAdvertisePacketData(new Date(), 0, SERVER1, SERVER1_ADDRESS));
                verify(this.mcmpHandler, timeout(TIMEOUT)).addProxy(any(ProxyConfiguration.class));

                // The worker must have survived the error and still be processing advertisements
                send(sendChannel, TestUtils.generateAdvertisePacketData(new Date(), 0, SERVER2, SERVER2_ADDRESS));

                ArgumentCaptor<ProxyConfiguration> capturedProxy = ArgumentCaptor.forClass(ProxyConfiguration.class);
                verify(this.mcmpHandler, timeout(TIMEOUT).times(2)).addProxy(capturedProxy.capture());

                assertEquals(SERVER2, capturedProxy.getValue().getRemoteAddress().getAddress().getHostAddress());
            } finally {
                closeListener(listener);
            }
        }
    }

    /**
     * An advertisement with a missing or malformed X-Manager-Address header must not prevent the server's subsequent
     * advertisements from registering its proxy.
     */
    @Test
    void testMalformedManagerAddressDoesNotPreventRegistration() throws Exception {
        try (DatagramChannel sendChannel = new SendingDatagramChannelFactoryImpl().createDatagramChannel(this.advertiseSocketAddress)) {
            when(this.channelFactory.createDatagramChannel(any(InetSocketAddress.class))).thenReturn(this.channel);
            AdvertiseListenerImpl listener = new AdvertiseListenerImpl(this.mcmpHandler, this.config, this.channelFactory);

            try {
                send(sendChannel, TestUtils.generateAdvertisePacketData(new Date(), 0, SERVER1, SERVER1 + ":port"));
                send(sendChannel, TestUtils.generateAdvertisePacketData(new Date(), 0, SERVER1, SERVER1 + ":99999"));
                send(sendChannel, TestUtils.generateAdvertisePacketData(new Date(), 0, SERVER1, SERVER1_ADDRESS, AdvertisedServer.MANAGER_ADDRESS));

                // A well-formed advertisement from the very same server must still register its proxy
                send(sendChannel, TestUtils.generateAdvertisePacketData(new Date(), 0, SERVER1, SERVER1_ADDRESS));

                ArgumentCaptor<ProxyConfiguration> capturedProxy = ArgumentCaptor.forClass(ProxyConfiguration.class);
                verify(this.mcmpHandler, timeout(TIMEOUT)).addProxy(capturedProxy.capture());

                assertEquals(SERVER1, capturedProxy.getValue().getRemoteAddress().getAddress().getHostAddress());
                assertEquals(SERVER_PORT, capturedProxy.getValue().getRemoteAddress().getPort());
                assertNotNull(awaitServer(listener, SERVER1));
            } finally {
                closeListener(listener);
            }
        }
    }

    /**
     * An advertisement failing digest verification must not modify the parameters of an already recorded server.
     */
    @Test
    void testUnverifiedMessageDoesNotModifyRecordedServer() throws Exception {
        try (DatagramChannel sendChannel = new SendingDatagramChannelFactoryImpl().createDatagramChannel(this.advertiseSocketAddress)) {
            when(this.channelFactory.createDatagramChannel(any(InetSocketAddress.class))).thenReturn(this.channel);
            AdvertiseListenerImpl listener = new AdvertiseListenerImpl(this.mcmpHandler, this.config, this.channelFactory);

            try {
                send(sendChannel, TestUtils.generateAdvertisePacketData(new Date(), 0, SERVER1, SERVER1_ADDRESS));
                AdvertisedServer server = awaitServer(listener, SERVER1);
                assertNotNull(server);

                // Same server, different manager address, but with a digest that does not verify
                String forged = new String(TestUtils.generateAdvertisePacketData(new Date(), 1, SERVER1, SERVER2_ADDRESS), DEFAULT_ENCODING);
                send(sendChannel, forged.replaceFirst("Digest: [0-9a-f]+", "Digest: " + "0".repeat(32)).getBytes(DEFAULT_ENCODING));

                // Messages are processed in order, so once the next server was added the forged message was processed too
                send(sendChannel, TestUtils.generateAdvertisePacketData(new Date(), 0, SERVER2, SERVER2_ADDRESS));
                verify(this.mcmpHandler, timeout(TIMEOUT).times(2)).addProxy(any(ProxyConfiguration.class));

                assertEquals(SERVER1_ADDRESS, server.getParameter(AdvertisedServer.MANAGER_ADDRESS));
            } finally {
                closeListener(listener);
            }
        }
    }

    /**
     * Waits for the listener to record the given server, which happens only after its proxy was added.
     */
    private static AdvertisedServer awaitServer(AdvertiseListenerImpl listener, String name) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT;
        AdvertisedServer server = listener.getServer(name);
        while (server == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
            server = listener.getServer(name);
        }
        return server;
    }

    private void send(DatagramChannel sendChannel, byte[] packet) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(512);
        buffer.put(packet);
        flipBuffer(buffer);

        sendChannel.send(buffer, this.advertiseSocketAddress);
    }

    private static void closeListener(AdvertiseListener listener) throws IOException {
        try {
            listener.close();
        } catch (IOException e) {
            // Workaround for https://bugs.openjdk.java.net/browse/JDK-8050499
            if (!System.getProperty("os.name").toLowerCase(Locale.ENGLISH).startsWith("mac") || !"Unknown error: 316".equals(e.getMessage())) {
                throw e;
            }
        }
    }
}
