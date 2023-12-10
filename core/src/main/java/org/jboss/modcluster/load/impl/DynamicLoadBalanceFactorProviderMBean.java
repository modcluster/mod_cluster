/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.load.impl;

import java.util.Map;

/**
 * @author Paul Ferraro
 */
public interface DynamicLoadBalanceFactorProviderMBean {
    /**
     * Returns the history count.
     *
     * @return a positive integer
     */
    int getHistory();

    /**
     * Returns the exponential decay factor.
     *
     * @return a positive integer
     */
    float getDecayFactor();

    /**
     * Sets the number of historical load values to consider when calculating the load balance factor.
     *
     * @param history
     */
    void setHistory(int history);

    /**
     * Sets the exponential decay factor to be applied to historical load values.
     *
     * @param decayFactor the new decay factor
     */
    void setDecayFactor(float decayFactor);

    /**
     * Returns the load metrics registered with this provider
     *
     * @return a collection of load metrics
     */
    Map<String, Double> getMetrics();
}
