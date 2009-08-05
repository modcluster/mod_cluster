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

package org.jboss.modcluster.ha.rpc;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import net.jcip.annotations.Immutable;

import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPServerState;

/**
 * GroupRpcResponse that provides the overall status picture for a
 * ModClusterService instance.
 * 
 * @author Brian Stansberry
 */
@Immutable
public class ModClusterServiceStatus implements Serializable
{
   /** The serialVersionUID */
   private static final long serialVersionUID = -6591593007825931165L;
   
   private final Set<MCMPServerState> states;
   private final List<MCMPServerDiscoveryEvent> unacknowledgedEvents;
   private final List<MCMPRequest> resetRequests;
   private final int loadBalanceFactor;
   
   public ModClusterServiceStatus(int loadBalanceFactor, Set<MCMPServerState> states, List<MCMPServerDiscoveryEvent> unacknowledgedEvents, List<MCMPRequest> resetRequests)
   {
      this.loadBalanceFactor = loadBalanceFactor;
      this.states = states;
      this.unacknowledgedEvents = unacknowledgedEvents;
      this.resetRequests = resetRequests;
   }

   public Set<MCMPServerState> getStates()
   {
      return this.states;
   }

   public List<MCMPServerDiscoveryEvent> getUnacknowledgedEvents()
   {
      return this.unacknowledgedEvents;
   }

   public List<MCMPRequest> getResetRequests()
   {
      return this.resetRequests;
   }

   public int getLoadBalanceFactor()
   {
      return this.loadBalanceFactor;
   }
   
}
