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

import org.jboss.modcluster.load.metric.LoadContext;
import org.jboss.modcluster.load.metric.LoadMetric;
import org.jboss.modcluster.load.metric.LoadMetricMBean;

/**
 * Abstract {@link LoadMetric} implementation with basic mutators/accessors.
 * 
 * @author Paul Ferraro
 */
public abstract class AbstractLoadMetric<C extends LoadContext> implements LoadMetric<C>, LoadMetricMBean
{
   private volatile int weight = LoadMetric.DEFAULT_WEIGHT;
   private volatile double capacity = LoadMetric.DEFAULT_CAPACITY;

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.load.metric.LoadMetric#getWeight()
    */
   public int getWeight()
   {
      return this.weight;
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.load.metric.LoadMetricMBean#setWeight(int)
    */
   public void setWeight(int weight)
   {
      this.weight = weight;
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.load.metric.LoadMetric#getCapacity()
    */
   public double getCapacity()
   {
      return this.capacity;
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.load.metric.LoadMetricMBean#setCapacity(double)
    */
   public void setCapacity(double capacity)
   {
      if (capacity <= 0)
      {
         throw new IllegalArgumentException("Capacity must be greater than zero.");
      }
      
      this.capacity = capacity;
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.modcluster.load.metric.LoadMetricMBean#getLoad()
    */
   public double getLoad() throws Exception
   {
      C context = this.getSource().createContext();
      
      try
      {
         return this.getLoad(context) / this.capacity;
      }
      finally
      {
         context.close();
      }
   }
}