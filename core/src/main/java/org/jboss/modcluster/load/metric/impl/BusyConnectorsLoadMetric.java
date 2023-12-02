/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.load.metric.impl;

import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.load.metric.LoadMetric;

/**
 * {@link LoadMetric} implementation that returns the total number of busy connector threads.
 *
 * @author Paul Ferraro
 */
public class BusyConnectorsLoadMetric extends AbstractLoadMetric {
    @Override
    public double getLoad(Engine engine) throws Exception {
        double busy = 0;
        double max = 0;
        boolean useCapacity = false;

        for (Connector connector : engine.getConnectors()) {
            busy += connector.getBusyThreads();
            int maxThreads = connector.getMaxThreads();

            // If connector does not maintain a corresponding explicit capacity value (e.g. Undertow listener), leave
            // load calculation to defined capacity.
            if (maxThreads == -1) {
                useCapacity = true;
            }
            max += maxThreads;
        }
        return useCapacity ? busy : busy / max;
    }
}
