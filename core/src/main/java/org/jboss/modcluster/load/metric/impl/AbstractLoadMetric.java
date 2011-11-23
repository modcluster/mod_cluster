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

import org.jboss.modcluster.load.metric.LoadMetric;

/**
 * Abstract {@link LoadMetric} implementation with basic mutators/accessors.
 * 
 * @author Paul Ferraro
 */
public abstract class AbstractLoadMetric implements LoadMetric {
    private volatile int weight = LoadMetric.DEFAULT_WEIGHT;
    private volatile double capacity = LoadMetric.DEFAULT_CAPACITY;

    @Override
    public int getWeight() {
        return this.weight;
    }

    @Override
    public void setWeight(int weight) {
        if (weight < 0) {
            throw new IllegalArgumentException("Capacity must be greater than or equal to zero.");
        }
        this.weight = weight;
    }

    @Override
    public double getCapacity() {
        return this.capacity;
    }

    @Override
    public void setCapacity(double capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than zero.");
        }
        this.capacity = capacity;
    }
}