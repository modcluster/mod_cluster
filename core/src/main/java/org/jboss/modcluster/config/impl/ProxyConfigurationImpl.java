/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.config.impl;

import java.net.InetSocketAddress;

import org.jboss.modcluster.config.ProxyConfiguration;

/**
 * @author Radoslav Husar
 */
public class ProxyConfigurationImpl implements ProxyConfiguration {

    private final InetSocketAddress remoteAddress;
    private final InetSocketAddress localAddress;

    public ProxyConfigurationImpl(InetSocketAddress remoteAddress) {
        this(remoteAddress, null);
    }

    public ProxyConfigurationImpl(InetSocketAddress remoteAddress, InetSocketAddress localAddress) {
        this.remoteAddress = remoteAddress;
        this.localAddress = localAddress;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return this.remoteAddress;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return this.localAddress;
    }
}
