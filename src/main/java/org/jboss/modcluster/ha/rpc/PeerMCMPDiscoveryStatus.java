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

import java.util.Set;

import org.jboss.ha.framework.interfaces.ClusterNode;
import org.jboss.modcluster.mcmp.MCMPServerState;
import org.jboss.modcluster.ha.ModClusterServiceDRMEntry;

/**
 * Extends {@link ModClusterServiceDRMEntry} to include information on
 * the most recent discovery event from the peer that the current
 * singleton master has included in the group-wide set of MCMP configurations.
 * 
 * @author Brian Stansberry
 */
public class PeerMCMPDiscoveryStatus extends ModClusterServiceDRMEntry
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 3497115763128334162L;
   
   private final MCMPServerDiscoveryEvent latestDiscoveryEvent;
   
   /**
    * Create a new PeerMCMPCommStatus.
    * 
    * @param peer the id of the peer
    * @param mcmpServerStates unmodifiable Set of MCMPServerState objects, or
    *                         <code>null</code> if such a set of states could
    *                         not be obtained for the peer.
    * @param latestDiscoveryEvent most recent discovery event received from the peer
    */
   public PeerMCMPDiscoveryStatus(ClusterNode peer, Set<MCMPServerState> mcmpServerStates, 
                             MCMPServerDiscoveryEvent latestDiscoveryEvent)
   {
      super(peer, mcmpServerStates);
      this.latestDiscoveryEvent = latestDiscoveryEvent;
   }

   public MCMPServerDiscoveryEvent getLatestDiscoveryEvent()
   {
      return this.latestDiscoveryEvent;
   }
   
}
