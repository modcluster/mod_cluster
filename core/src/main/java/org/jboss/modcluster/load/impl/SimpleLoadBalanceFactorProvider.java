/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.load.impl;

import org.jboss.modcluster.container.Engine;

/**
 * A {@link org.jboss.modcluster.load.LoadBalanceFactorProvider} that returns a static value.
 *
 * @author Brian Stansberry
 */
public class SimpleLoadBalanceFactorProvider implements SimpleLoadBalanceFactorProviderMBean {
    private int loadBalanceFactor = 1;

    @Override
    public int getLoadBalanceFactor(Engine engine) {
        return this.loadBalanceFactor;
    }

    @Override
    public void setLoadBalanceFactor(int loadBalanceFactor) {
        this.loadBalanceFactor = loadBalanceFactor;
    }
}
