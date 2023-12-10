/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.mcmp;

import java.net.InetAddress;

public interface MCMPConnectionListener {
    void connectionEstablished(InetAddress localAddress);

    boolean isEstablished();
}
