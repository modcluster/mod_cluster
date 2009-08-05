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

package org.jboss.modcluster.mcmp;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Source for a list of requests that should be sent to an httpd-side 
 * mod_cluster instance when an {@link MCMPHandler} determines that
 * the httpd-side state needs to be reset.
 * 
 * @author Brian Stansberry
 *
 */
public interface ResetRequestSource
{
   enum Status
   {
      ENABLED, DISABLED, STOPPED
   }
   
   interface VirtualHost extends Serializable
   {
      Set<String> getAliases();
      Map<String, Status> getContexts();
   }
   
   /**
    * Gets a list of requests that should be sent to an httpd-side 
    * mod_cluster instance when an {@link MCMPHandler} determines that
    * its state needs to be reset.
    * 
    * @param response a parsed INFO-RSP, expressed as a map of virtual hosts per jvmRoute
    * @return a list of requests. Will not return <code>null</code>.
    */
   List<MCMPRequest> getResetRequests(Map<String, Set<VirtualHost>> response);
}
