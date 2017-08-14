/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
