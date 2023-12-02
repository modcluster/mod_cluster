/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.config.builder;

import org.jboss.modcluster.config.BalancerConfiguration;
import org.jboss.modcluster.config.impl.BalancerConfigurationImpl;

/**
 * Builder for balancer configuration.
 *
 * @author Radoslav Husar
 * @since 1.3.6.Final
 */
public class BalancerConfigurationBuilder extends AbstractConfigurationBuilder implements Creator<BalancerConfiguration> {

    private boolean stickySession = true;
    private boolean stickySessionRemove = false;
    private boolean stickySessionForce = false;
    private int workerTimeout = -1;
    private int maxAttempts = -1;

    BalancerConfigurationBuilder(ConfigurationBuilder parentBuilder) {
        super(parentBuilder);
    }

    /**
     * Enables or disables sticky sessions.
     */
    public BalancerConfigurationBuilder setStickySession(boolean stickySession) {
        this.stickySession = stickySession;
        return this;
    }

    /**
     * Sets to remove session when the request cannot be routed to the right node.
     */
    public BalancerConfigurationBuilder setStickySessionRemove(boolean stickySessionRemove) {
        this.stickySessionRemove = stickySessionRemove;
        return this;
    }

    /**
     * Sets to return an error when the request cannot be routed to the right node.
     */
    public BalancerConfigurationBuilder setStickySessionForce(boolean stickySessionForce) {
        this.stickySessionForce = stickySessionForce;
        return this;
    }

    /**
     * Sets timeout to wait for an available worker (default is no wait).
     */
    public BalancerConfigurationBuilder setWorkerTimeout(int workerTimeout) {
        this.workerTimeout = workerTimeout;
        return this;
    }

    /**
     * Sets the maximum number of attempts to send the request to the backend server.
     */
    public BalancerConfigurationBuilder setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
        return this;
    }

    @Override
    public BalancerConfiguration create() {
        return new BalancerConfigurationImpl(stickySession, stickySessionRemove, stickySessionForce, workerTimeout, maxAttempts);
    }
}