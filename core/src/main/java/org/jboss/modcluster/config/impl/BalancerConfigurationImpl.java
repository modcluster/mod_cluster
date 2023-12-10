/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.config.impl;

import org.jboss.modcluster.config.BalancerConfiguration;

/**
 * @author Radoslav Husar
 * @since 1.3.6.Final
 */
public class BalancerConfigurationImpl implements BalancerConfiguration {

    private final boolean stickySession;
    private final boolean stickySessionRemove;
    private final boolean stickySessionForce;
    private final int workerTimeout;
    private final int maxAttempts;

    public BalancerConfigurationImpl(boolean stickySession, boolean stickySessionRemove, boolean stickySessionForce, int workerTimeout, int maxAttempts) {
        this.stickySession = stickySession;
        this.stickySessionRemove = stickySessionRemove;
        this.stickySessionForce = stickySessionForce;
        this.workerTimeout = workerTimeout;
        this.maxAttempts = maxAttempts;
    }

    @Override
    public boolean getStickySession() {
        return stickySession;
    }

    @Override
    public boolean getStickySessionRemove() {
        return stickySessionRemove;
    }

    @Override
    public boolean getStickySessionForce() {
        return stickySessionForce;
    }

    @Override
    public int getWorkerTimeout() {
        return workerTimeout;
    }

    @Override
    public int getMaxAttempts() {
        return maxAttempts;
    }
}
