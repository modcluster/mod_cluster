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

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

import org.jboss.logging.Logger;
import org.jboss.modcluster.ModClusterLogger;
import org.jboss.modcluster.container.Engine;

/**
 * Uses {@link OperatingSystemMXBean#getSystemLoadAverage} to calculate average system load.
 * 
 * @author Paul Ferraro
 */
public class AverageSystemLoadMetric extends AbstractLoadMetric {
    private final OperatingSystemMXBean bean;

    public AverageSystemLoadMetric() {
        this(ManagementFactory.getOperatingSystemMXBean());
    }

    public AverageSystemLoadMetric(OperatingSystemMXBean bean) {
        this.bean = bean;
    }

    @Override
    public double getLoad(Engine engine) throws Exception {
        double load = this.bean.getSystemLoadAverage();
        if (load < 0) {
            ModClusterLogger.LOGGER.notSupportedOnSystem(this.getClass().getSimpleName());
            this.setWeight(0);
            return 0;
        }
        return load / this.bean.getAvailableProcessors();
    }
}
