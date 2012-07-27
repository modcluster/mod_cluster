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
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import org.apache.catalina.util.StringManager;
import org.jboss.ha.framework.interfaces.HAServiceKeyProvider;
import org.jboss.ha.framework.interfaces.HASingletonMBean;
import org.jboss.logging.Logger;
import org.jboss.modcluster.Constants;
import org.jboss.modcluster.Utils;
import org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler;
import org.jboss.modcluster.ha.rpc.DefaultRpcResponse;
import org.jboss.modcluster.ha.rpc.RpcResponseFilter;
import org.jboss.modcluster.ha.rpc.MCMPServerDiscoveryEvent;
import org.jboss.modcluster.ha.rpc.PeerMCMPDiscoveryStatus;
import org.jboss.modcluster.ha.rpc.RpcResponse;
import org.jboss.modcluster.mcmp.AbstractMCMPHandler;
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

   private enum ResetState
   {
      NONE, REQUIRED, PENDING
   }   
   
   private AtomicReference<ResetState> resetState = new AtomicReference<ResetState>(ResetState.NONE);
   
   @GuardedBy("pendingDiscoveryEvents")
   private List<MCMPServerDiscoveryEvent> pendingDiscoveryEvents = new LinkedList<MCMPServerDiscoveryEvent>();
   
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
    * @see org.jboss.modcluster.ha.ClusteredMCMPHandler#getPendingDiscoveryEvents()
    */
   public List<MCMPServerDiscoveryEvent> getPendingDiscoveryEvents()
   {
      synchronized (this.pendingDiscoveryEvents)
      {
         return new ArrayList<MCMPServerDiscoveryEvent>(this.pendingDiscoveryEvents);
      }
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ha.ClusteredMCMPHandler#discoveryEventsReceived(org.jboss.modcluster.ha.rpc.MCMPServerDiscoveryEvent)
    */
   public void discoveryEventsReceived(PeerMCMPDiscoveryStatus status)
   {
      MCMPServerDiscoveryEvent latestEvent = status.getLatestDiscoveryEvent();
      
      if (latestEvent != null)
      {
         synchronized (this.pendingDiscoveryEvents)
         {
            Iterator<MCMPServerDiscoveryEvent> events = this.pendingDiscoveryEvents.iterator();
            
            while (events.hasNext() && (latestEvent.compareTo(events.next()) >= 0))
            {
               events.remove();
            }
         }
      }
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ha.ClusteredMCMPHandler#updateServersFromMasterNode(java.util.Set)
    */
   public Set<MCMPServerState> updateServersFromMasterNode(Set<MCMPServer> masterList)
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
    * @see org.jboss.modcluster.ha.ClusteredMCMPHandler#isResetNecessary()
    */
   public boolean isResetNecessary()
   {
      return this.resetState.get() == ResetState.REQUIRED;
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ha.ClusteredMCMPHandler#resetInitiated()
    */
   public void resetInitiated()
   {
      this.resetState.set(ResetState.PENDING);
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ha.ClusteredMCMPHandler#resetCompleted()
    */
   public void resetCompleted()
   {
      this.resetState.compareAndSet(ResetState.PENDING, ResetState.NONE);
   }
   
   // ------------------------------------------------------------  MCMPHandler

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.mcmp.MCMPHandler#addProxy(java.net.InetAddress, int)
    */
   public void addProxy(InetAddress address, int port)
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
   public void removeProxy(InetAddress address, int port)
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

      return this.rpcStub.getProxyConfiguration().getResult();
   }

   public String getProxyInfo()
   {
      if (this.singleton.isMasterNode())
      {
         return this.localHandler.getProxyInfo();
      }

      return this.rpcStub.getProxyInfo().getResult();
   }

   public void init(List<InetSocketAddress> initialProxies)
   {
      if (this.singleton.isMasterNode())
      {
         this.localHandler.init(initialProxies);
      }
      else
      {
         this.localHandler.init(new ArrayList<InetSocketAddress>());
         
         if (initialProxies != null)
         {
            for (InetSocketAddress proxy : initialProxies)
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

      return this.rpcStub.isProxyHealthOK().getResult().booleanValue();
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
         this.rpcStub.markProxiesInError().getResult();
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
         this.rpcStub.reset().getResult();
      }
   }

   public Map<MCMPServerState, String> sendRequest(MCMPRequest request)
   {
      if (this.singleton.isMasterNode())
      {
         return this.localHandler.sendRequest(request);
      }

      try
      {
         return this.rpcStub.sendRequest(request).getResult();
      }
      catch (RuntimeException e)
      {
         this.recordRequestFailure();
         
         log.warn(e.getMessage(), e);
         
         return null;
      }
   }

   public Map<MCMPServerState, List<String>> sendRequests(List<MCMPRequest> requests)
   {
      if (this.singleton.isMasterNode())
      {
         return this.localHandler.sendRequests(requests);
      }

      try
      {
         return this.rpcStub.sendRequests(requests).getResult();
      }
      catch (RuntimeException e)
      {
         this.recordRequestFailure();
         
         log.warn(e.getMessage(), e);
         
         return null;
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
   
   private void sendDiscoveryEventToPartition(InetAddress address, int port, boolean addition)
   {
      InetSocketAddress socketAddress = new InetSocketAddress(address, port);
      
      synchronized (this.pendingDiscoveryEvents)
      {
         // Ensure discovery event enters queue sequentially by index
         MCMPServerDiscoveryEvent event = new MCMPServerDiscoveryEvent(this.serviceKeyProvider.getHAPartition().getClusterNode(), socketAddress, addition, this.discoveryEventIndex.incrementAndGet());
         
         this.pendingDiscoveryEvents.add(event);
         
         try
         {
            this.rpcStub.mcmpServerDiscoveryEvent(event).getResult();
         }
         catch (RuntimeException e)
         {
            // Just log it; we'll retry later
            String msg = addition ? "modcluster.error.discovery.add" : "modcluster.error.discovery.remove";
            log.error(this.sm.getString(msg, address, Integer.valueOf(port)), e);
         }
      }
   }
   
   void recordRequestFailure()
   {
      this.resetState.set(ResetState.REQUIRED);
   }
   
   class RpcStub implements ClusteredMCMPHandlerRpcHandler
   {
      /**
       * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#getProxyConfiguration()
       */
      public RpcResponse<String> getProxyConfiguration()
      {
         return this.invokeRpc("getProxyConfiguration");
      }

      /**
       * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#getProxyInfo()
       */
      public RpcResponse<String> getProxyInfo()
      {
         return this.invokeRpc("getProxyInfo");
      }

      /**
       * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#isProxyHealthOK()
       */
      public RpcResponse<Boolean> isProxyHealthOK()
      {
         return this.invokeRpc("isProxyHealthOk");
      }

      /**
       * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#markProxiesInError()
       */
      public RpcResponse<Void> markProxiesInError()
      {
         return this.invokeRpc("markProxiesInError");
      }

      /**
       * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#mcmpServerDiscoveryEvent(org.jboss.modcluster.ha.rpc.MCMPServerDiscoveryEvent)
       */
      public RpcResponse<Void> mcmpServerDiscoveryEvent(MCMPServerDiscoveryEvent event)
      {
         try
         {
            return this.invokeRpc("mcmpServerDiscoveryEvent", new Object[] { event }, DISC_EVENT_TYPES);
         }
         catch (Exception e)
         {
            DefaultRpcResponse<Void> response = new DefaultRpcResponse<Void>(null);
            response.setException(e);
            return response;
         }
      }

      /**
       * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#reset()
       */
      public RpcResponse<Void> reset()
      {
         return this.invokeRpc("reset");
      }

      /**
       * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#sendRequest(org.jboss.modcluster.mcmp.MCMPRequest)
       */
      public RpcResponse<Map<MCMPServerState, String>> sendRequest(MCMPRequest request)
      {
         return this.invokeRpc("sendRequest", new Object[] { request }, MCMPREQ_TYPES, true);
      }

      /**
       * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#sendRequests(java.util.List)
       */
      public RpcResponse<Map<MCMPServerState, List<String>>> sendRequests(List<MCMPRequest> requests)
      {
         return this.invokeRpc("sendRequests", new Object[] { requests }, MCMPREQS_TYPES, true);
      }
      
      private <T> RpcResponse<T> invokeRpc(String methodName)
      {
         return this.invokeRpc(methodName, NULL_ARGS, NULL_TYPES, false);
      }
      
      private <T> RpcResponse<T> invokeRpc(String methodName, Object[] args, Class<?>[] types, boolean recordFailure)
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
      
      @SuppressWarnings("unchecked")
      private <T> RpcResponse<T> invokeRpc(String methodName, Object[] args, Class<?>[] types) throws Exception
      {
         List<?> responses = ClusteredMCMPHandlerImpl.this.serviceKeyProvider.getHAPartition().callMethodOnCluster(ClusteredMCMPHandlerImpl.this.serviceKeyProvider.getHAServiceKey(), methodName, args, types, false, new RpcResponseFilter());
         
         Throwable thrown = null;
         
         for (Object obj : responses)
         {
            if (obj instanceof RpcResponse)
            {
               return (RpcResponse) obj;
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
