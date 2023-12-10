/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.config;

import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.concurrent.ThreadFactory;

/**
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public interface AdvertiseConfiguration {
    InetSocketAddress DEFAULT_SOCKET_ADDRESS = new InetSocketAddress("224.0.1.105", 23364);

    InetSocketAddress getAdvertiseSocketAddress();

    /**
     * Multicast-enabled {@link NetworkInterface} to listen for advertisements.
     */
    NetworkInterface getAdvertiseInterface();

    /**
     * Advertise security key.
     */
    String getAdvertiseSecurityKey();

    ThreadFactory getAdvertiseThreadFactory();
}
