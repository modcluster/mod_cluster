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

import javax.management.AttributeNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.logging.Logger;
import org.jboss.modcluster.ModClusterLogger;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.load.metric.LoadMetric;

/**
 * {@link LoadMetric} implementation that uses com.sun.management.OperatingSystemMXBean to determine system memory usage.
 * 
 * @author Paul Ferraro
 */
public class SystemMemoryUsageLoadMetric extends AbstractLoadMetric {
    public static final String FREE_MEMORY = "FreePhysicalMemorySize";
    public static final String TOTAL_MEMORY = "TotalPhysicalMemorySize";

    private volatile MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    
    public void setMBeanServer(MBeanServer server) {
        this.server = server;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.modcluster.load.metric.LoadMetric#getLoad(org.jboss.modcluster.container.Engine)
     */
    @Override
    public double getLoad(Engine engine) throws Exception {
        try {
            ObjectName name = ObjectName.getInstance(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
            double free = ((Number) this.server.getAttribute(name, FREE_MEMORY)).doubleValue();
            double total = ((Number) this.server.getAttribute(name, TOTAL_MEMORY)).doubleValue();
            return (total - free) / total;
        } catch (AttributeNotFoundException e) {
            ModClusterLogger.LOGGER.missingOSBean(this.getClass().getSimpleName());
            this.setWeight(0);
            return 0;
        }
    }
}
