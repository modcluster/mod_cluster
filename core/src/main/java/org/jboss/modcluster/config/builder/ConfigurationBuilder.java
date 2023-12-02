/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.config.builder;

import org.jboss.modcluster.config.ModClusterConfiguration;

/**
 * Builder for the main mod_cluster configuration object.
 *
 * @author Radoslav Husar
 * @since 1.3.6.Final
 */
public interface ConfigurationBuilder {

    /**
     * Builder for multicast-based advertise configuration.
     */
    AdvertiseConfigurationBuilder advertise();

    /**
     * Builder for balancer configuration.
     */
    BalancerConfigurationBuilder balancer();

    /**
     * Builder for MCMP (Mod-Cluster Management Protocol) handler configuration.
     */
    MCMPHandlerConfigurationBuilder mcmp();

    /**
     * Builder for proxy node configuration.
     */
    NodeConfigurationBuilder node();

    /**
     * Builds the main configuration object.
     */
    ModClusterConfiguration build();
}
