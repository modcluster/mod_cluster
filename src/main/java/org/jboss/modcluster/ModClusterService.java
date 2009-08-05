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

package org.jboss.modcluster;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Server;
import org.apache.catalina.util.StringManager;
import org.jboss.beans.metadata.api.annotations.Inject;
import org.jboss.beans.metadata.api.model.FromContext;
import org.jboss.ha.framework.interfaces.ClusterNode;
import org.jboss.ha.framework.interfaces.DistributedReplicantManager;
import org.jboss.ha.framework.interfaces.HAPartition;
import org.jboss.ha.framework.interfaces.HASingletonElectionPolicy;
import org.jboss.ha.framework.server.HAServiceEvent;
import org.jboss.ha.framework.server.HAServiceEventFactory;
import org.jboss.ha.framework.server.HAServiceRpcHandler;
import org.jboss.ha.framework.server.HASingletonImpl;
import org.jboss.modcluster.config.BalancerConfiguration;
import org.jboss.modcluster.config.MCMPHandlerConfiguration;
import org.jboss.modcluster.config.ModClusterConfig;
import org.jboss.modcluster.config.NodeConfiguration;
import org.jboss.modcluster.ha.ClusteredMCMPHandler;
import org.jboss.modcluster.ha.ClusteredMCMPHandlerImpl;
import org.jboss.modcluster.ha.HASingletonAwareResetRequestSource;
import org.jboss.modcluster.ha.HASingletonAwareResetRequestSourceImpl;
import org.jboss.modcluster.ha.ModClusterServiceDRMEntry;
import org.jboss.modcluster.ha.rpc.BooleanGroupRpcResponse;
import org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler;
import org.jboss.modcluster.ha.rpc.GroupRpcResponse;
import org.jboss.modcluster.ha.rpc.MCMPServerDiscoveryEvent;
import org.jboss.modcluster.ha.rpc.ModClusterServiceRpcHandler;
import org.jboss.modcluster.ha.rpc.ModClusterServiceStateGroupRpcResponse;
import org.jboss.modcluster.ha.rpc.PeerMCMPDiscoveryStatus;
import org.jboss.modcluster.ha.rpc.ResetRequestGroupRpcResponse;
import org.jboss.modcluster.ha.rpc.ResetRequestSourceRpcHandler;
import org.jboss.modcluster.ha.rpc.StringGroupRpcResponse;
import org.jboss.modcluster.ha.rpc.ThrowableGroupRpcResponse;
import org.jboss.modcluster.load.LoadBalanceFactorProvider;
import org.jboss.modcluster.mcmp.AddressPort;
import org.jboss.modcluster.mcmp.MCMPHandler;
import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPServer;
import org.jboss.modcluster.mcmp.MCMPServerState;
import org.jboss.modcluster.mcmp.MCMPUtils;
import org.jboss.modcluster.mcmp.ResetRequestSource;
import org.jboss.modcluster.mcmp.impl.DefaultMCMPHandler;

/**
 * A ModClusterService.
 * 
 * @author Brian Stansberry
 * @version $Revision$
 */
public class ModClusterService extends HASingletonImpl<HAServiceEvent>
   implements JBossWebEventHandler, ModClusterServiceMBean, ModClusterServiceRpcHandler<List<?>, MCMPServerState>
{
   private static final Class<?>[] CLUSTER_STATUS_COMPLETE_TYPES = new Class[] { Map.class };
   private static final Class<?>[] GET_CLUSTER_COORDINATOR_STATE_TYPES = new Class[] { Set.class };
   
   // -----------------------------------------------------------------  Fields
   
   final MCMPHandler localHandler;
   final ClusteredMCMPHandler clusteredHandler;
   final HASingletonAwareResetRequestSource resetRequestSource;
   final Map<ClusterNode, MCMPServerDiscoveryEvent> proxyChangeDigest =
      new HashMap<ClusterNode, MCMPServerDiscoveryEvent>();
   final ModClusterServiceDRMEntry drmEntry;
   
   /**
    * The string manager for this package.
    */
   final StringManager sm = StringManager.getManager(Constants.Package);
   
   private final RpcHandler rpcHandler;
   private final JBossWebEventHandler eventHandlerDelegate;
   private final String domain;
   private final boolean masterPerDomain;
   
   volatile int latestLoad;
   volatile int statusCount = 0;
   volatile int processStatusFrequency = 1;
   
   /**
    * Create a new ClusterCoordinator.
    * 
    * @param partition   the partition of which we are a member
    * @param config      our configuration
    * @param loadFactorProvider source for local load balance statistics
    */
   public ModClusterService(HAPartition partition,
                            ModClusterConfig config,
                            LoadBalanceFactorProvider loadFactorProvider)
   {
      this(partition, config, loadFactorProvider, null);
   }
   
   
   /**
    * Create a new ClusterCoordinator.
    * 
    * @param partition   the partition of which we are a member
    * @param config      our configuration
    * @param loadFactorProvider source for local load balance statistics
    * @param singletonElector chooses the singleton master
    */
   public ModClusterService(HAPartition partition,
                            ModClusterConfig config,
                            LoadBalanceFactorProvider loadFactorProvider,
                            HASingletonElectionPolicy electionPolicy)
   {
      super(new HAServiceEventFactory());
      
      assert partition != null          : this.sm.getString("modcluster.error.iae.null", "partition");
      assert loadFactorProvider != null : this.sm.getString("modcluster.error.iae.null", "loadFactorProvider");
      assert config != null             : this.sm.getString("modcluster.error.iae.null", "config is null");
      
      this.setHAPartition(partition);
      
      this.resetRequestSource = new HASingletonAwareResetRequestSourceImpl(config, config, this, this);
      this.localHandler = new DefaultMCMPHandler(config, this.resetRequestSource);
      this.clusteredHandler = new ClusteredMCMPHandlerImpl(this.localHandler, this, this);
      this.eventHandlerDelegate = new ClusteredJBossWebEventHandler(config, config, config, this.clusteredHandler, loadFactorProvider);
      this.domain = config.getDomain();
      this.masterPerDomain = config.isMasterPerDomain();
      
      this.setElectionPolicy(electionPolicy);
      
      this.drmEntry = new ModClusterServiceDRMEntry(partition.getClusterNode(), null);
      
      this.rpcHandler = new RpcHandler();
   }
   
   /**
    * Create a new ClusterCoordinator using the given component parts.
    * Only intended for use by test suites that may wish to inject
    * mock components.
    * 
    * @param partition
    * @param nodeConfig
    * @param balancerConfig
    * @param localHandler
    * @param resetRequestSource
    * @param clusteredHandler
    * @param loadManager
    * @param singletonElector
    */
   protected ModClusterService(HAPartition partition,
                               NodeConfiguration nodeConfig,
                               BalancerConfiguration balancerConfig,
                               MCMPHandlerConfiguration mcmpConfig,
                               MCMPHandler localHandler,
                               HASingletonAwareResetRequestSource resetRequestSource,
                               ClusteredMCMPHandler clusteredHandler,
                               LoadBalanceFactorProvider loadFactorProvider,
                               HASingletonElectionPolicy electionPolicy)
   {
      super(new HAServiceEventFactory());
      
      assert partition != null          : this.sm.getString("modcluster.error.iae.null", "partition");
      assert localHandler != null       : this.sm.getString("modcluster.error.iae.null", "localHandler");
      assert resetRequestSource != null : this.sm.getString("modcluster.error.iae.null", "resetRequestSource");
      assert nodeConfig != null         : this.sm.getString("modcluster.error.iae.null", "nodeConfig");
      assert balancerConfig != null     : this.sm.getString("modcluster.error.iae.null", "balancerConfig");
      assert clusteredHandler != null   : this.sm.getString("modcluster.error.iae.null", "clusteredHandler");
      
      this.setHAPartition(partition);
      
      this.localHandler = localHandler;
      this.resetRequestSource = resetRequestSource;
      this.clusteredHandler = clusteredHandler;
      this.eventHandlerDelegate = new ClusteredJBossWebEventHandler(nodeConfig, balancerConfig, mcmpConfig, this.clusteredHandler, loadFactorProvider);
      this.domain = nodeConfig.getDomain();
      this.masterPerDomain = mcmpConfig.isMasterPerDomain();
      
      this.setElectionPolicy(electionPolicy);
      
      this.drmEntry = new ModClusterServiceDRMEntry(partition.getClusterNode(), null);
      
      this.rpcHandler = new RpcHandler();
   }
   
   // -------------------------------------------------- ModClusterServiceMBean

   public void addProxy(String host, int port)
   {
      this.clusteredHandler.addProxy(host, port);
   }


   public void removeProxy(String host, int port)
   {
      this.clusteredHandler.removeProxy(host, port);
   }


   public String getProxyInfo()
   {
      return this.clusteredHandler.getProxyInfo();
   }


   public String getProxyConfiguration()
   {
      return this.clusteredHandler.getProxyConfiguration();
   }


   public void refresh()
   {
      this.clusteredHandler.markProxiesInError();
   }


   public void reset()
   {
      this.clusteredHandler.reset();
   }
   
   
   // ---------------------------------------------------- JBossWebEventHandler


   public void init()
   {
      this.eventHandlerDelegate.init();
   }

   public void shutdown()
   {
      this.eventHandlerDelegate.shutdown();
   }
   
   public void startServer(Server server)
   {
      this.eventHandlerDelegate.startServer(server);
   }

   public void stopServer(Server server)
   {
      this.eventHandlerDelegate.stopServer(server);
   }

   public void addContext(Context context)
   {
      this.eventHandlerDelegate.addContext(context);
   }
   
   public void startContext(Context context)
   {
      this.eventHandlerDelegate.startContext(context);
   }
   
   public void stopContext(Context context)
   {
      this.eventHandlerDelegate.stopContext(context);
   }
   
   public void removeContext(Context context)
   {
      this.eventHandlerDelegate.removeContext(context);
   }
   
   public void status(Engine engine)
   {
      this.eventHandlerDelegate.status(engine);
   }
   
   // ------------------------------------------------------------- Properties

   public String getDomain()
   {
      return this.domain;
   }
   
   public int getProcessStatusFrequency()
   {
      return this.processStatusFrequency;
   }

   public void setProcessStatusFrequency(int processStatusFrequency)
   {
      this.processStatusFrequency = processStatusFrequency;
   }

   // -------------------------------------------------------  Public Overrides

   @Override
   public void startSingleton()
   {
      // Ensure we do a full status on the next event
      this.statusCount = this.processStatusFrequency - 1;
   }

   @Override
   @Inject(fromContext = FromContext.NAME)
   public void setServiceHAName(String haName)
   {
      super.setServiceHAName(haName);
   }

   // --------------------------------------------------------------  Protected

   /**
    * {@inheritDoc}
    * 
    * @return an inner class that allows us to avoid exposing RPC methods as
    *         public methods of this class
    */
   @Override
   protected HAServiceRpcHandler<HAServiceEvent> getRpcHandler()
   {
      return this.rpcHandler;
   }

   /**
    * {@inheritDoc}
    * 
    * @returns the key used by DRM and the partition rpc handler mapping.
    */
   @Override
   public String getHAServiceKey()
   {
      String name = this.getServiceHAName();
      
      return ((this.domain != null) && this.masterPerDomain) ? name + ":" + this.domain : name;
   }
   
   /**
    * {@inheritDoc}
    * 
    * @return a {@link ModClusterServiceDRMEntry}
    */
   @Override
   protected Serializable getReplicant()
   {
      return this.drmEntry;
   }

   /**
    * {@inheritDoc}
    * @return a list of cluster nodes from which to elect a new master
    */
   @Override
   @SuppressWarnings("unchecked")
   protected List<ClusterNode> getElectionCandidates()
   {
      List<ModClusterServiceDRMEntry> candidates = this.getHAPartition().getDistributedReplicantManager().lookupReplicants(this.getHAServiceKey());
      
      return this.narrowCandidateList(candidates);
   }
   
   /**
    * Processes the candidate list, discarding those who don't match our domain nor the best
    * candidate when it comes to the ability to communicate with proxies.
    * 
    * @param candidates the universe of possible candidates.
    * @return a list of candidates with an equivalent ability to communicate
    *         with proxies, or <code>null</code> if <code>candidates</code>
    *         is <code>null</code>.
    */
   List<ClusterNode> narrowCandidateList(Collection<ModClusterServiceDRMEntry> candidates)
   {
      if (candidates == null) return null;
      
      List<ClusterNode> narrowed = new ArrayList<ClusterNode>(candidates.size());
      ModClusterServiceDRMEntry champion = null;
      
      for (ModClusterServiceDRMEntry candidate: candidates)
      {
         if (champion == null)
         {
            champion = candidate;
            narrowed.add(candidate.getPeer());
         }
         else
         {
            int compFactor = candidate.compareTo(champion);
            if (compFactor < 0)
            {
               // New champ
               narrowed.clear();
               champion = candidate;
               narrowed.add(candidate.getPeer());
            }
            else if (compFactor == 0)
            {
               // As good as our champ
               narrowed.add(candidate.getPeer());
            }
            // else candidate didn't make the cut; continue
         }
      }
      
      return narrowed;
   }
   
   /**
    * @see org.jboss.modcluster.ha.rpc.ModClusterServiceRpcHandler#clusterStatusComplete(java.util.Map)
    */
   public void clusterStatusComplete(Map<ClusterNode, PeerMCMPDiscoveryStatus> statuses)
   {
      try
      {
         this.callMethodOnPartition("clusterStatusComplete", new Object[] { statuses }, CLUSTER_STATUS_COMPLETE_TYPES);
      }
      catch (Exception e)
      {
         this.log.error(this.sm.getString("modcluster.error.status.complete"), e);
      }
   }

   /**
    * @see org.jboss.modcluster.ha.rpc.ModClusterServiceRpcHandler#getClusterCoordinatorState(java.util.Set)
    */
   public List<?> getClusterCoordinatorState(Set<MCMPServerState> masterList)
   {
      try
      {
         return this.callMethodOnPartition("getClusterCoordinatorState", new Object[] { masterList }, GET_CLUSTER_COORDINATOR_STATE_TYPES);
      }
      catch (Exception e)
      {
         throw Utils.convertToUnchecked(e);
      }
   }
   
   void updateLocalDRM(ModClusterServiceDRMEntry status)
   {
      try
      {
         this.getHAPartition().getDistributedReplicantManager().add(this.getHAServiceKey(), status);
      }
      catch (Exception e)
      {
         throw Utils.convertToUnchecked(e);
      }
   }

   // ---------------------------------------------------------- Inner classes
   
   /**
    * This is the object that gets invoked on via reflection by HAPartition.
    */
   protected class RpcHandler extends HASingletonImpl<HAServiceEvent>.RpcHandler implements ModClusterServiceRpcHandler<GroupRpcResponse, MCMPServer>, ClusteredMCMPHandlerRpcHandler, ResetRequestSourceRpcHandler<GroupRpcResponse>
   {
      private final ModClusterService coord = ModClusterService.this;
      private final GroupRpcResponse SUCCESS = new GroupRpcResponse(this.coord.getHAPartition().getClusterNode());
/*
      public GroupRpcResponse getLocalAddress() throws IOException
      {
         if (!this.coord.isMasterNode()) return null;
         
         return new InetAddressGroupRpcResponse(this.coord.getHAPartition().getClusterNode(), this.coord.localHandler.getLocalAddress());
      }
*/
      /**
       * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#mcmpServerDiscoveryEvent(org.jboss.modcluster.ha.rpc.MCMPServerDiscoveryEvent)
       */
      public GroupRpcResponse mcmpServerDiscoveryEvent(MCMPServerDiscoveryEvent event)
      {
         if (!this.coord.isMasterNode()) return null;
         
         synchronized (ModClusterService.this.proxyChangeDigest)
         {
            AddressPort ap = event.getMCMPServer();
            
            if (event.isAddition())
            {
               this.coord.localHandler.addProxy(ap.getAddress(), ap.getPort());
            }
            else
            {
               this.coord.localHandler.removeProxy(ap.getAddress(), ap.getPort());
            }
            
            ModClusterService.this.proxyChangeDigest.put(event.getSender(), event);
            
            return new GroupRpcResponse(ModClusterService.this.getHAPartition().getClusterNode());
         }
      }
      
      /**
       * @see org.jboss.modcluster.ha.rpc.ModClusterServiceRpcHandler#getClusterCoordinatorState(java.util.Set)
       */
      public GroupRpcResponse getClusterCoordinatorState(Set<MCMPServer> masterList)
      {
         // TODO is this the correct response here?
         if (this.coord.isMasterNode()) return null;
         
         Set<MCMPServerState> ourStates = this.coord.clusteredHandler.updateServersFromMasterNode(masterList);
         
         boolean needReset = this.coord.clusteredHandler.getNeedsResetTransmission();
         
         Map<String, Set<ResetRequestSource.VirtualHost>> map = Collections.emptyMap();
         List<MCMPRequest> resetRequests = needReset ? this.coord.resetRequestSource.getLocalResetRequests(map) : null;
         
         ClusterNode node = ModClusterService.this.getHAPartition().getClusterNode();
         List<MCMPServerDiscoveryEvent> events = this.coord.clusteredHandler.getPendingDiscoveryEvents();
         
         GroupRpcResponse response = new ModClusterServiceStateGroupRpcResponse(node, this.coord.latestLoad, ourStates, events, resetRequests);
         
         if (needReset)
         {
            this.coord.clusteredHandler.recordResetTransmission();
         }
         
         return response;
      }
      
      /**
       * @see org.jboss.modcluster.ha.rpc.ModClusterServiceRpcHandler#clusterStatusComplete(java.util.Map)
       */
      public void clusterStatusComplete(Map<ClusterNode, PeerMCMPDiscoveryStatus> statuses)
      {
         HAPartition partition = this.coord.getHAPartition();
         ClusterNode cn = partition.getClusterNode();
         PeerMCMPDiscoveryStatus newStatus = statuses.get(cn);
         if (newStatus != null)
         {
            // Notify our handler that discovery events have been processed
            this.coord.clusteredHandler.discoveryEventsReceived(newStatus.getLatestDiscoveryEvent());
            
            // Notify our handler that any reset requests have been processed
            this.coord.clusteredHandler.recordResetSuccess();
            
            DistributedReplicantManager drm = partition.getDistributedReplicantManager();
            String key = this.coord.getHAServiceKey();
            ModClusterServiceDRMEntry oldStatus = (ModClusterServiceDRMEntry) drm.lookupLocalReplicant(key);
            if (!newStatus.equals(oldStatus))
            {
               try
               {
                  drm.add(key, new ModClusterServiceDRMEntry(cn, newStatus.getMCMPServerStates(), oldStatus.getJvmRoutes()));
               }
               catch (Exception e)
               {
                  this.coord.log.error(ModClusterService.this.sm.getString("modcluster.error.drm"), e);
               }
            }
         }
      }

      /**
       * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#getProxyConfiguration()
       */
      public GroupRpcResponse getProxyConfiguration()
      {
         if (!this.coord.isMasterNode()) return null;
         
         ClusterNode node = ModClusterService.this.getHAPartition().getClusterNode();
         String configuration = this.coord.localHandler.getProxyConfiguration();
         
         return new StringGroupRpcResponse(node, configuration);
      }

      /**
       * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#getProxyInfo()
       */
      public GroupRpcResponse getProxyInfo()
      {
         if (!this.coord.isMasterNode()) return null;
         
         ClusterNode node = ModClusterService.this.getHAPartition().getClusterNode();
         String info = this.coord.localHandler.getProxyInfo();
         
         return new StringGroupRpcResponse(node, info);
      }

      /**
       * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#isProxyHealthOK()
       */
      public GroupRpcResponse isProxyHealthOK()
      {
         if (!this.coord.isMasterNode()) return null;
         
         ClusterNode node = ModClusterService.this.getHAPartition().getClusterNode();
         boolean ok = this.coord.localHandler.isProxyHealthOK();
         
         return new BooleanGroupRpcResponse(node, ok);
      }

      /**
       * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#markProxiesInError()
       */
      public GroupRpcResponse markProxiesInError()
      {
         if (!this.coord.isMasterNode()) return null;
         
         this.coord.localHandler.markProxiesInError();
         
         return this.SUCCESS;
      }

      /**
       * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#reset()
       */
      public GroupRpcResponse reset()
      {
         if (!this.coord.isMasterNode()) return null;
         
         this.coord.localHandler.reset();
         
         return this.SUCCESS;
      }

      /**
       * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#sendRequest(org.jboss.modcluster.mcmp.MCMPRequest)
       */
      public GroupRpcResponse sendRequest(MCMPRequest request)
      {
         if (!this.coord.isMasterNode()) return null;
         
         this.coord.localHandler.sendRequest(request);
         
         return this.SUCCESS;
      }

      /**
       * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#sendRequests(java.util.List)
       */
      public GroupRpcResponse sendRequests(List<MCMPRequest> requests)
      {
         if (!this.coord.isMasterNode()) return null;
         
         this.coord.localHandler.sendRequests(requests);
         
         return this.SUCCESS;
      }
      
      /**
       * @see org.jboss.modcluster.ha.rpc.ResetRequestSourceRpcHandler#getResetRequests()
       */
      public GroupRpcResponse getResetRequests(Map<String, Set<ResetRequestSource.VirtualHost>> response)
      {
         ClusterNode node = ModClusterService.this.getHAPartition().getClusterNode();
         List<MCMPRequest> requests = this.coord.resetRequestSource.getLocalResetRequests(response);
         
         return new ResetRequestGroupRpcResponse(node, requests);
      }
   }
   
   private class ClusteredJBossWebEventHandler extends DefaultJBossWebEventHandler
   {
      private final ModClusterService coord = ModClusterService.this;
      
      /**
       * Create a new ClusteredJBossWebEventHandler.
       * 
       * @param config
       * @param loadFactorProvider
       */
      public ClusteredJBossWebEventHandler(NodeConfiguration nodeConfig, BalancerConfiguration balancerConfig,
            MCMPHandlerConfiguration mcmpConfig, MCMPHandler clusteredHandler, LoadBalanceFactorProvider loadFactorProvider)
      {
         super(nodeConfig, balancerConfig, mcmpConfig, clusteredHandler, loadFactorProvider);
      }
      
      @Override
      public void startServer(Server server)
      {
         // Pass on ref to our server
         this.coord.resetRequestSource.setJbossWebServer(server);

         super.startServer(server);
      }

      @Override
      protected void config(Engine engine)
      {
         this.config(engine, this.coord.localHandler);
      }
      
      @Override
      protected void jvmRouteEstablished(Engine engine)
      {
         this.coord.drmEntry.addJvmRoute(engine.getJvmRoute());
         this.coord.updateLocalDRM(this.coord.drmEntry);
      }

       @Override
      protected void removeAll(Engine engine)
      {
         super.removeAll(engine);
         
         this.coord.drmEntry.removeJvmRoute(engine.getJvmRoute());
         this.coord.updateLocalDRM(this.coord.drmEntry);
      }
      
      @Override
      public void status(Engine engine)
      {
         log.debug(this.coord.sm.getString("modcluster.engine.status", engine.getName()));
         
         this.coord.latestLoad = this.getLoadBalanceFactor();
         
         if (this.coord.isMasterNode())
         {
            this.coord.statusCount = (this.coord.statusCount + 1) % this.coord.processStatusFrequency;

            if (this.coord.statusCount == 0)
            {
               this.updateClusterStatus();
            }
         }
      }
      
      @SuppressWarnings("unchecked")
      void updateClusterStatus()
      {
         Set<MCMPServerState> masterList = null;
         Map<ClusterNode, MCMPServerDiscoveryEvent> latestEvents = null;
         Map<ClusterNode, ModClusterServiceDRMEntry> nonresponsive = new HashMap<ClusterNode, ModClusterServiceDRMEntry>();
         Map<String, Integer> loadBalanceFactors = new HashMap<String, Integer>();
         Map<ClusterNode, PeerMCMPDiscoveryStatus> statuses = new HashMap<ClusterNode, PeerMCMPDiscoveryStatus>();
         List<MCMPRequest> resetRequests = new ArrayList<MCMPRequest>();
         HAPartition partition = this.coord.getHAPartition();
         DistributedReplicantManager drm = partition.getDistributedReplicantManager();
         boolean resync = false;
         
         do
         {
            resync = false;
            
            this.coord.localHandler.status();
            
            synchronized (this.coord.proxyChangeDigest)
            {
               masterList = this.coord.localHandler.getProxyStates();
               latestEvents = new HashMap<ClusterNode, MCMPServerDiscoveryEvent>(this.coord.proxyChangeDigest);
            }
            
            List<ModClusterServiceDRMEntry> replicants = drm.lookupReplicants(this.coord.getHAServiceKey());
            nonresponsive.clear();
            
            for (ModClusterServiceDRMEntry replicant: replicants)
            {
               nonresponsive.put(replicant.getPeer(), replicant);
            }
            nonresponsive.remove(partition.getClusterNode());
            
            // FIXME -- what about our own dropped discovery events if we just became master?
            List responses = this.coord.getClusterCoordinatorState(masterList);
            
            // Gather up all the reset requests in one list
            // FIXME -- what about our own dropped requests if we just became master?
            resetRequests.clear();
            
            // Gather all the load balance factors
            loadBalanceFactors.clear();
            
            // Add our own lbf - it is not returned via getclusterCoordinatorState(...)
            for (String jvmRoute: this.coord.drmEntry.getJvmRoutes())
            {
               loadBalanceFactors.put(jvmRoute, this.coord.latestLoad);
            }
            
            // Gather the info on who knows about what proxies
            statuses.clear();
            
            for (Object response: responses)
            {
               if (response instanceof ModClusterServiceStateGroupRpcResponse)
               {
                  ModClusterServiceStateGroupRpcResponse mcssgrr = (ModClusterServiceStateGroupRpcResponse) response;
                  ClusterNode cn = mcssgrr.getSender();
                  
                  // Check for discovery events we haven't processed
                  MCMPServerDiscoveryEvent latestEvent = latestEvents.get(cn);
                  
                  for (MCMPServerDiscoveryEvent toCheck: mcssgrr.getUnacknowledgedEvents())
                  {
                     if (latestEvent != null && latestEvent.getEventIndex() <= toCheck.getEventIndex())
                     {
                        continue; // already processed it
                     }
                  
                     AddressPort ap = toCheck.getMCMPServer();
                     if (toCheck.isAddition())
                     {
                        this.coord.localHandler.addProxy(ap.getAddress(), ap.getPort());
                     }
                     else
                     {
                        this.coord.localHandler.removeProxy(ap.getAddress(), ap.getPort());
                     }
                     resync = true;
                  }
                  
                  if (!resync) // don't bother if we are going to start over
                  {
                     statuses.put(cn, new PeerMCMPDiscoveryStatus(cn, mcssgrr.getStates(), latestEvent));
                     
                     List<MCMPRequest> toAdd = mcssgrr.getResetRequests();
                     if (toAdd != null)
                     {
                        resetRequests.addAll(toAdd);
                     }
                     
                     ModClusterServiceDRMEntry removed = nonresponsive.remove(cn);
                     if (removed != null)
                     {
                        Integer lbf = Integer.valueOf(mcssgrr.getLoadBalanceFactor());
                        for (String jvmRoute: removed.getJvmRoutes())
                        {
                           loadBalanceFactors.put(jvmRoute, lbf);
                        }
                     }
                  }
               }
               else if (response instanceof ThrowableGroupRpcResponse)
               {
                  ThrowableGroupRpcResponse tgrr = (ThrowableGroupRpcResponse) response;
                  ClusterNode cn = tgrr.getSender();
                  
                  log.warn(this.coord.sm.getString("modcluster.error.rpc.known", "getClusterCoordinatorState", cn), tgrr.getValue());
                  
                  // Don't remove from nonresponsive list and we'll pass back an error
                  // status (null server list) to this peer
               }
               else if (response instanceof Throwable)
               {
                  log.warn(this.coord.sm.getString("modcluster.error.rpc.unknown", "getClusterCoordinatorState"), (Throwable) response);
               }
               else
               {
                  log.error(this.coord.sm.getString("modcluster.error.rpc.unexpected", response, "getClusterCoordinatorState"));
               }
            }
         }
         // We picked up previously unknown discovery events; start over
         while (resync);
         
         // Add error-state objects for non-responsive peers
         Integer lbf = Integer.valueOf(0);
         for (Map.Entry<ClusterNode, ModClusterServiceDRMEntry> entry: nonresponsive.entrySet())
         {
            ClusterNode cn = entry.getKey();
            statuses.put(entry.getKey(), new PeerMCMPDiscoveryStatus(cn, null, latestEvents.get(cn)));
            
            for (String jvmRoute: entry.getValue().getJvmRoutes())
            {
               loadBalanceFactors.put(jvmRoute, lbf);
            }
         }
         // FIXME handle crashed members, gone from DRM
         
         // Advise the proxies of any reset requests
         this.coord.localHandler.sendRequests(resetRequests);
         
         // Pass along the LBF values
         List<MCMPRequest> statusRequests = new ArrayList<MCMPRequest>();
         for (Map.Entry<String, Integer> entry: loadBalanceFactors.entrySet())
         {
            statusRequests.add(MCMPUtils.createStatusRequest(entry.getKey(), entry.getValue().intValue()));
         }
         this.coord.localHandler.sendRequests(statusRequests);
         
         // Advise the members the process is done and that they should update DRM
         this.notifyClusterStatusComplete(masterList, statuses);
      }

      private void notifyClusterStatusComplete(Set<MCMPServerState> masterList, Map<ClusterNode, PeerMCMPDiscoveryStatus> statuses)
      {
         HAPartition partition = this.coord.getHAPartition();
         
         // Determine who should update DRM first -- us or the rest of the nodes
         Set<ModClusterServiceDRMEntry> allStatuses = new HashSet<ModClusterServiceDRMEntry>(statuses.values());
         DistributedReplicantManager drm = partition.getDistributedReplicantManager();
         ModClusterServiceDRMEntry ourCurrentStatus = (ModClusterServiceDRMEntry) drm.lookupLocalReplicant(this.coord.getHAServiceKey());
         allStatuses.add(ourCurrentStatus);
         
         ClusterNode node = partition.getClusterNode();
         
         boolean othersFirst = this.coord.narrowCandidateList(allStatuses).contains(node);
         ModClusterServiceDRMEntry newStatus = new ModClusterServiceDRMEntry(node, masterList, this.coord.drmEntry.getJvmRoutes());
         boolean updated = !newStatus.equals(ourCurrentStatus);
         
         if (othersFirst)
         {
            this.coord.clusterStatusComplete(statuses);
         }
         
         if (updated)
         {
            this.coord.updateLocalDRM(newStatus);
         }
         
         if (!othersFirst)
         {
            this.coord.clusterStatusComplete(statuses);
         }
      }
   }
}
