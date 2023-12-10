/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.load.metric.impl;

import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Host;
import org.jboss.modcluster.load.metric.LoadMetric;

/**
 * {@link LoadMetric} implementation that returns the total number of active web sessions.
 *
 * @author Paul Ferraro
 */
public class ActiveSessionsLoadMetric extends AbstractLoadMetric {
    @Override
    public double getLoad(Engine engine) {
        int sessions = 0;
        for (Host host : engine.getHosts()) {
            for (Context context : host.getContexts()) {
                sessions += context.getActiveSessionCount();
            }
        }
        return sessions;
    }
}
