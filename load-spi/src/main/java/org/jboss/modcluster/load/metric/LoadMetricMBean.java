/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.load.metric;

/**
 * @author Paul Ferraro
 */
public interface LoadMetricMBean {
    /**
     * Returns the "weight" of this metric, i.e. significance of this load metric compared to the other metrics.
     *
     * @return the weight of the metric
     */
    int getWeight();

    /**
     * Returns the load capacity of this metric. Used to normalize the value returned by {@link LoadMetric#getLoad} expressed as a
     * percentage of the capacity, such that: 0 <= ({@link LoadMetric#getLoad} / {@link #getCapacity()}) < 1
     *
     * @return the estimated capacity of this metric.
     */
    double getCapacity();

    /**
     * Sets the weight of the metric relative to the other metrics. Defaults to 1.
     *
     * @param weight weight of the metric.
     */
    void setWeight(int weight);

    /**
     * Sets the maximum capacity of this metric. Used for metrics that do not have an implicit capacity and need to be configured. Defaults to 1.
     *
     * @param capacity non-negative capacity of the metric.
     */
    void setCapacity(double capacity);
}
