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

import java.util.List;

import javax.management.JMException;

import org.jboss.modcluster.load.metric.LoadMetric;

/**
 * Generic {@link LoadMetric} whose load is the ratio of 2 aggregated mbean attributes.
 * @author Paul Ferraro
 */
public class MBeanAttributeRatioLoadMetric extends SourcedLoadMetric<MBeanQueryLoadContext>
{
   private final String dividendAttribute;
   private final String divisorAttribute;
   
   public MBeanAttributeRatioLoadMetric(MBeanQueryLoadMetricSource source, String currentAttribute, String maxAttribute)
   {
      super(source);
      
      this.dividendAttribute = currentAttribute;
      this.divisorAttribute = maxAttribute;
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.load.metric.LoadMetric#getLoad()
    */
   public double getLoad(MBeanQueryLoadContext context) throws JMException
   {
      double dividend = 0;
      
      List<Number> results = context.getAttributes(this.dividendAttribute, Number.class);
      
      for (Number result: results)
      {
         dividend += result.doubleValue();
      }
      
      double divisor = 0;
      
      results = context.getAttributes(this.divisorAttribute, Number.class);
      
      for (Number result: results)
      {
         divisor += result.doubleValue();
      }
      
      return dividend / divisor;
   }
}
