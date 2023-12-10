/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.config.impl;

import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.concurrent.ThreadFactory;

import org.jboss.modcluster.config.AdvertiseConfiguration;

/**
 * @author Radoslav Husar
 * @since 1.3.6.Final
 */
public class AdvertiseConfigurationImpl implements AdvertiseConfiguration {

    private final InetSocketAddress advertiseSocketAddress;
    private final NetworkInterface advertiseInterface;
    private final String advertiseSecurityKey;
    private final ThreadFactory advertiseThreadFactory;

    public AdvertiseConfigurationImpl(InetSocketAddress advertiseSocketAddress, NetworkInterface advertiseInterface, String advertiseSecurityKey, ThreadFactory advertiseThreadFactory) {
        this.advertiseSocketAddress = advertiseSocketAddress;
        this.advertiseInterface = advertiseInterface;
        this.advertiseSecurityKey = advertiseSecurityKey;
        this.advertiseThreadFactory = advertiseThreadFactory;
    }

    @Override
    public InetSocketAddress getAdvertiseSocketAddress() {
        return advertiseSocketAddress;
    }

    @Override
    public NetworkInterface getAdvertiseInterface() {
        return advertiseInterface;
    }

    @Override
    public String getAdvertiseSecurityKey() {
        return advertiseSecurityKey;
    }

    @Override
    public ThreadFactory getAdvertiseThreadFactory() {
        return advertiseThreadFactory;
    }
}
