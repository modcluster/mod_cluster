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

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.jboss.modcluster.load.metric.LoadMetric;
import org.jboss.modcluster.load.metric.LoadMetricSource;

/**
 * Abstract {@link LoadMetricSource} that queries the mbean server for the set of beans to be made available to its registered {@link LoadMetric}s.
 * 
 * @author Paul Ferraro
 */
public class MBeanQueryLoadMetricSource implements LoadMetricSource<MBeanQueryLoadContext>
{
   private final MBeanServer server;
   private final ObjectName name;
   
   public MBeanQueryLoadMetricSource(String pattern) throws MalformedObjectNameException
   {
      this(pattern, ManagementFactory.getPlatformMBeanServer());
   }
   
   public MBeanQueryLoadMetricSource(String pattern, MBeanServer server) throws MalformedObjectNameException
   {
      this.server = server;
      this.name = ObjectName.getInstance(pattern);
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.load.metric.LoadMetricSource#createContext()
    */
   public MBeanQueryLoadContext createContext()
   {
      return new MBeanQueryLoadContext(this.server, this.name);
   }
}
