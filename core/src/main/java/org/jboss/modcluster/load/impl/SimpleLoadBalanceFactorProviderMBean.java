/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.load.impl;

import org.jboss.modcluster.load.LoadBalanceFactorProvider;

/**
 * @author Paul Ferraro
 */
public interface SimpleLoadBalanceFactorProviderMBean extends LoadBalanceFactorProvider {
    void setLoadBalanceFactor(int loadBalanceFactor);
}
