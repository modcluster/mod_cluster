/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.modcluster.mcmp.impl;

import java.util.Collections;
import java.util.Map;

import net.jcip.annotations.Immutable;

import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPRequestType;

/**
 * Encapsulates the parameters for a request over MCMP.
 * 
 * @author Brian Stansberry
 * @author Paul Ferraro
 */
@Immutable
public class DefaultMCMPRequest implements MCMPRequest
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 7107364666635260031L;
   
   private final MCMPRequestType requestType;
   private final boolean wildcard;
   private final Map<String, String> parameters;
   private final String jvmRoute;
   
   /**
    * Create a new ModClusterRequest. 
    */
   public DefaultMCMPRequest(MCMPRequestType requestType, boolean wildcard, String jvmRoute, Map<String, String> parameters)
   {
      this.requestType = requestType;
      this.wildcard = wildcard;
      this.jvmRoute = jvmRoute;
      this.parameters = Collections.unmodifiableMap(parameters);
   }

   public MCMPRequestType getRequestType()
   {
      return this.requestType;
   }

   public boolean isWildcard()
   {
      return this.wildcard;
   }

   public String getJvmRoute()
   {
      return this.jvmRoute;
   }
   
   public Map<String, String> getParameters()
   {
      return this.parameters;
   }
   
   @Override
   public String toString()
   {
      StringBuilder sb = new StringBuilder(getClass().getName());
      sb.append("{requestType=").append(this.requestType);
      sb.append(",wildcard=").append(this.wildcard);
      sb.append(",jvmRoute=").append(this.jvmRoute);
      sb.append(",parameters=").append(this.parameters);
      sb.append("}");
      
      return sb.toString();
   }
}
