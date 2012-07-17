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

import java.lang.management.OperatingSystemMXBean;

import javax.management.AttributeNotFoundException;
import javax.management.JMException;
import javax.management.MalformedObjectNameException;

import org.jboss.logging.Logger;

/**
 * Uses {@link OperatingSystemMXBean#getSystemLoadAverage} to calculate average system load.
 * Only supported on Java 1.6 or later.
 *
 * @author Paul Ferraro
 */
public class AverageSystemLoadMetric extends SourcedLoadMetric<MBeanLoadContext>
{
   public static final String SYSTEM_LOAD_AVERAGE = "SystemLoadAverage";
   public static final String AVAILABLE_PROCESSORS = "AvailableProcessors";
   
   private final Logger logger = Logger.getLogger(this.getClass());
   
   public AverageSystemLoadMetric() throws MalformedObjectNameException
   {
      super(new OperatingSystemLoadMetricSource());
   }
   
   public AverageSystemLoadMetric(OperatingSystemLoadMetricSource source)
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
         double load = context.getAttribute(SYSTEM_LOAD_AVERAGE, Double.class).doubleValue();
         if (load < 0)
         {
            this.logger.warn(this.getClass().getSimpleName() + " is unsupported and will be disabled.");
            // Disable this metric
            this.setWeight(0);
            return 0;
         }
         return load / context.getAttribute(AVAILABLE_PROCESSORS, Integer.class).intValue();
      }
      catch (AttributeNotFoundException e)
      {
         this.logger.warn(this.getClass().getSimpleName() + " requires Java 1.6 or later.");
         // Disable this metric
         this.setWeight(0);
         return 0;
      }
   }
}
