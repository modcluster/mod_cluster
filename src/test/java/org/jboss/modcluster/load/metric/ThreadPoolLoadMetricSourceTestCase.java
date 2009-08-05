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
package org.jboss.modcluster.load.metric;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;

import org.easymock.EasyMock;
import org.jboss.modcluster.load.metric.impl.ThreadPoolLoadMetricSource;

/**
 * @author Paul Ferraro
 *
 */
public class ThreadPoolLoadMetricSourceTestCase extends MBeanQueryLoadMetricSourceTestCase
{
   public ThreadPoolLoadMetricSourceTestCase() throws MalformedObjectNameException
   {
      this(EasyMock.createStrictMock(MBeanServer.class));
   }

   private ThreadPoolLoadMetricSourceTestCase(MBeanServer server) throws MalformedObjectNameException
   {
      super(new ThreadPoolLoadMetricSource(server), ThreadPoolLoadMetricSource.DEFAULT_PATTERN, server);
   }
}
