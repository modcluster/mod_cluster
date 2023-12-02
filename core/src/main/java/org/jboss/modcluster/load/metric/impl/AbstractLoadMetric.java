/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.load.metric.impl;

import org.jboss.modcluster.ModClusterMessages;
import org.jboss.modcluster.load.metric.LoadMetric;

/**
 * Abstract {@link LoadMetric} implementation with basic mutators/accessors.
 *
 * @author Paul Ferraro
 */
public abstract class AbstractLoadMetric implements LoadMetric {
    private volatile int weight = LoadMetric.DEFAULT_WEIGHT;
    private volatile double capacity = LoadMetric.DEFAULT_CAPACITY;

    @Override
    public int getWeight() {
        return this.weight;
    }

    @Override
    public void setWeight(int weight) {
        if (weight < 0) {
            throw ModClusterMessages.MESSAGES.invalidWeight();
        }
        this.weight = weight;
    }

    @Override
    public double getCapacity() {
        return this.capacity;
    }

    @Override
    public void setCapacity(double capacity) {
        if (capacity <= 0) {
            throw ModClusterMessages.MESSAGES.invalidCapacity();
        }
        this.capacity = capacity;
    }
}