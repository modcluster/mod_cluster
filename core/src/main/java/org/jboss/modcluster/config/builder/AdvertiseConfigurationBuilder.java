/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.config.builder;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.jboss.modcluster.config.AdvertiseConfiguration;
import org.jboss.modcluster.config.impl.AdvertiseConfigurationImpl;

/**
 * Builder for multicast-based advertise configuration.
 *
 * @author Radoslav Husar
 * @since 1.3.6.Final
 */
public class AdvertiseConfigurationBuilder extends AbstractConfigurationBuilder implements Creator<AdvertiseConfiguration> {

    private InetSocketAddress advertiseSocketAddress = AdvertiseConfiguration.DEFAULT_SOCKET_ADDRESS;
    private NetworkInterface advertiseInterface;
    private String advertiseSecurityKey;
    private ThreadFactory advertiseThreadFactory = Executors.defaultThreadFactory();

    AdvertiseConfigurationBuilder(ConfigurationBuilder parentBuilder) {
        super(parentBuilder);
    }

    /**
     * Sets the socket factory to use for advertisements.
     */
    public AdvertiseConfigurationBuilder setAdvertiseSocketAddress(InetSocketAddress advertiseSocketAddress) {
        this.advertiseSocketAddress = advertiseSocketAddress;
        return this;
    }

    /**
     * Sets the interface to use for advertisements.
     */
    public AdvertiseConfigurationBuilder setAdvertiseInterface(NetworkInterface advertiseInterface) {
        this.advertiseInterface = advertiseInterface;
        return this;
    }

    /**
     * Sets the interface to use for advertisements.
     *
     * @deprecated Use {@link AdvertiseConfigurationBuilder#setAdvertiseInterface(java.net.NetworkInterface)} instead.
     */
    @Deprecated
    public AdvertiseConfigurationBuilder setAdvertiseInterface(InetAddress advertiseInterface) {
        try {
            this.advertiseInterface = NetworkInterface.getByInetAddress(advertiseInterface);
        } catch (SocketException e) {
            // TODO i18n
            throw new RuntimeException();
        }
        return this;
    }

    /**
     * Sets the shared advertise security key.
     */
    public AdvertiseConfigurationBuilder setAdvertiseSecurityKey(String advertiseSecurityKey) {
        this.advertiseSecurityKey = advertiseSecurityKey;
        return this;
    }

    /**
     * Sets the tread factory for advertise mechanism.
     */
    public AdvertiseConfigurationBuilder setAdvertiseThreadFactory(ThreadFactory advertiseThreadFactory) {
        this.advertiseThreadFactory = advertiseThreadFactory;
        return this;
    }

    @Override
    public AdvertiseConfiguration create() {
        return new AdvertiseConfigurationImpl(advertiseSocketAddress, advertiseInterface, advertiseSecurityKey, advertiseThreadFactory);
    }
}