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
import org.jboss.modcluster.load.metric.impl.BusyConnectorsLoadMetric;
import org.junit.jupiter.api.Test;

/**
 * @author Paul Ferraro
 */
class BusyConnectorsLoadMetricTestCase {
    @Test
    void getLoad() throws Exception {
        LoadMetric metric = new BusyConnectorsLoadMetric();
        Engine engine = mock(Engine.class);
        Connector connector = mock(Connector.class);

        when(engine.getConnectors()).thenReturn(Collections.singleton(connector));
        when(connector.getBusyThreads()).thenReturn(25);
        when(connector.getMaxThreads()).thenReturn(100);

        double load = metric.getLoad(engine);

        assertEquals(0.25, load, 0.0);
    }
}
