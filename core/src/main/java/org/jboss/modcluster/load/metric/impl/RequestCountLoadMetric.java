/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.load.metric.impl;

import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.load.metric.LoadMetric;

/**
 * {@link LoadMetric} implementation that returns the number of web requests.
 *
 * @author Paul Ferraro
 */
public class RequestCountLoadMetric extends AbstractLoadMetric {
    private final DeterministicLoadState state;

    public RequestCountLoadMetric() {
        this(new DeterministicLoadStateImpl());
    }

    public RequestCountLoadMetric(DeterministicLoadState state) {
        this.state = state;
    }

    @Override
    public double getLoad(Engine engine) throws Exception {
        long count = 0;
        for (Connector connector : engine.getConnectors()) {
            count += connector.getRequestCount();
        }
        return this.state.delta(count);
    }
}
