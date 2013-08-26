/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.modcluster.load.metric.impl;

import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.load.metric.LoadMetric;

/**
 * {@link LoadMetric} implementation that returns the total number of busy connector threads.
 * 
 * @author Paul Ferraro
 */
public class BusyConnectorsLoadMetric extends AbstractLoadMetric {
    @Override
    public double getLoad(Engine engine) throws Exception {
        double busy = 0;
        double max = 0;
        boolean useCapacity = false;

        for (Connector connector : engine.getConnectors()) {
            busy += connector.getBusyThreads();
            int maxThreads = connector.getMaxThreads();

            // If connector does not maintain a corresponding explicit capacity value (e.g. Undertow listener), leave
            // load calculation to defined capacity.
            if (maxThreads == -1) {
                useCapacity = true;
            }
            max += maxThreads;
        }
        return useCapacity ? busy : busy / max;
    }
}
