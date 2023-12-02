/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.load.metric.impl;

import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.load.metric.LoadMetric;

/**
 * {@link LoadMetric} implementation that returns the incoming bandwidth in KB.
 *
 * @author Paul Ferraro
 */
public class ReceiveTrafficLoadMetric extends AbstractLoadMetric {
    private final DeterministicLoadState state;

    public ReceiveTrafficLoadMetric() {
        this(new DeterministicLoadStateImpl());
    }

    public ReceiveTrafficLoadMetric(DeterministicLoadState state) {
        this.state = state;
    }

    @Override
    public double getLoad(Engine engine) throws Exception {
        double bytes = 0;
        for (Connector connector : engine.getConnectors()) {
            bytes += connector.getBytesReceived();
        }
        // Convert to KB/sec
        return this.state.delta(bytes / 1000);
    }
}
