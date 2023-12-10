/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.load.impl;

import org.jboss.modcluster.load.LoadBalanceFactorProvider;
import org.jboss.modcluster.load.LoadBalanceFactorProviderFactory;

public class SimpleLoadBalanceFactorProviderFactory implements LoadBalanceFactorProviderFactory {
    private final LoadBalanceFactorProvider provider;

    public SimpleLoadBalanceFactorProviderFactory(LoadBalanceFactorProvider provider) {
        this.provider = provider;
    }

    @Override
    public LoadBalanceFactorProvider createLoadBalanceFactorProvider() {
        return this.provider;
    }
}
