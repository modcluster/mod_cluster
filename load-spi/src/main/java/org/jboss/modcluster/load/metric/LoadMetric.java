/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.load.metric;

import org.jboss.modcluster.container.Engine;

/**
 * Represents a specific load metric.
 *
 * @author Paul Ferraro
 */
public interface LoadMetric extends LoadMetricMBean {
    double DEFAULT_CAPACITY = 1;
    int DEFAULT_WEIGHT = 1;

    /**
     * Returns the current load of this metric as a percent of the metric's capacity.
     *
     * @return raw load / capacity.
     * @throws NodeUnavailableException if the node should be put into the error state.
     * @throws Exception                if the load could not be determined.
     */
    double getLoad(Engine engine) throws Exception;
}
