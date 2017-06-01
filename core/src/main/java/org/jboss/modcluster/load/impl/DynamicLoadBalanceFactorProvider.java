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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.jboss.logging.Logger;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.load.LoadBalanceFactorProvider;
import org.jboss.modcluster.load.metric.LoadMetric;
import org.jboss.modcluster.load.metric.NodeUnavailableException;

/**
 * {@link LoadBalanceFactorProvider} implementation that periodically aggregates load from a set of {@link LoadMetric}s.
 * 
 * @author Paul Ferraro
 */
public class DynamicLoadBalanceFactorProvider implements LoadBalanceFactorProvider, DynamicLoadBalanceFactorProviderMBean {
    public static final int DEFAULT_DECAY_FACTOR = 2;
    public static final int DEFAULT_HISTORY = 9;

    private final Logger log = Logger.getLogger(this.getClass());

    private final Map<LoadMetric, List<Double>> loadHistory = new LinkedHashMap<LoadMetric, List<Double>>();

    private volatile int decayFactor = 2;
    private volatile int history = 9;

    public DynamicLoadBalanceFactorProvider(Set<LoadMetric> metrics) {
        for (LoadMetric metric : metrics) {
            this.loadHistory.put(metric, new ArrayList<Double>(this.history + 1));
        }
    }

    @Override
    public synchronized Map<String, Double> getMetrics() {
        Map<String, Double> metrics = new TreeMap<String, Double>();
        for (Map.Entry<LoadMetric, List<Double>> entry : this.loadHistory.entrySet()) {
            List<Double> history = entry.getValue();
            metrics.put(entry.getKey().getClass().getSimpleName(), history.isEmpty() ? 0 : history.get(0));
        }
        return metrics;
    }

    @Override
    public synchronized int getLoadBalanceFactor(Engine engine) {
        boolean nodeUnavailable = false;
        int totalWeight = 0;
        double totalWeightedLoad = 0;

        for (Map.Entry<LoadMetric, List<Double>> entry : this.loadHistory.entrySet()) {
            LoadMetric metric = entry.getKey();

            int weight = metric.getWeight();

            if (weight > 0) {
                List<Double> metricLoadHistory = entry.getValue();

                try {
                    // Normalize load with respect to capacity
                    this.recordLoad(metricLoadHistory, metric.getLoad(engine) / metric.getCapacity());

                    totalWeight += weight;
                    totalWeightedLoad += this.average(metricLoadHistory) * weight;
                } catch (NodeUnavailableException e) {
                    // The metric requested to put the node into error state
                    // Call LoadMetric#getLoad on remaining metrics so that historical values are populated
                    nodeUnavailable = true;
                } catch (Exception e) {
                    this.log.error(e.getLocalizedMessage(), e);
                }
            }
        }

        if (nodeUnavailable) {
            return -1;
        }

        // Convert load ratio to integer percentage
        int load = (int) Math.round(100 * totalWeightedLoad / totalWeight);

        // apply ceiling & floor and invert to express as "load factor"
        // result should be a value between 1-100
        return 100 - Math.max(0, Math.min(load, 99));
    }

    private void recordLoad(List<Double> queue, double load) {
        if (!queue.isEmpty()) {
            // History could have changed, so prune queue accordingly
            for (int i = (queue.size() - 1); i >= this.history; --i) {
                queue.remove(i);
            }
        }

        // Add new load to the head
        queue.add(0, load);
    }

    /**
     * Compute historical average using time decay function
     */
    private double average(List<Double> queue) {
        assert !queue.isEmpty();

        double totalLoad = 0;
        double totalDecay = 0;
        double decayFactor = this.decayFactor;

        // Historical value contribute an exponentially decayed factor
        for (int i = 0; i < queue.size(); ++i) {
            double decay = 1 / Math.pow(decayFactor, i);

            totalDecay += decay;
            totalLoad += queue.get(i) * decay;
        }

        return totalLoad / totalDecay;
    }

    @Override
    public int getDecayFactor() {
        return this.decayFactor;
    }

    @Override
    public void setDecayFactor(int decayFactor) {
        this.decayFactor = Math.max(1, decayFactor);
    }

    @Override
    public int getHistory() {
        return this.history;
    }

    @Override
    public void setHistory(int history) {
        this.history = Math.max(0, history);
    }
}
