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

import java.util.List;

import javax.management.JMException;

/**
 * Generic {@link LoadMetric} whose load is the aggregated value of an mbean attribute.
 * @author Paul Ferraro
 */
public class MBeanAttributeLoadMetric extends SourcedLoadMetric<MBeanQueryLoadContext>
{
   private final String attribute;
   
   public MBeanAttributeLoadMetric(MBeanQueryLoadMetricSource source, String attribute)
   {
      super(source);
      
      this.attribute = attribute;
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.load.metric.LoadMetric#getLoad(org.jboss.modcluster.load.metric.LoadContext)
    */
   public double getLoad(MBeanQueryLoadContext context) throws JMException
   {
      double load = 0;
      
      List<Number> results = context.getAttributes(this.attribute, Number.class);
      
      for (Number result: results)
      {
         load += result.doubleValue();
      }
      
      return load;
   }
}
