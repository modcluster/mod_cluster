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
package org.jboss.modcluster.load.impl;

import java.util.Map;

/**
 * @author Paul Ferraro
 */
public interface DynamicLoadBalanceFactorProviderMBean {
    /**
     * Returns the history count.
     *
     * @return a positive integer
     */
    int getHistory();

    /**
     * Returns the exponential decay factor.
     *
     * @return a positive integer
     */
    float getDecayFactor();

    /**
     * Sets the number of historical load values to consider when calculating the load balance factor.
     *
     * @param history
     */
    void setHistory(int history);

    /**
     * Sets the exponential decay factor to be applied to historical load values.
     *
     * @param decayFactor the new decay factor
     */
    void setDecayFactor(float decayFactor);

    /**
     * Returns the load metrics registered with this provider
     *
     * @return a collection of load metrics
     */
    Map<String, Double> getMetrics();
}
