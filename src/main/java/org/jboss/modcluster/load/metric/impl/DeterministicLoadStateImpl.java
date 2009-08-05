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

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.modcluster.load.metric.DeterministicLoadState;

/**
 * Computes incremental load change per second from record of previous load.
 * @author Paul Ferraro
 */
public class DeterministicLoadStateImpl implements DeterministicLoadState
{
   private final AtomicReference<Double> previousLoad = new AtomicReference<Double>(new Double(0));
   private final AtomicLong previousTime = new AtomicLong(System.currentTimeMillis());
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.load.metric.DeterministicLoadState#delta(double)
    */
   public double delta(double currentLoad)
   {
      long currentTime = System.currentTimeMillis();
      long previousTime = this.previousTime.getAndSet(currentTime);
      
      double previousLoad = this.previousLoad.getAndSet(new Double(currentLoad)).doubleValue();
      
      double seconds = (currentTime - previousTime) / 1000d;
      
      // Normalize by time interval (in seconds)
      return (currentLoad - previousLoad) / seconds;
   }
   
   public long getPreviousTime()
   {
      return this.previousTime.get();
   }
}
