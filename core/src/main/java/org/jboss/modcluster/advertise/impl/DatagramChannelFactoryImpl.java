/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
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
import java.util.Locale;

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
        this.canBindToMulticastAddress = (value != null) && (value.toLowerCase(Locale.ENGLISH).startsWith("linux") || value.toLowerCase(Locale.ENGLISH).startsWith("mac") || value.toLowerCase(Locale.ENGLISH).startsWith("hp"));
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

        // Avoid MODCLUSTER-746 Creating an IPv4 multicast socket on Windows while java.net.preferIPv6Addresses=true can throw UnsupportedAddressTypeException
        InetSocketAddress bindToPort = (address instanceof Inet4Address) ? new InetSocketAddress("0.0.0.0", port) : new InetSocketAddress("::", port);

        // Windows-like
        if (!canBindToMulticastAddress) {
            return newChannel(address, bindToPort);
        }

        // Linux-like
        try {
            return newChannel(address, new InetSocketAddress(address, port));
        } catch (IOException e) {
            ModClusterLogger.LOGGER.potentialCrossTalking(address, (address instanceof Inet4Address) ? "IPv4" : "IPv6", e.getLocalizedMessage());
            ModClusterLogger.LOGGER.catchingDebug(e);
            return newChannel(address, bindToPort);
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
