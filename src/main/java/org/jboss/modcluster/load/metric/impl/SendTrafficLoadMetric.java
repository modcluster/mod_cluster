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

import javax.management.JMException;
import javax.management.MalformedObjectNameException;

import org.jboss.modcluster.load.metric.DeterministicLoadState;
import org.jboss.modcluster.load.metric.LoadMetric;

/**
 * {@link LoadMetric} implementation that returns the outgoing bandwidth in KB.
 * 
 * @author Paul Ferraro
 */
public class SendTrafficLoadMetric extends MBeanAttributeLoadMetric
{
   public static final String DEFAULT_ATTRIBUTE = "bytesSent";
   
   private final DeterministicLoadState state;
   
   public SendTrafficLoadMetric() throws MalformedObjectNameException
   {
      this(new RequestProcessorLoadMetricSource());
   }
   
   public SendTrafficLoadMetric(DeterministicLoadState state) throws MalformedObjectNameException
   {
      this(new RequestProcessorLoadMetricSource(), state);
   }
   
   public SendTrafficLoadMetric(RequestProcessorLoadMetricSource source)
   {
      this(source, DEFAULT_ATTRIBUTE);
   }
   
   public SendTrafficLoadMetric(RequestProcessorLoadMetricSource source, DeterministicLoadState state)
   {
      this(source, DEFAULT_ATTRIBUTE, state);
   }
   
   public SendTrafficLoadMetric(RequestProcessorLoadMetricSource source, String attribute)
   {
      this(source, attribute, new DeterministicLoadStateImpl());
   }
   
   public SendTrafficLoadMetric(RequestProcessorLoadMetricSource source, String attribute, DeterministicLoadState state)
   {
      super(source, attribute);
      
      this.state = state;
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.load.metric.impl.MBeanAttributeLoadMetric#getLoad()
    */
   @Override
   public double getLoad(MBeanQueryLoadContext context) throws JMException
   {
      // Convert to KB/sec
      return this.state.delta(super.getLoad(context) / 1000);
   }
}
