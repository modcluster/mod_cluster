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

package org.jboss.modcluster.config;


public interface NodeConfiguration
{
   /**
    * Domain parameter, which allows tying a jvmRoute to a particular domain.
    */
   String getDomain();

   /**
    * Allows controlling flushing of packets.
    */
   boolean getFlushPackets();

   /**
    * Time to wait before flushing packets.
    */
   int getFlushWait();

   /**
    * Time to wait for a pong answer to a ping.
    */
   int getPing();

   /**
    * Soft maximum inactive connection count.
    */
   int getSmax();

   /**
    * Maximum time on seconds for idle connections above smax.
    */
   int getTtl();

   /**
    * Maximum time on seconds for idle connections the proxy will wait to connect to the node.
    */
   int getNodeTimeout();

   /**
    * Name of the balancer.
    */
   String getBalancer();
}