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
