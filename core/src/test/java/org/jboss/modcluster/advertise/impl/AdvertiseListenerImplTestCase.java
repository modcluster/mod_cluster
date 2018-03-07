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
package org.jboss.modcluster.advertise.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Date;
import java.util.concurrent.Executors;

import org.jboss.modcluster.TestUtils;
import org.jboss.modcluster.advertise.AdvertiseListener;
import org.jboss.modcluster.advertise.DatagramChannelFactory;
import org.jboss.modcluster.config.AdvertiseConfiguration;
import org.jboss.modcluster.config.ProxyConfiguration;
import org.jboss.modcluster.mcmp.MCMPHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Tests {@link AdvertiseListenerImpl}.
 *
 * @author Brian Stansberry
 * @author Radoslav Husar
 */
public class AdvertiseListenerImplTestCase {

    private static final String ADVERTISE_GROUP = System.getProperty("multicast.address1", "224.0.1.106");
    private static final int ADVERTISE_PORT = 23364;
    private static final String SERVER1 = "127.0.0.1";
    private static final String SERVER2 = "127.0.1.1";
    private static final int SERVER_PORT = 8989;
    private static final String SERVER1_ADDRESS = String.format("%s:%d", SERVER1, SERVER_PORT);
    private static final String SERVER2_ADDRESS = String.format("%s:%d", SERVER2, SERVER_PORT);
    private static final int TIMEOUT_MILLIS = 3_000; // time for advertise worker to process messages

    private MCMPHandler mcmpHandler = mock(MCMPHandler.class);
    private AdvertiseConfiguration config = mock(AdvertiseConfiguration.class);
    private DatagramChannelFactory channelFactory = mock(DatagramChannelFactory.class);

    private DatagramChannel channel;

    @Before
    public void setup() throws Exception {
        when(this.config.getAdvertiseThreadFactory()).thenReturn(Executors.defaultThreadFactory());
        when(this.config.getAdvertiseSocketAddress()).thenReturn(new InetSocketAddress(ADVERTISE_GROUP, ADVERTISE_PORT));
        when(this.config.getAdvertiseSecurityKey()).thenReturn(null);
        when(this.config.getAdvertiseInterface()).thenReturn(TestUtils.getAdvertiseInterface());

        InetAddress groupAddress = InetAddress.getByName(ADVERTISE_GROUP);
        this.channel = new DatagramChannelFactoryImpl().createDatagramChannel(new InetSocketAddress(groupAddress, ADVERTISE_PORT));
    }

    @Test
    public void testBasicOperation() throws Exception {
        // Test using a separate sendChannel to test AdvertiseListenerImpl
        try (DatagramChannel sendChannel = new SendingDatagramChannelFactoryImpl().createDatagramChannel(new InetSocketAddress(ADVERTISE_GROUP, ADVERTISE_PORT))) {

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
            buffer.flip();

            sendChannel.send(buffer, config.getAdvertiseSocketAddress());
            buffer.flip();

            verify(this.mcmpHandler, timeout(TIMEOUT_MILLIS)).addProxy(capturedSocketAddress.capture());
            reset(this.mcmpHandler);

            InetSocketAddress socketAddress = capturedSocketAddress.getValue().getRemoteAddress();
            assertEquals(SERVER1, socketAddress.getAddress().getHostAddress());
            assertEquals(SERVER_PORT, socketAddress.getPort());

            capturedSocketAddress = ArgumentCaptor.forClass(ProxyConfiguration.class);
            buffer.clear();
            buffer.put(TestUtils.generateAdvertisePacketData(new Date(), 1, SERVER2, SERVER2_ADDRESS));
            buffer.flip();

            sendChannel.send(buffer, config.getAdvertiseSocketAddress());
            buffer.flip();

            verify(this.mcmpHandler, timeout(TIMEOUT_MILLIS)).addProxy(capturedSocketAddress.capture());
            reset(this.mcmpHandler);

            socketAddress = capturedSocketAddress.getValue().getRemoteAddress();
            assertEquals(SERVER2, socketAddress.getAddress().getHostAddress());
            assertEquals(SERVER_PORT, socketAddress.getPort());

            assertFalse(this.channel.isConnected());

            listener.close();

            assertFalse(this.channel.isOpen());
        }
    }
}
