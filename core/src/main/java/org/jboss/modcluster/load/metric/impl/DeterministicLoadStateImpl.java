/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.load.metric.impl;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Computes incremental load change per second from record of previous load.
 *
 * @author Paul Ferraro
 */
public class DeterministicLoadStateImpl implements DeterministicLoadState {
    private final AtomicReference<Double> previousLoad = new AtomicReference<>((double) 0);
    private final AtomicLong previousTime = new AtomicLong(System.currentTimeMillis());

    @Override
    public double delta(double currentLoad) {
        long currentTime = System.currentTimeMillis();
        long previousTime = this.previousTime.getAndSet(currentTime);

        double previousLoad = this.previousLoad.getAndSet(currentLoad);

        double seconds = (currentTime - previousTime) / 1000d;

        // Normalize by time interval (in seconds)
        return (currentLoad - previousLoad) / seconds;
    }

    public long getPreviousTime() {
        return this.previousTime.get();
    }
}
