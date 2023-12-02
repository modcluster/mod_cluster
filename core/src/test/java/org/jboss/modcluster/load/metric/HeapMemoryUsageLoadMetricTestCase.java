/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.load.metric;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.load.metric.impl.HeapMemoryUsageLoadMetric;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class HeapMemoryUsageLoadMetricTestCase {
    private MemoryMXBean bean = mock(MemoryMXBean.class);
    private HeapMemoryUsageLoadMetric metric = new HeapMemoryUsageLoadMetric(this.bean);

    @Test
    public void getLoad() throws Exception {
        Engine engine = mock(Engine.class);
        MemoryUsage usage = new MemoryUsage(0, 1000, 2000, 5000);

        when(this.bean.getHeapMemoryUsage()).thenReturn(usage);

        double load = this.metric.getLoad(engine);

        assertEquals(0.2, load, 0.0);
    }

    @Test
    public void getLoadNoMax() throws Exception {
        Engine engine = mock(Engine.class);
        MemoryUsage usage = new MemoryUsage(0, 1000, 2000, -1);

        when(this.bean.getHeapMemoryUsage()).thenReturn(usage);

        double load = this.metric.getLoad(engine);

        assertEquals(0.5, load, 0.0);
    }
}
