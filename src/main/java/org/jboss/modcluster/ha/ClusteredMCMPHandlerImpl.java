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

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import org.apache.catalina.util.StringManager;
import org.jboss.ha.framework.interfaces.HAServiceKeyProvider;
import org.jboss.ha.framework.interfaces.HASingletonMBean;
import org.jboss.logging.Logger;
import org.jboss.modcluster.Constants;
import org.jboss.modcluster.Utils;
import org.jboss.modcluster.ha.rpc.BooleanGroupRpcResponse;
import org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler;
import org.jboss.modcluster.ha.rpc.GroupRpcResponse;
import org.jboss.modcluster.ha.rpc.GroupRpcResponseFilter;
import org.jboss.modcluster.ha.rpc.MCMPServerDiscoveryEvent;
import org.jboss.modcluster.ha.rpc.StringGroupRpcResponse;
import org.jboss.modcluster.ha.rpc.ThrowableGroupRpcResponse;
import org.jboss.modcluster.mcmp.AbstractMCMPHandler;
import org.jboss.modcluster.mcmp.AddressPort;
import org.jboss.modcluster.mcmp.MCMPHandler;
import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPServer;
import org.jboss.modcluster.mcmp.MCMPServerState;

@ThreadSafe
public class ClusteredMCMPHandlerImpl extends AbstractMCMPHandler implements ClusteredMCMPHandler
{
   static final Object[] NULL_ARGS = new Object[0];
   static final Class<?>[] NULL_TYPES = new Class[0];
   static final Class<?>[] MCMPREQ_TYPES = new Class[] { MCMPRequest.class };
   static final Class<?>[] MCMPREQS_TYPES = new Class[] { List.class };
   static final Class<?>[] DISC_EVENT_TYPES = new Class[] { MCMPServerDiscoveryEvent.class };
   
   static final Logger log = Logger.getLogger(ClusteredMCMPHandlerImpl.class);
   
   final HAServiceKeyProvider serviceKeyProvider;
   private final MCMPHandler localHandler;
   private final HASingletonMBean singleton;
   private final ClusteredMCMPHandlerRpcHandler rpcStub = new RpcStub();
   
   private volatile String haServiceName;
   
   @GuardedBy("errorState")
   private final List<Boolean> errorState = new ArrayList<Boolean>();
   
   @GuardedBy("this")
   private List<MCMPServerDiscoveryEvent> pendingDiscoveryEvents = new ArrayList<MCMPServerDiscoveryEvent>();
   
   private AtomicInteger discoveryEventIndex = new AtomicInteger();

   /**
    * The string manager for this package.
    */
   final StringManager sm = StringManager.getManager(Constants.Package);
   
   public ClusteredMCMPHandlerImpl(MCMPHandler localHandler, HASingletonMBean singleton, HAServiceKeyProvider serviceKeyProvider)
   {
      this.localHandler = localHandler;
      this.singleton = singleton;
      this.serviceKeyProvider = serviceKeyProvider;
   }
   
   // ---------------------------------------------------  ClusteredMCMPHandler

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ha.ClusteredMCMPHandler#getHAServiceName()
    */
   public String getHAServiceName()
   {
      return this.haServiceName;
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ha.ClusteredMCMPHandler#setHAServiceName(java.lang.String)
    */
   public void setHAServiceName(String serviceName)
   {
      this.haServiceName = serviceName;
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ha.ClusteredMCMPHandler#getPartitionName()
    */
   public String getPartitionName()
   {
      return this.serviceKeyProvider.getHAPartition().getPartitionName();
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ha.ClusteredMCMPHandler#getPendingDiscoveryEvents()
    */
   public synchronized List<MCMPServerDiscoveryEvent> getPendingDiscoveryEvents()
   {
      return new ArrayList<MCMPServerDiscoveryEvent>(this.pendingDiscoveryEvents);
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ha.ClusteredMCMPHandler#discoveryEventsReceived(org.jboss.modcluster.ha.rpc.MCMPServerDiscoveryEvent)
    */
   public synchronized void discoveryEventsReceived(MCMPServerDiscoveryEvent lastReceived)
   {
      if (lastReceived != null)
      {
         for (Iterator<MCMPServerDiscoveryEvent> it = this.pendingDiscoveryEvents.iterator(); it.hasNext();)
         {
            MCMPServerDiscoveryEvent event = it.next();
            if (event.getEventIndex() <= lastReceived.getEventIndex())
            {
               it.remove();
            }
            else
            {
               return;
            }
         }
      }
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ha.ClusteredMCMPHandler#updateServersFromMasterNode(java.util.Set)
    */
   public synchronized Set<MCMPServerState> updateServersFromMasterNode(Set<MCMPServer> masterList)
   {
      for (MCMPServer server : masterList)
      {
         this.localHandler.addProxy(server.getAddress(), server.getPort(), server.isEstablished());
      }
      
      for (MCMPServer server : this.localHandler.getProxyStates())
      {
         if (!masterList.contains(server))
         {
            this.localHandler.removeProxy(server.getAddress(), server.getPort());
         }
      }
      
      this.localHandler.status();
      
      return this.localHandler.getProxyStates();
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ha.ClusteredMCMPHandler#getNeedsResetTransmission()
    */
   public boolean getNeedsResetTransmission()
   {
      synchronized (this.errorState)
      {
         return this.errorState.size() > 0 && (this.errorState.get(this.errorState.size() - 1).booleanValue() == false);
      }
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ha.ClusteredMCMPHandler#recordResetTransmission()
    */
   public void recordResetTransmission()
   {
      synchronized (this.errorState)
      {
         if (this.errorState.size() > 0)
         {
            this.errorState.set(0, Boolean.TRUE);
         }
      }
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ha.ClusteredMCMPHandler#recordResetSuccess()
    */
   public void recordResetSuccess()
   {
      synchronized (this.errorState)
      {
         if (this.errorState.size() > 0 && this.errorState.get(this.errorState.size() - 1).booleanValue())
         {
            this.errorState.remove(0);
         }
      }
   }
   
   // ------------------------------------------------------------  MCMPHandler

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.mcmp.MCMPHandler#addProxy(java.net.InetAddress, int)
    */
   public synchronized void addProxy(InetAddress address, int port)
   {
      if (this.singleton.isMasterNode())
      {
         this.localHandler.addProxy(address, port);
      }
      else
      {
         this.sendDiscoveryEventToPartition(address, port, true);
      }
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.mcmp.MCMPHandler#addProxy(java.net.InetAddress, int, boolean)
    */
   public void addProxy(InetAddress address, int port, boolean established)
   {
      this.localHandler.addProxy(address, port, established);
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.mcmp.MCMPHandler#removeProxy(java.net.InetAddress, int)
    */
   public synchronized void removeProxy(InetAddress address, int port)
   {
      if (this.singleton.isMasterNode())
      {
         this.localHandler.removeProxy(address, port);
      }
      else
      {
         this.sendDiscoveryEventToPartition(address, port, false);
      }
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.mcmp.MCMPHandler#getProxyStates()
    */
   public Set<MCMPServerState> getProxyStates()
   {
      return this.localHandler.getProxyStates();
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.mcmp.MCMPHandler#getLocalAddress()
    */
   public InetAddress getLocalAddress() throws IOException
   {
      return this.localHandler.getLocalAddress();
   }

   public String getProxyConfiguration()
   {
      if (this.singleton.isMasterNode())
      {
         return this.localHandler.getProxyConfiguration();
      }

      GroupRpcResponse response = this.rpcStub.getProxyConfiguration();
      
      this.validateResponse(response, false);
      
      return ((StringGroupRpcResponse) response).getValue();
   }

   public String getProxyInfo()
   {
      if (this.singleton.isMasterNode())
      {
         return this.localHandler.getProxyInfo();
      }

      GroupRpcResponse response = this.rpcStub.getProxyInfo();
      
      this.validateResponse(response, false);
      
      return ((StringGroupRpcResponse) response).getValue();
   }

   public void init(List<AddressPort> initialProxies)
   {
      if (this.singleton.isMasterNode())
      {
         this.localHandler.init(initialProxies);
      }
      else
      {
         this.localHandler.init(new ArrayList<AddressPort>());
         
         if (initialProxies != null)
         {
            for (AddressPort proxy : initialProxies)
            {
               this.sendDiscoveryEventToPartition(proxy.getAddress(), proxy.getPort(), true);
            }
         }
      }
   }

   public boolean isProxyHealthOK()
   {
      if (this.singleton.isMasterNode())
      {
         return this.localHandler.isProxyHealthOK();
      }

      GroupRpcResponse response = this.rpcStub.isProxyHealthOK();
      
      this.validateResponse(response, false);
      
      return ((BooleanGroupRpcResponse) response).getValue();
   }

   public void markProxiesInError()
   {
      this.recordRequestFailure();
      
      if (this.singleton.isMasterNode())
      {
         this.localHandler.markProxiesInError();
      }
      else
      {
         GroupRpcResponse response = this.rpcStub.markProxiesInError();
         
         this.validateResponse(response, false);
      }
   }

   public void reset()
   {
      if (this.singleton.isMasterNode())
      {
         this.localHandler.reset();
      }
      else
      {
         GroupRpcResponse response = this.rpcStub.reset();
         
         this.validateResponse(response, false);
      }
   }

   public void sendRequest(MCMPRequest request)
   {
      if (this.singleton.isMasterNode())
      {
         this.localHandler.sendRequest(request);
      }
      else
      {
         GroupRpcResponse response = this.rpcStub.sendRequest(request);
         
         this.validateResponse(response, true);
      }
   }

   public void sendRequests(List<MCMPRequest> requests)
   {
      if (this.singleton.isMasterNode())
      {
         this.localHandler.sendRequests(requests);
      }
      else
      {
         GroupRpcResponse response = this.rpcStub.sendRequests(requests);
         
         this.validateResponse(response, true);
      }
   }
   
   public void shutdown()
   {
      this.localHandler.shutdown();
   }

   public void status()
   {
      log.warn(this.sm.getString("modcluster.error.status.unsupported"));
   }
   
   // ----------------------------------------------------------------  Private
   
   private void validateResponse(GroupRpcResponse response, boolean recordFailure)
   {
      if (response instanceof ThrowableGroupRpcResponse)
      {
         if (recordFailure)
         {
            this.recordRequestFailure();
         }
         
         throw ((ThrowableGroupRpcResponse) response).getValueAsRuntimeException();
      }
   }
   
   private synchronized void sendDiscoveryEventToPartition(InetAddress address, int port, boolean addition)
   {
      AddressPort ap = new AddressPort(address, port);
      MCMPServerDiscoveryEvent event = new MCMPServerDiscoveryEvent(this.serviceKeyProvider.getHAPartition().getClusterNode(), ap, addition, this.discoveryEventIndex.incrementAndGet());
      this.pendingDiscoveryEvents.add(event);
      
      GroupRpcResponse response = this.rpcStub.mcmpServerDiscoveryEvent(event);
      
      if (response instanceof ThrowableGroupRpcResponse)
      {
         // Just log it; we'll retry later
         String msg = addition ? "modcluster.error.discovery.add" : "modcluster.error.discovery.remove";
         log.error(this.sm.getString(msg, address, Integer.valueOf(port)), ((ThrowableGroupRpcResponse) response).getValue());
      }
   }
   
   void recordRequestFailure()
   {
      synchronized (this.errorState)
      {
         if (this.errorState.size() == 0 || this.errorState.get(this.errorState.size() - 1).booleanValue())
         {
            this.errorState.add(Boolean.FALSE);
         }
      }
   }
   
   class RpcStub implements ClusteredMCMPHandlerRpcHandler
   {
      /**
       * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#getProxyConfiguration()
       */
      public GroupRpcResponse getProxyConfiguration()
      {
         return this.invokeRpc("getProxyConfiguration");
      }

      /**
       * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#getProxyInfo()
       */
      public GroupRpcResponse getProxyInfo()
      {
         return this.invokeRpc("getProxyInfo");
      }

      /**
       * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#isProxyHealthOK()
       */
      public GroupRpcResponse isProxyHealthOK()
      {
         return this.invokeRpc("isProxyHealthOk");
      }

      /**
       * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#markProxiesInError()
       */
      public GroupRpcResponse markProxiesInError()
      {
         return this.invokeRpc("markProxiesInError");
      }

      /**
       * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#mcmpServerDiscoveryEvent(org.jboss.modcluster.ha.rpc.MCMPServerDiscoveryEvent)
       */
      public GroupRpcResponse mcmpServerDiscoveryEvent(MCMPServerDiscoveryEvent event)
      {
         try
         {
            return this.invokeRpc("mcmpServerDiscoveryEvent", new Object[] { event }, DISC_EVENT_TYPES);
         }
         catch (Exception e)
         {
            return new ThrowableGroupRpcResponse(null, e);
         }
      }

      /**
       * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#reset()
       */
      public GroupRpcResponse reset()
      {
         return this.invokeRpc("reset");
      }

      /**
       * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#sendRequest(org.jboss.modcluster.mcmp.MCMPRequest)
       */
      public GroupRpcResponse sendRequest(MCMPRequest request)
      {
         return this.invokeRpc("sendRequest", new Object[] { request }, MCMPREQ_TYPES, true);
      }

      /**
       * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#sendRequests(java.util.List)
       */
      public GroupRpcResponse sendRequests(List<MCMPRequest> requests)
      {
         return this.invokeRpc("sendRequests", new Object[] { requests }, MCMPREQS_TYPES, true);
      }
      
      private GroupRpcResponse invokeRpc(String methodName)
      {
         return this.invokeRpc(methodName, NULL_ARGS, NULL_TYPES, false);
      }
      
      private GroupRpcResponse invokeRpc(String methodName, Object[] args, Class<?>[] types, boolean recordFailure)
      {
         try
         {
            return this.invokeRpc(methodName, args, types);
         }
         catch (Exception e)
         {
            if (recordFailure)
            {
               ClusteredMCMPHandlerImpl.this.recordRequestFailure();
            }
            
            throw Utils.convertToUnchecked(e);
         }
      }
      
      private GroupRpcResponse invokeRpc(String methodName, Object[] args, Class<?>[] types) throws Exception
      {
         List<?> responses = ClusteredMCMPHandlerImpl.this.serviceKeyProvider.getHAPartition().callMethodOnCluster(ClusteredMCMPHandlerImpl.this.serviceKeyProvider.getHAServiceKey(), methodName, args, types, false, new GroupRpcResponseFilter());
         
         Throwable thrown = null;
         
         for (Object obj : responses)
         {
            if (obj instanceof GroupRpcResponse)
            {
               return (GroupRpcResponse) obj;
            }
            else if (obj instanceof Throwable)
            {
               if (thrown == null)
               {
                  thrown = (Throwable) obj;
               }
            }
            else
            {
               log.warn(ClusteredMCMPHandlerImpl.this.sm.getString("modcluster.error.rpc.unexpected", obj, methodName));
            }
         }
         
         if (thrown != null)
         {
            throw Utils.convertToUnchecked(thrown);
         }
            
         throw new IllegalStateException(ClusteredMCMPHandlerImpl.this.sm.getString("modcluster.error.rpc.noresp", methodName));
      }
   }
}
