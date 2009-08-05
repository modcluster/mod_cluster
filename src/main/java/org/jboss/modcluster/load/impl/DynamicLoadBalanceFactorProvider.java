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
package org.jboss.modcluster.load.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;
import org.jboss.modcluster.load.LoadBalanceFactorProvider;
import org.jboss.modcluster.load.metric.LoadContext;
import org.jboss.modcluster.load.metric.LoadMetric;
import org.jboss.modcluster.load.metric.LoadMetricSource;

/**
 * {@link LoadBalanceFactorProvider} implementation that periodically aggregates load from a set of {@link LoadMetricSource}s.
 * 
 * @author Paul Ferraro
 */
public class DynamicLoadBalanceFactorProvider implements LoadBalanceFactorProvider, DynamicLoadBalanceFactorProviderMBean
{
   private final Logger log = Logger.getLogger(this.getClass());
   
   private final Map<LoadMetricSource<LoadContext>, Collection<LoadMetric<LoadContext>>> metrics = new LinkedHashMap<LoadMetricSource<LoadContext>, Collection<LoadMetric<LoadContext>>>();
   private final Map<LoadMetric<LoadContext>, List<Double>> loadHistory = new HashMap<LoadMetric<LoadContext>, List<Double>>();
   
   private volatile int decayFactor = 2;
   private volatile int history = 9;

   public DynamicLoadBalanceFactorProvider(Set<LoadMetric<LoadContext>> metrics)
   {
      for (LoadMetric<LoadContext> metric: metrics)
      {
         LoadMetricSource<LoadContext> source = metric.getSource();
         Collection<LoadMetric<LoadContext>> sourceMetrics = this.metrics.get(source);
         
         if (sourceMetrics == null)
         {
            sourceMetrics = new ArrayList<LoadMetric<LoadContext>>();
            this.metrics.put(source, sourceMetrics);
         }
         
         sourceMetrics.add(metric);
      }
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.load.LoadBalanceFactorProvider#getLoadBalanceFactor()
    */
   public synchronized int getLoadBalanceFactor()
   {
      int totalWeight = 0;
      double totalWeightedLoad = 0;
      
      for (Map.Entry<LoadMetricSource<LoadContext>, Collection<LoadMetric<LoadContext>>> entry: this.metrics.entrySet())
      {
         LoadMetricSource<LoadContext> source = entry.getKey();         
         Collection<LoadMetric<LoadContext>> metrics = entry.getValue();
         
         boolean skip = true;
         
         // Skip this source if all weights are 0
         for (LoadMetric<LoadContext> metric: metrics)
         {
            skip = skip && (metric.getWeight() <= 0);
         }

         if (!skip)
         {
            LoadContext context = source.createContext();
            
            try
            {
               for (LoadMetric<LoadContext> metric: metrics)
               {
                  int weight = metric.getWeight();
                  
                  if (weight > 0)
                  {
                     try
                     {
                        // Normalize load with respect to capacity
                        List<Double> queue = this.recordLoad(metric, metric.getLoad(context) / metric.getCapacity());
                        
                        totalWeight += weight;
                        totalWeightedLoad += this.average(queue) * weight;
                     }
                     catch (Exception e)
                     {
                        this.log.error(e.getMessage(), e);
                     }
                  }
               }
            }
            finally
            {
               context.close();
            }
         }
      }

      // Convert load ratio to integer percentage
      int load = (int) Math.round(100 * totalWeightedLoad / totalWeight);
      
      // apply ceiling & floor and invert to express as "load factor"
      return 100 - Math.max(0, Math.min(load, 100));
   }
   
   private List<Double> recordLoad(LoadMetric<LoadContext> metric, double load)
   {
      List<Double> queue = this.loadHistory.get(metric);
      
      if (queue == null)
      {
         queue = new ArrayList<Double>(this.history + 1);
         
         this.loadHistory.put(metric, queue);
      }
      else
      {
         // History could have changed, so prune queue accordingly
         for (int i = (queue.size() - 1); i >= this.history; --i)
         {
            queue.remove(i);
         }
      }
      
      // Add new load to the front
      queue.add(0, new Double(load));
      
      return queue;
   }
   
   /**
    * Compute historical average using time decay function
    * @param queue
    * @return
    */
   private double average(List<Double> queue)
   {
      assert !queue.isEmpty();
      
      double totalLoad = 0;
      double totalDecay = 0;
      double decayFactor = this.decayFactor;
      
      // Historical value contribute an exponentially decayed factor
      for (int i = 0; i < queue.size(); ++i)
      {
         double decay = 1 / Math.pow(decayFactor, i);
         
         totalDecay += decay;
         totalLoad += queue.get(i).doubleValue() * decay;
      }
      
      return totalLoad / totalDecay;
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.load.impl.DynamicLoadBalanceFactorProviderMBean#getDecayFactor()
    */
   public int getDecayFactor()
   {
      return this.decayFactor;
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.load.impl.DynamicLoadBalanceFactorProviderMBean#setDecayFactor(int)
    */
   public void setDecayFactor(int decayFactor)
   {
      this.decayFactor = Math.max(1, decayFactor);
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.load.impl.DynamicLoadBalanceFactorProviderMBean#getHistory()
    */
   public int getHistory()
   {
      return this.history;
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.load.impl.DynamicLoadBalanceFactorProviderMBean#setHistory(int)
    */
   public void setHistory(int history)
   {
      this.history = Math.max(0, history);
   }
}
