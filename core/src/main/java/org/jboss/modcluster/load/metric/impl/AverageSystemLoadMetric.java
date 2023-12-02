/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.load.metric.impl;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

import org.jboss.modcluster.ModClusterLogger;
import org.jboss.modcluster.container.Engine;

/**
 * Uses {@link OperatingSystemMXBean#getSystemLoadAverage} to calculate average system load.
 *
 * @author Paul Ferraro
 */
public class AverageSystemLoadMetric extends AbstractLoadMetric {
    private final OperatingSystemMXBean bean;

    public AverageSystemLoadMetric() {
        this(ManagementFactory.getOperatingSystemMXBean());
    }

    public AverageSystemLoadMetric(OperatingSystemMXBean bean) {
        this.bean = bean;
    }

    @Override
    public double getLoad(Engine engine) throws Exception {
        double load = this.bean.getSystemLoadAverage();
        if (load < 0) {
            ModClusterLogger.LOGGER.notSupportedOnSystem(this.getClass().getSimpleName());
            this.setWeight(0);
            return 0;
        }
        return load / this.bean.getAvailableProcessors();
    }
}
