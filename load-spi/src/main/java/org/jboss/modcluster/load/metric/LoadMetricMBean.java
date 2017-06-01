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
package org.jboss.modcluster.load.metric;

/**
 * @author Paul Ferraro
 * 
 */
public interface LoadMetricMBean {
    /**
     * Returns the "weight" of this metric, i.e. significance of this load metric compared to the other metrics.
     * 
     * @return the weight of the metric
     */
    int getWeight();

    /**
     * Returns the load capacity of this metric. Used to normalize the value returned by {@link #getLoad()} expressed as a
     * percentage of the capacity, such that: 0 <= ({@link #getLoad()} / {@link #getCapacity()}) < 1
     * 
     * @return the estimated capacity of this metric.
     */
    double getCapacity();

    /**
     * Returns the current load of this metric as a percent of the metric's capacity.
     * 
     * @return raw load / capacity.
     * @throws Exception if the load could not be determined.
     */
    // double getLoad() throws Exception;

    void setWeight(int weight);

    void setCapacity(double capacity);
}
