/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.advertise;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;

/**
 * Factory for creating datagram multicast channels.
 *
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public interface DatagramChannelFactory {
    DatagramChannel createDatagramChannel(InetSocketAddress multicastSocketAddress) throws IOException;
}
