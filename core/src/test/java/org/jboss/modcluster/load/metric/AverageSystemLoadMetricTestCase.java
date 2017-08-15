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
