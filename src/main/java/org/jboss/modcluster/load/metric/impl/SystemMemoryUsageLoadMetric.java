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
package org.jboss.modcluster.load.metric.impl;

import javax.management.AttributeNotFoundException;
import javax.management.JMException;
import javax.management.MalformedObjectNameException;

import org.jboss.logging.Logger;
import org.jboss.modcluster.load.metric.LoadMetric;

/**
 * {@link LoadMetric} implementation that uses {@link com.sun.management.OperatingSystemMXBean}
 * to determine system memory usage.
 * 
 * @author Paul Ferraro
 */
public class SystemMemoryUsageLoadMetric extends SourcedLoadMetric<MBeanLoadContext>
{
   public static final String FREE_MEMORY = "FreePhysicalMemorySize";
   public static final String TOTAL_MEMORY = "TotalPhysicalMemorySize";
   
   private Logger logger = Logger.getLogger(this.getClass());

   public SystemMemoryUsageLoadMetric() throws MalformedObjectNameException
   {
      super(new OperatingSystemLoadMetricSource());
   }
   
   public SystemMemoryUsageLoadMetric(OperatingSystemLoadMetricSource source)
   {
      super(source);
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.load.metric.LoadMetric#getLoad()
    */
   public double getLoad(MBeanLoadContext context) throws JMException
   {
      try
      {
         double free = context.getAttribute(FREE_MEMORY, Number.class).doubleValue();
         double total = context.getAttribute(TOTAL_MEMORY, Number.class).doubleValue();
         
         return (total - free) / total;
      }
      catch (AttributeNotFoundException e)
      {
         this.logger.warn(this.getClass().getSimpleName() + " requires com.sun.management.OperatingSystemMXBean.");
         
         this.setWeight(0);
         
         return 0;
      }
   }
}
