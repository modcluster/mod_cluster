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

package org.jboss.modcluster.ha;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.catalina.Server;
import org.apache.catalina.util.StringManager;
import org.jboss.ha.framework.interfaces.HAServiceKeyProvider;
import org.jboss.ha.framework.interfaces.HASingletonMBean;
import org.jboss.logging.Logger;
import org.jboss.modcluster.Constants;
import org.jboss.modcluster.Utils;
import org.jboss.modcluster.config.BalancerConfiguration;
import org.jboss.modcluster.config.NodeConfiguration;
import org.jboss.modcluster.ha.rpc.ResetRequestGroupRpcResponse;
import org.jboss.modcluster.ha.rpc.ResetRequestSourceRpcHandler;
import org.jboss.modcluster.ha.rpc.ThrowableGroupRpcResponse;
import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.ResetRequestSource;
import org.jboss.modcluster.mcmp.impl.ResetRequestSourceImpl;

/**
 * {@link ResetRequestSource} that provides different reset requests
 * depending on whether or not it believes it is running on the singleton
 * master.
 * 
 * @author Brian Stansberry
 */
public class HASingletonAwareResetRequestSourceImpl extends ResetRequestSourceImpl implements HASingletonAwareResetRequestSource
{
   static final String METHOD_NAME = "getResetRequests";
   static final Class<?>[] TYPES = new Class[] { Map.class };
   
   private static final Logger log = Logger.getLogger(HASingletonAwareResetRequestSourceImpl.class);
   
   /**
    * The string manager for this package.
    */
   private final StringManager sm = StringManager.getManager(Constants.Package);
   
   private final HASingletonMBean singleton;
   private final ResetRequestSourceRpcHandler<List<?>> rpcStub;
   private volatile Server jbossWebServer;
   
   public HASingletonAwareResetRequestSourceImpl(NodeConfiguration nodeConfig, BalancerConfiguration balancerConfig, HASingletonMBean singleton, HAServiceKeyProvider serviceKeyProvider)
   {
      super(nodeConfig, balancerConfig);
      this.singleton = singleton;
      this.rpcStub = new RpcStub(serviceKeyProvider);
   }
   
   @Override
   public List<MCMPRequest> getResetRequests(Map<String, Set<VirtualHost>> response)
   {
      if (this.singleton.isMasterNode())
      {
         List<MCMPRequest> resets = this.getLocalResetRequests(response);
         this.addRemoteRequests(resets, response);
         return resets;
      }

      return Collections.emptyList();
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ha.HASingletonAwareResetRequestSource#getLocalResetRequests()
    */
   public List<MCMPRequest> getLocalResetRequests(Map<String, Set<VirtualHost>> response)
   {
      if (this.jbossWebServer == null)
      {
         return new ArrayList<MCMPRequest>();
      }
      
      return super.getResetRequests(response, this.jbossWebServer);
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ha.HASingletonAwareResetRequestSource#setJbossWebServer(org.apache.catalina.Server)
    */
   public void setJbossWebServer(Server jbossWebServer)
   {
      this.jbossWebServer = jbossWebServer;
   }
   
   private void addRemoteRequests(List<MCMPRequest> resets, Map<String, Set<VirtualHost>> resp)
   {
      List<?> responses = this.rpcStub.getResetRequests(resp);
      
      for (Object response : responses)
      {
         if (response instanceof ResetRequestGroupRpcResponse)
         {
            resets.addAll(((ResetRequestGroupRpcResponse) response).getValue());
         }
         else if (response instanceof ThrowableGroupRpcResponse)
         {
            ThrowableGroupRpcResponse tgrr = (ThrowableGroupRpcResponse) response;
            //FIXME what to do?
            log.warn(this.sm.getString("modcluster.error.rpc.known", METHOD_NAME, tgrr.getSender()), tgrr.getValue());
         }
         else if (response instanceof Throwable)
         {
            log.warn(this.sm.getString("modcluster.error.rpc.unknown", METHOD_NAME), (Throwable) response);
         }
         else
         {
            log.error(this.sm.getString("modcluster.error.rpc.unexpected", response, METHOD_NAME));
         }
         
      }
   }

   private static class RpcStub implements ResetRequestSourceRpcHandler<List<?>>
   {
      private final HAServiceKeyProvider serviceKeyProvider;
      
      RpcStub(HAServiceKeyProvider serviceKeyProvider)
      {
         this.serviceKeyProvider = serviceKeyProvider;
      }
      
      /**
       * @see org.jboss.modcluster.ha.rpc.ResetRequestSourceRpcHandler#getResetRequests()
       */
      public List<?> getResetRequests(Map<String, Set<ResetRequestSource.VirtualHost>> response)
      {
         try
         {
            return this.serviceKeyProvider.getHAPartition().callMethodOnCluster(this.serviceKeyProvider.getHAServiceKey(), METHOD_NAME, new Object[] { response }, TYPES, true);
         }
         catch (Exception e)
         {
            //FIXME what to do?
            throw Utils.convertToUnchecked(e);
         }
      }
   }
}
