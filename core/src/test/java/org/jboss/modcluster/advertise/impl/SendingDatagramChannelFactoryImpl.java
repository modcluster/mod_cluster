/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.advertise.impl;

/**
 * Extends {@link DatagramChannelFactoryImpl} in such a way that a sending channel/socket will be constructed as
 * opposed to listening socket which is bound to the multicast address (which acts as a filtering mechanism).
 *
 * @author Radoslav Husar
 */
class SendingDatagramChannelFactoryImpl extends DatagramChannelFactoryImpl {

    SendingDatagramChannelFactoryImpl() {
        super();
        this.canBindToMulticastAddress = false;
    }
}
