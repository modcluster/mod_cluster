/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.config.impl;

import org.jboss.modcluster.config.AdvertiseConfiguration;
import org.jboss.modcluster.config.BalancerConfiguration;
import org.jboss.modcluster.config.MCMPHandlerConfiguration;
import org.jboss.modcluster.config.ModClusterConfiguration;
import org.jboss.modcluster.config.NodeConfiguration;

/**
 * @author Radoslav Husar
 * @since 1.3.6.Final
 */
public class ModClusterConfigurationImpl implements ModClusterConfiguration {

    private final AdvertiseConfiguration advertiseConfiguration;
    private final BalancerConfiguration balancerConfiguration;
    private final NodeConfiguration nodeConfiguration;
    private final MCMPHandlerConfiguration mcmpHandlerConfiguration;

    public ModClusterConfigurationImpl(AdvertiseConfiguration advertiseConfiguration, BalancerConfiguration balancerConfiguration, NodeConfiguration nodeConfiguration, MCMPHandlerConfiguration mcmpHandlerConfiguration) {
        this.advertiseConfiguration = advertiseConfiguration;
        this.balancerConfiguration = balancerConfiguration;
        this.nodeConfiguration = nodeConfiguration;
        this.mcmpHandlerConfiguration = mcmpHandlerConfiguration;
    }

    @Override
    public AdvertiseConfiguration getAdvertiseConfiguration() {
        return advertiseConfiguration;
    }

    @Override
    public BalancerConfiguration getBalancerConfiguration() {
        return balancerConfiguration;
    }

    @Override
    public NodeConfiguration getNodeConfiguration() {
        return nodeConfiguration;
    }

    @Override
    public MCMPHandlerConfiguration getMCMPHandlerConfiguration() {
        return mcmpHandlerConfiguration;
    }
}
