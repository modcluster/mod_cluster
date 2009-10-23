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

import javax.management.MalformedObjectNameException;

import org.jboss.modcluster.load.metric.LoadMetric;

/**
 * {@link LoadMetric} implementation that returns the usage ratio of a connection pool.
 * 
 * @author Paul Ferraro
 */
public class ConnectionPoolUsageLoadMetric extends MBeanAttributeRatioLoadMetric
{
   public static final String DEFAULT_DIVIDEND_ATTRIBUTE = "InUseConnectionCount";
   public static final String DEFAULT_DIVISOR_ATTRIBUTE = "MaxSize";
   
   public ConnectionPoolUsageLoadMetric() throws MalformedObjectNameException
   {
      this(new ConnectionPoolLoadMetricSource());
   }
   
   public ConnectionPoolUsageLoadMetric(ConnectionPoolLoadMetricSource source)
   {
      super(source, DEFAULT_DIVIDEND_ATTRIBUTE, DEFAULT_DIVISOR_ATTRIBUTE);
   }
   
   public ConnectionPoolUsageLoadMetric(ConnectionPoolLoadMetricSource source, String usedAttribute, String maxAttribute)
   {
      super(source, usedAttribute, maxAttribute);
   }
}
