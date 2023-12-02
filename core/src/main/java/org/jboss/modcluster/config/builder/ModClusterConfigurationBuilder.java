/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.config.builder;

import org.jboss.modcluster.config.ModClusterConfiguration;
import org.jboss.modcluster.config.impl.ModClusterConfigurationImpl;

/**
 * Builder for the main mod_cluster configuration object.
 *
 * @author Radoslav Husar
 * @since 1.3.6.Final
 */
public class ModClusterConfigurationBuilder implements ConfigurationBuilder {

    private final AdvertiseConfigurationBuilder advertise;
    private final BalancerConfigurationBuilder balancer;
    private final NodeConfigurationBuilder node;
    private final MCMPHandlerConfigurationBuilder mcmp;

    public ModClusterConfigurationBuilder() {
        advertise = new AdvertiseConfigurationBuilder(this);
        balancer = new BalancerConfigurationBuilder(this);
        node = new NodeConfigurationBuilder(this);
        mcmp = new MCMPHandlerConfigurationBuilder(this);
    }

    @Override
    public NodeConfigurationBuilder node() {
        return node;
    }

    @Override
    public AdvertiseConfigurationBuilder advertise() {
        return advertise;
    }

    @Override
    public BalancerConfigurationBuilder balancer() {
        return balancer;
    }

    @Override
    public MCMPHandlerConfigurationBuilder mcmp() {
        return mcmp;
    }

    @Override
    public ModClusterConfiguration build() {
        return new ModClusterConfigurationImpl(advertise.create(), balancer.create(), node.create(), mcmp.create());
    }
}