/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.load.metric;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.load.metric.impl.DeterministicLoadState;
import org.jboss.modcluster.load.metric.impl.ReceiveTrafficLoadMetric;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class ReceiveTrafficLoadMetricTestCase {
    @Test
    public void getLoad() throws Exception {
        Engine engine = mock(Engine.class);
        Connector connector = mock(Connector.class);
        DeterministicLoadState state = mock(DeterministicLoadState.class);

        LoadMetric metric = new ReceiveTrafficLoadMetric(state);

        when(engine.getConnectors()).thenReturn(Collections.singleton(connector));
        when(connector.getBytesReceived()).thenReturn(10000L);
        when(state.delta(10)).thenReturn(9.0);

        double load = metric.getLoad(engine);

        assertEquals(9.0, load, 0.0);
    }
}
