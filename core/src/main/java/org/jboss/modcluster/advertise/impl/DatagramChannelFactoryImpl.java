/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.channels.DatagramChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.logging.Logger;
import org.jboss.modcluster.ModClusterLogger;
import org.jboss.modcluster.advertise.DatagramChannelFactory;

/**
 * On Linux-like systems, we attempt to avoid cross-talk problem by binding the DatagramChannel to the multicast
 * address, if possible. If not possible, default to binding only to the port. See
 * {@link <a href="https://issues.jboss.org/browse/JGRP-777">JGRP-777</a>}.
 * <p>
 * On Windows-like systems, we do not attempt to bind the socket to the multicast address, we only bind to the port.
 * <p>
 * On BSD-like systems (e.g. OS X) we also need to bind the DatagramChannel to the multicast address. If httpd with
 * mod_proxy_cluster (which is bound to the multicast address) is running on the same system, the AdvertiseListener
 * would not be able to bind to any address (i.e. only binding to the port without the address) because JDK does not
 * expose SO_REUSEPORT as a socket option which would be required for this to work. This is to be supported since JDK 9.
 * See {@link <a href="https://bugs.openjdk.java.net/browse/JDK-6432031">JDK-6432031</a>}.
 *
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public class DatagramChannelFactoryImpl implements DatagramChannelFactory {

    private final Logger log = Logger.getLogger(this.getClass());

    boolean canBindToMulticastAddress;

    public DatagramChannelFactoryImpl() {
        String value = this.getSystemProperty("os.name");
        this.canBindToMulticastAddress = (value != null) && (value.toLowerCase().startsWith("linux") || value.toLowerCase().startsWith("mac") || value.toLowerCase().startsWith("hp"));
    }

    private String getSystemProperty(final String key) {
        PrivilegedAction<String> action = new PrivilegedAction<String>() {
            @Override
            public String run() {
                try {
                    return System.getProperty(key);
                } catch (SecurityException e) {
                    DatagramChannelFactoryImpl.this.log.warn(e.getLocalizedMessage(), e);
                    return null;
                }
            }
        };
        return AccessController.doPrivileged(action);
    }

    @Override
    public DatagramChannel createDatagramChannel(InetSocketAddress multicastSocketAddress) throws IOException {
        InetAddress address = multicastSocketAddress.getAddress();
        int port = multicastSocketAddress.getPort();

        // Misconfiguration
        if (address == null) {
            throw ModClusterLogger.LOGGER.createMulticastSocketWithNullMulticastAddress();
        }
        if (!address.isMulticastAddress()) {
            throw ModClusterLogger.LOGGER.createMulticastSocketWithUnicastAddress(address);
        }

        // Windows-like
        if (!canBindToMulticastAddress) {
            return newChannel(address, new InetSocketAddress(port));
        }

        // Linux-like
        try {
            return newChannel(address, new InetSocketAddress(address, port));
        } catch (IOException e) {
            ModClusterLogger.LOGGER.potentialCrossTalking(address, (address instanceof Inet4Address) ? "IPv4" : "IPv6", e.getLocalizedMessage());
            ModClusterLogger.LOGGER.catchingDebug(e);
            return newChannel(address, new InetSocketAddress(port));
        }
    }

    private static DatagramChannel newChannel(InetAddress multicastAddress, InetSocketAddress bindAddress) throws IOException {
        DatagramChannel channel;
        if (multicastAddress == null) {
            channel = DatagramChannel.open();
        } else {
            channel = DatagramChannel.open(multicastAddress instanceof Inet4Address ? StandardProtocolFamily.INET : StandardProtocolFamily.INET6);
        }
        channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        if (bindAddress != null) {
            channel.bind(bindAddress);
        }
        return channel;
    }
}
