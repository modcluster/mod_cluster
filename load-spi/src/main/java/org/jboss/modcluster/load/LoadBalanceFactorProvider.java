/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.load;

import org.jboss.modcluster.container.Engine;

/**
 * Provides the load balance factor for a node.
 *
 * @author Brian Stansberry
 */
public interface LoadBalanceFactorProvider {
    int getLoadBalanceFactor(Engine engine);
}
