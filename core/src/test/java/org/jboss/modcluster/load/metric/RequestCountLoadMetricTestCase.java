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
import org.jboss.modcluster.load.metric.impl.RequestCountLoadMetric;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class RequestCountLoadMetricTestCase {
    @Test
    public void getLoad() throws Exception {
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
