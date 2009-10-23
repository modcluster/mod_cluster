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

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.jboss.ha.framework.interfaces.ClusterNode;
import org.jboss.modcluster.mcmp.MCMPServerState;

/**
 * Represents the status of a given MCMP client's ability to communicate
 * with MCMP servers.
 * 
 * @author Brian Stansberry
 */
public class ModClusterServiceDRMEntry implements Serializable, Comparable<ModClusterServiceDRMEntry>
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 8275232749243297786L;
   
   private final ClusterNode peer;
   private volatile Set<MCMPServerState> mcmpServerStates;
   private final Integer healthyEstablishedCount;
   private final Integer establishedCount;
   private final Integer healthyCount;
   private final Integer knownCount;
   private final Set<String> jvmRoutes;

   public ModClusterServiceDRMEntry(ClusterNode peer, Set<MCMPServerState> mcmpServerStates)
   {
      this(peer, mcmpServerStates, new HashSet<String>());
   }

   public ModClusterServiceDRMEntry(ClusterNode peer, Set<MCMPServerState> mcmpServerStates, Set<String> jvmRoutes)
   {
      assert peer != null : "peer is null";
      assert jvmRoutes != null : "jvmRoutes is null";
      
      this.peer = peer;
      this.mcmpServerStates = mcmpServerStates;
      
      int healthyEstablished = 0;
      int knownEstablished = 0;
      int healthy = 0;
      int known = 0;
      
      if (this.mcmpServerStates != null)
      {
         for (MCMPServerState state : this.mcmpServerStates)
         {
            known++;
            if (state.getState() == MCMPServerState.State.OK)
            {
               healthy++;
               if (state.isEstablished())
               {
                  knownEstablished++;
                  healthyEstablished++;
               }
            }
            else if (state.isEstablished())
            {
               knownEstablished++;
            }
         }
      }
      
      this.establishedCount = Integer.valueOf(knownEstablished);
      this.healthyCount = Integer.valueOf(healthy);
      this.healthyEstablishedCount = Integer.valueOf(healthyEstablished);
      this.knownCount = Integer.valueOf(known);
      this.jvmRoutes = jvmRoutes;
   }

   public ClusterNode getPeer()
   {
      return this.peer;
   }
   
   public Set<MCMPServerState> getMCMPServerStates()
   {
      return this.mcmpServerStates;
   }
   
   public Set<String> getJvmRoutes()
   {
      synchronized (this.jvmRoutes)
      {
         return new HashSet<String>(this.jvmRoutes);
      }
   }
   
   public void addJvmRoute(String jvmRoute)
   {
      synchronized (this.jvmRoutes)
      {
         this.jvmRoutes.add(jvmRoute);
      }
   }
   
   public void removeJvmRoute(String jvmRoute)
   {
      synchronized (this.jvmRoutes)
      {
         this.jvmRoutes.remove(jvmRoute);
      }
   }

   public int compareTo(ModClusterServiceDRMEntry other)
   {
      int result = other.healthyEstablishedCount.compareTo(this.healthyEstablishedCount);
      if (result == 0)
      {
         result = other.establishedCount.compareTo(this.establishedCount);
         if (result == 0)
         {
            result = other.healthyCount.compareTo(this.healthyCount);
            if (result == 0)
            {
               result = other.knownCount.compareTo(this.knownCount);
            }
         }
      }
      
      return result;
   }

   @Override
   public boolean equals(Object object)
   {
      if (object == this) return true;
      if ((object == null) || !(object instanceof ModClusterServiceDRMEntry)) return false;
      
      ModClusterServiceDRMEntry entry = (ModClusterServiceDRMEntry) object;
      
      return this.peer.equals(entry.peer);
   }

   @Override
   public int hashCode()
   {
      return this.peer.hashCode();
   }
   
   @Override
   public String toString()
   {
      StringBuilder builder = new StringBuilder(this.getClass().getName());
      builder.append("{peer=").append(this.peer);
      if (this.mcmpServerStates != null)
      {
         builder.append(",states=").append(this.mcmpServerStates);
      }
      return builder.append("}").toString();
   }
}