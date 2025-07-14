/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.load.metric;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.load.metric.impl.DeterministicLoadState;
import org.jboss.modcluster.load.metric.impl.RequestCountLoadMetric;
import org.junit.jupiter.api.Test;

/**
 * @author Paul Ferraro
 */
class RequestCountLoadMetricTestCase {
    @Test
    void getLoad() throws Exception {
        Engine engine = mock(Engine.class);
        Connector connector = mock(Connector.class);
        DeterministicLoadState state = mock(DeterministicLoadState.class);

        LoadMetric metric = new RequestCountLoadMetric(state);

        when(engine.getConnectors()).thenReturn(Collections.singleton(connector));
        when(connector.getRequestCount()).thenReturn(10L);
        when(state.delta(10.0)).thenReturn(9.0);

        double load = metric.getLoad(engine);

        assertEquals(9.0, load, 0.0);
    }
}
