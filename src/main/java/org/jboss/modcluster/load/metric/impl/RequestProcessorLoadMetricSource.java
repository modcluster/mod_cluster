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

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;

import org.jboss.modcluster.Utils;
import org.jboss.modcluster.load.metric.LoadMetricSource;

/**
 * {@link LoadMetricSource} implementation that simplifies generic mbean access to 
 * the JBossWeb request processors.
 * 
 * @author Paul Ferraro
 */
public class RequestProcessorLoadMetricSource extends MBeanQueryLoadMetricSource
{
   public static final String DEFAULT_PATTERN = Utils.defaultObjectNameDomain() + ":type=GlobalRequestProcessor,*";
   
   /**
    * Create a new GlobalRequestProcessorLoadMetricSource.
    * 
    * @param registration
    * @throws MalformedObjectNameException 
    */
   public RequestProcessorLoadMetricSource() throws MalformedObjectNameException
   {
      super(DEFAULT_PATTERN);
   }
   
   /**
    * Create a new GlobalRequestProcessorLoadMetricSource.
    * 
    * @param registration
    * @param pattern
    * @throws MalformedObjectNameException 
    */
   public RequestProcessorLoadMetricSource(String pattern) throws MalformedObjectNameException
   {
      super(pattern);
   }

   /**
    * Create a new GlobalRequestProcessorLoadMetricSource.
    * 
    * @param registration
    * @param server
    * @throws MalformedObjectNameException 
    */
   public RequestProcessorLoadMetricSource(MBeanServer server) throws MalformedObjectNameException
   {
      super(DEFAULT_PATTERN, server);
   }

   /**
    * Create a new GlobalRequestProcessorLoadMetricSource.
    * 
    * @param registration
    * @param pattern
    * @param server
    * @throws MalformedObjectNameException 
    */
   public RequestProcessorLoadMetricSource(String pattern, MBeanServer server) throws MalformedObjectNameException
   {
      super(pattern, server);
   }
}
