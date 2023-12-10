/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.config.builder;

import org.jboss.modcluster.config.ModClusterConfiguration;

/**
 * @author Radoslav Husar
 * @since 1.3.6.Final
 */
class AbstractConfigurationBuilder implements ConfigurationBuilder {

    private final ConfigurationBuilder parent;

    AbstractConfigurationBuilder(ConfigurationBuilder parent) {
        this.parent = parent;
    }

    @Override
    public AdvertiseConfigurationBuilder advertise() {
        return parent.advertise();
    }

    @Override
    public BalancerConfigurationBuilder balancer() {
        return parent.balancer();
    }

    @Override
    public NodeConfigurationBuilder node() {
        return parent.node();
    }

    @Override
    public MCMPHandlerConfigurationBuilder mcmp() {
        return parent.mcmp();
    }

    @Override
    public ModClusterConfiguration build() {
        return parent.build();
    }
}