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

package org.jboss.modcluster.config.builder;

import java.net.InetAddress;
import java.net.InetSocketAddress;
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
    private InetAddress advertiseInterface;
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
    public AdvertiseConfigurationBuilder setAdvertiseInterface(InetAddress advertiseInterface) {
        this.advertiseInterface = advertiseInterface;
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