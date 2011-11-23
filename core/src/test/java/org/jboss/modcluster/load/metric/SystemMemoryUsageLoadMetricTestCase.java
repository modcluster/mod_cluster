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

import java.lang.management.ManagementFactory;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Server;
import org.jboss.modcluster.load.metric.impl.SystemMemoryUsageLoadMetric;
import org.junit.Test;

/**
 * @author Paul Ferraro
 * 
 */
public class SystemMemoryUsageLoadMetricTestCase {
    @Test
    public void getLoad() throws Exception {
        SystemMemoryUsageLoadMetric metric = new SystemMemoryUsageLoadMetric();
        ObjectName name = ObjectName.getInstance(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);

        MBeanServer mbeanServer = mock(MBeanServer.class);
        Engine engine = mock(Engine.class);
        Server server = mock(Server.class);

        when(engine.getServer()).thenReturn(server);
        when(server.getMBeanServer()).thenReturn(mbeanServer);

        when(mbeanServer.getAttribute(name, SystemMemoryUsageLoadMetric.FREE_MEMORY)).thenReturn(256);
        when(mbeanServer.getAttribute(name, SystemMemoryUsageLoadMetric.TOTAL_MEMORY)).thenReturn(1024);

        double load = metric.getLoad(engine);

        assertEquals(0.75, load, 0.0);

        when(mbeanServer.getAttribute(name, SystemMemoryUsageLoadMetric.FREE_MEMORY)).thenThrow(
                new AttributeNotFoundException());

        load = metric.getLoad(engine);

        assertEquals(0.0, load, 0.0);
        assertEquals(0, metric.getWeight());
    }
}
