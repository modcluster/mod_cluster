/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.load.metric.impl;

import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.load.metric.LoadMetric;

/**
 * {@link LoadMetric} implementation that returns the outgoing bandwidth in KB.
 *
 * @author Paul Ferraro
 */
public class SendTrafficLoadMetric extends AbstractLoadMetric {
    private final DeterministicLoadState state;

    public SendTrafficLoadMetric() {
        this(new DeterministicLoadStateImpl());
    }

    public SendTrafficLoadMetric(DeterministicLoadState state) {
        this.state = state;
    }

    @Override
    public double getLoad(Engine engine) throws Exception {
        double bytes = 0;
        for (Connector connector : engine.getConnectors()) {
            bytes += connector.getBytesSent();
        }
        // Convert to KB/sec
        return this.state.delta(bytes / 1000);
    }
}
