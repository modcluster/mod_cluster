/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.advertise.impl;

import java.io.IOException;

import org.jboss.modcluster.advertise.AdvertiseListener;
import org.jboss.modcluster.advertise.AdvertiseListenerFactory;
import org.jboss.modcluster.config.AdvertiseConfiguration;
import org.jboss.modcluster.mcmp.MCMPHandler;

/**
 * @author Paul Ferraro
 */
public class AdvertiseListenerFactoryImpl implements AdvertiseListenerFactory {

    @Override
    public AdvertiseListener createListener(MCMPHandler handler, AdvertiseConfiguration config) throws IOException {
        return new AdvertiseListenerImpl(handler, config, new DatagramChannelFactoryImpl());
    }
}
