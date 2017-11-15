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
