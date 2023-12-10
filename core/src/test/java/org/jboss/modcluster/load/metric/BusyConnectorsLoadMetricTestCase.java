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
import org.jboss.modcluster.load.metric.impl.BusyConnectorsLoadMetric;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class BusyConnectorsLoadMetricTestCase {
    @Test
    public void getLoad() throws Exception {
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
