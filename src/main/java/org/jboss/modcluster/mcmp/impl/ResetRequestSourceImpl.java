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
package org.jboss.modcluster.mcmp.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.catalina.Server;
import org.apache.catalina.ServerFactory;
import org.jboss.modcluster.config.BalancerConfiguration;
import org.jboss.modcluster.config.NodeConfiguration;
import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPUtils;
import org.jboss.modcluster.mcmp.ResetRequestSource;

/**
 * @author Paul Ferraro
 *
 */
public class ResetRequestSourceImpl implements ResetRequestSource
{
   private final NodeConfiguration nodeConfig;
   private final BalancerConfiguration balancerConfig;
   
   public ResetRequestSourceImpl(NodeConfiguration nodeConfig, BalancerConfiguration balancerConfig)
   {
      this.nodeConfig = nodeConfig;
      this.balancerConfig = balancerConfig;
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.mcmp.ResetRequestSource#getResetRequests(java.util.Map)
    */
   public List<MCMPRequest> getResetRequests(Map<String, Set<VirtualHost>> response)
   {
      return this.getResetRequests(response, ServerFactory.getServer());
   }
   
   protected List<MCMPRequest> getResetRequests(Map<String, Set<VirtualHost>> response, Server server)
   {
      return MCMPUtils.getResetRequests(response, server, this.nodeConfig, this.balancerConfig);
   }
}
