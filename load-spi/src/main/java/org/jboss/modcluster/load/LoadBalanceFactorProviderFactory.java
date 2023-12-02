/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.load;

/**
 * @author Paul Ferraro
 */
public interface LoadBalanceFactorProviderFactory {
    LoadBalanceFactorProvider createLoadBalanceFactorProvider();
}
