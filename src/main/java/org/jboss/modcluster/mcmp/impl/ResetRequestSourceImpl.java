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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.jboss.modcluster.ServerProvider;
import org.jboss.modcluster.Utils;
import org.jboss.modcluster.config.BalancerConfiguration;
import org.jboss.modcluster.config.NodeConfiguration;
import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPRequestFactory;
import org.jboss.modcluster.mcmp.MCMPRequestType;
import org.jboss.modcluster.mcmp.ResetRequestSource;

/**
 * @author Paul Ferraro
 *
 */
public class ResetRequestSourceImpl implements ResetRequestSource
{
   private final NodeConfiguration nodeConfig;
   private final BalancerConfiguration balancerConfig;
   private final ServerProvider<Server> serverProvider;
   private final MCMPRequestFactory requestFactory;
   
   private volatile Map<String, Set<String>> excludedContexts;
   
   public ResetRequestSourceImpl(NodeConfiguration nodeConfig, BalancerConfiguration balancerConfig, ServerProvider<Server> serverProvider, MCMPRequestFactory requestFactory)
   {
      this.nodeConfig = nodeConfig;
      this.balancerConfig = balancerConfig;
      this.serverProvider = serverProvider;
      this.requestFactory = requestFactory;
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.mcmp.ResetRequestSource#init(java.util.Map)
    */
   public void init(Map<String, Set<String>> excludedContexts)
   {
      this.excludedContexts = excludedContexts;
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.mcmp.ResetRequestSource#getResetRequests(java.util.Map)
    */
   public List<MCMPRequest> getResetRequests(Map<String, Set<VirtualHost>> response)
   {
      List<MCMPRequest> requests = new ArrayList<MCMPRequest>();
      
      Server server = this.serverProvider.getServer();
      
      if (server == null) return requests;
      
      List<MCMPRequest> engineRequests = new LinkedList<MCMPRequest>();      
      
      for (Service service: server.findServices())
      {
         Engine engine = (Engine) service.getContainer();
         
         engineRequests.add(this.requestFactory.createConfigRequest(engine, this.nodeConfig, this.balancerConfig));

         Set<ResetRequestSource.VirtualHost> responseHosts = Collections.emptySet();
         String jvmRoute = engine.getJvmRoute();
         
         if (response.containsKey(jvmRoute))
         {
            responseHosts = response.get(jvmRoute);
         }
         
         for (Container child: engine.findChildren())
         {
            Host host = (Host) child;
            String hostName = host.getName();
            Set<String> aliases = Utils.getAliases(host);
            
            VirtualHost responseHost = null;
            
            for (VirtualHost virtualHost: responseHosts)
            {
               if (virtualHost.getAliases().contains(hostName))
               {
                  responseHost = virtualHost;
                  break;
               }
            }
            
            Set<String> responseAliases = Collections.emptySet();
            Map<String, ResetRequestSource.Status> responseContexts = Collections.emptyMap();
            
            if (responseHost != null)
            {
               responseAliases = responseHost.getAliases();
               
               // If the host(or aliases) is missing - force full reset
               if (!aliases.equals(responseAliases))
               {
                  engineRequests.add(0, this.requestFactory.createRemoveRequest(engine));
               }
               else
               {
                  responseContexts = responseHost.getContexts();
               }
            }
            
            Set<String> obsoleteContexts = new HashSet<String>(responseContexts.keySet());
            
            for (Container container: host.findChildren())
            {
               Context context = (Context) container;
               String path = context.getPath();
               
               Set<String> excludedPaths = this.excludedContexts.get(hostName);
               
               if ((excludedPaths != null) && excludedPaths.contains(path))
               {
                  continue;
               }
               
               obsoleteContexts.remove(path);
               
               ResetRequestSource.Status status = responseContexts.get(path);
               
               if (Utils.isContextStarted(context))
               {
                  if (status != ResetRequestSource.Status.ENABLED)
                  {
                     engineRequests.add(this.requestFactory.createEnableRequest(context));
                  }
               }
               else
               {
                  if (status == ResetRequestSource.Status.ENABLED)
                  {
                     engineRequests.add(this.requestFactory.createStopRequest(context));
                  }
               }
            }
            
            if (!obsoleteContexts.isEmpty())
            {
               // If all contexts from response no longer exist - remove all
               if (obsoleteContexts.size() == responseContexts.size())
               {
                  // Send REMOVE-APP * request first
                  engineRequests.add(0, this.requestFactory.createRemoveRequest(engine));
               }
               // otherwise only remove those that no longer exist
               else
               {
                  for (String context: obsoleteContexts)
                  {
                     engineRequests.add(this.requestFactory.createRequest(MCMPRequestType.REMOVE_APP, jvmRoute, responseAliases, context));
                  }
               }
            }
         }
         
         requests.addAll(engineRequests);
         
         engineRequests.clear();
      }
      
      return requests;
   }
}
