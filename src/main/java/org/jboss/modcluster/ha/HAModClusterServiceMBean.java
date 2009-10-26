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
package org.jboss.modcluster.ha;

import java.util.concurrent.TimeUnit;

import org.jboss.ha.framework.interfaces.HASingletonMBean;
import org.jboss.modcluster.ModClusterServiceMBean;

/**
 * StandardMBean interface for {@link HAModClusterService}.
 * 
 * @author Brian Stansberry
 */
public interface HAModClusterServiceMBean extends HASingletonMBean, ModClusterServiceMBean
{   
   /**
    * @return
    */
   int getProcessStatusFrequency();

   /**
    * @param processStatusFrequency
    */
   void setProcessStatusFrequency(int processStatusFrequency);
   
   /**
    * Disables all contexts on each node within the current domain.
    * If the current domain is null, then all nodes are stopped.
    * @return true, if disable was successful on all nodes
    */
   boolean disableDomain();
   
   /**
    * Enables all contexts on each node within the current domain.
    * If the current domain is null, then all nodes are stopped.
    * @return true, if enable was successful on all nodes
    */
   boolean enableDomain();
   
   /**
    * Gracefully stops all contexts on each node within the current domain.
    * If the current domain is null, then all nodes are stopped.
    * @param timeout the number of units of time for which to wait for all sessions to drain on a given node
    * @param unit the unit of time represented by the timeout parameter
    * @return true, if all contexts were stopped within the specified timeout, false otherwise.
    */
   boolean stopDomain(long timeout, TimeUnit unit);
}
