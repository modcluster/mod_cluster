/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.mcmp;

import java.net.InetSocketAddress;

/**
 * Represents a native server that is running the <code>mod_cluster</code> module and proxying requests to JBoss Web. For
 * example, an Apache httpd instance. Such an instance represents the server in the Mod Cluster Management Protocol, with an
 * MCMPHandler acting as the client.
 *
 * @author Brian Stansberry
 */
public interface MCMPServer {
    InetSocketAddress getSocketAddress();

    boolean isEstablished();
}
