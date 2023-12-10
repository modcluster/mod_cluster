/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.load.metric;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.management.OperatingSystemMXBean;

import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.load.metric.impl.AverageSystemLoadMetric;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class AverageSystemLoadMetricTestCase {
    @Test
    public void getLoad() throws Exception {
        OperatingSystemMXBean bean = mock(OperatingSystemMXBean.class);
        LoadMetric metric = new AverageSystemLoadMetric(bean);
        Engine engine = mock(Engine.class);

        when(bean.getSystemLoadAverage()).thenReturn(0.50);
        when(bean.getAvailableProcessors()).thenReturn(2);

        double load = metric.getLoad(engine);

        assertEquals(0.25, load, 0.0);
    }
}
