/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.config;

/**
 * @author Radoslav Husar
 */
public interface ModClusterConfiguration {

    AdvertiseConfiguration getAdvertiseConfiguration();

    BalancerConfiguration getBalancerConfiguration();

    NodeConfiguration getNodeConfiguration();

    MCMPHandlerConfiguration getMCMPHandlerConfiguration();

}