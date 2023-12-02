/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.load.metric.impl;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.load.metric.LoadMetric;

/**
 * {@link LoadMetric} implementation that returns the heap memory usage ratio.
 *
 * @author Paul Ferraro
 */
public class HeapMemoryUsageLoadMetric extends AbstractLoadMetric {
    private final MemoryMXBean bean;

    public HeapMemoryUsageLoadMetric() {
        this(ManagementFactory.getMemoryMXBean());
    }

    public HeapMemoryUsageLoadMetric(MemoryMXBean bean) {
        this.bean = bean;
    }

    @Override
    public double getLoad(Engine engine) throws Exception {
        MemoryUsage usage = this.bean.getHeapMemoryUsage();

        long max = usage.getMax();
        double used = usage.getUsed();

        // Max may be undefined, so fall back to committed
        return used / ((max >= 0) ? max : usage.getCommitted());
    }
}
