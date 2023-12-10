/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.config;

import java.net.InetSocketAddress;

/**
 * Proxy configuration with destination address and optional local address to bind to.
 *
 * @author Radoslav Husar
 * @since 1.3.1.Final
 */
public interface ProxyConfiguration {

    /**
     * Returns the remote address of the proxy.
     *
     * @return remote address of the proxy
     */
    InetSocketAddress getRemoteAddress();

    /**
     * Returns the local address to bind to for connecting to the proxy, if {@code null} the default will be used,
     * if port is {@code 0} an ephemeral port is used.
     *
     * @return local address to bind to for connecting to the proxy
     */
    InetSocketAddress getLocalAddress();

}
