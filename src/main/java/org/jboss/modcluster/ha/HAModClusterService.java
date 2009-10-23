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

package org.jboss.modcluster.ha;

import java.io.Serializable;
import java.net.InetSocketAddress;
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
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.util.StringManager;
import org.jboss.beans.metadata.api.annotations.Inject;
import org.jboss.beans.metadata.api.model.FromContext;
import org.jboss.ha.framework.interfaces.CachableMarshalledValue;
import org.jboss.ha.framework.interfaces.ClusterNode;
import org.jboss.ha.framework.interfaces.DistributedReplicantManager;
import org.jboss.ha.framework.interfaces.HAPartition;
import org.jboss.ha.framework.interfaces.HASingletonElectionPolicy;
import org.jboss.ha.framework.server.HAServiceEvent;
import org.jboss.ha.framework.server.HAServiceEventFactory;
import org.jboss.ha.framework.server.HAServiceRpcHandler;
import org.jboss.ha.framework.server.HASingletonImpl;
import org.jboss.ha.framework.server.SimpleCachableMarshalledValue;
import org.jboss.modcluster.Constants;
import org.jboss.modcluster.ContainerEventHandler;
import org.jboss.modcluster.CatalinaEventHandler;
import org.jboss.modcluster.CatalinaEventHandlerAdapter;
import org.jboss.modcluster.ServerProvider;
import org.jboss.modcluster.Utils;
import org.jboss.modcluster.advertise.AdvertiseListenerFactory;
import org.jboss.modcluster.advertise.impl.AdvertiseListenerFactoryImpl;
import org.jboss.modcluster.config.BalancerConfiguration;
import org.jboss.modcluster.config.MCMPHandlerConfiguration;
import org.jboss.modcluster.config.NodeConfiguration;
import org.jboss.modcluster.config.ha.HAConfiguration;
import org.jboss.modcluster.config.ha.HAModClusterConfig;
import org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler;
import org.jboss.modcluster.ha.rpc.DefaultRpcResponse;
import org.jboss.modcluster.ha.rpc.MCMPServerDiscoveryEvent;
import org.jboss.modcluster.ha.rpc.ModClusterServiceRpcHandler;
import org.jboss.modcluster.ha.rpc.ModClusterServiceStatus;
import org.jboss.modcluster.ha.rpc.PeerMCMPDiscoveryStatus;
import org.jboss.modcluster.ha.rpc.ResetRequestSourceRpcHandler;
import org.jboss.modcluster.ha.rpc.RpcResponse;
import org.jboss.modcluster.load.LoadBalanceFactorProvider;
import org.jboss.modcluster.load.LoadBalanceFactorProviderFactory;
import org.jboss.modcluster.mcmp.MCMPHandler;
import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPRequestFactory;
import org.jboss.modcluster.mcmp.MCMPServer;
import org.jboss.modcluster.mcmp.MCMPServerState;
import org.jboss.modcluster.mcmp.ResetRequestSource;
import org.jboss.modcluster.mcmp.impl.DefaultMCMPHandler;
import org.jboss.modcluster.mcmp.impl.DefaultMCMPRequestFactory;

/**
 * A ModClusterService.
 * 
 * @author Brian Stansberry
 * @version $Revision$
 */
public class HAModClusterService extends HASingletonImpl<HAServiceEvent>
   implements LifecycleListener, HAModClusterServiceMBean, ModClusterServiceRpcHandler<List<RpcResponse<ModClusterServiceStatus>>, MCMPServerState>, LoadBalanceFactorProviderFactory, ServerProvider<Server>
{
   private static final Class<?>[] CLUSTER_STATUS_COMPLETE_TYPES = new Class[] { Map.class };
   private static final Class<?>[] GET_CLUSTER_COORDINATOR_STATE_TYPES = new Class[] { Set.class };
   
   // -----------------------------------------------------------------  Fields
   
   final MCMPHandler localHandler;
   final MCMPRequestFactory requestFactory;
   final ClusteredMCMPHandler clusteredHandler;
   final HASingletonAwareResetRequestSource resetRequestSource;
   final Map<ClusterNode, MCMPServerDiscoveryEvent> proxyChangeDigest = new HashMap<ClusterNode, MCMPServerDiscoveryEvent>();
   final ModClusterServiceDRMEntry drmEntry;
   
   /**
    * The string manager for this package.
    */
   final StringManager sm = StringManager.getManager(Constants.Package);
   
   private final ServerProvider<Server> serverProvider;
   private final LifecycleListener lifecycleListener;
   private final LoadBalanceFactorProvider loadBalanceFactorProvider;
   private final RpcHandler rpcHandler;
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
   public HAModClusterService(HAPartition partition,
                               HAModClusterConfig config,
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
   public HAModClusterService(HAPartition partition,
                               HAModClusterConfig config,
                               LoadBalanceFactorProvider loadFactorProvider,
                               HASingletonElectionPolicy electionPolicy)
   {
      super(new HAServiceEventFactory());
      
      assert partition != null          : this.sm.getString("modcluster.error.iae.null", "partition");
      assert loadFactorProvider != null : this.sm.getString("modcluster.error.iae.null", "loadFactorProvider");
      assert config != null             : this.sm.getString("modcluster.error.iae.null", "config is null");
      
      this.setHAPartition(partition);
      
      this.loadBalanceFactorProvider = loadFactorProvider;
      this.requestFactory = new DefaultMCMPRequestFactory();
      this.resetRequestSource = new HASingletonAwareResetRequestSourceImpl(config, config, this, this.requestFactory, this, this);
      this.localHandler = new DefaultMCMPHandler(config, this.resetRequestSource, this.requestFactory);
      this.clusteredHandler = new ClusteredMCMPHandlerImpl(this.localHandler, this, this);
      
      ContainerEventHandler<Server, Engine, Context> eventHandler = new ClusteredCatalinaEventHandler(config, config, config, this.clusteredHandler, this.resetRequestSource, this.requestFactory, this, new AdvertiseListenerFactoryImpl());
      
      this.serverProvider = eventHandler;
      this.lifecycleListener = new CatalinaEventHandlerAdapter(eventHandler);
      
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
   protected HAModClusterService(HAPartition partition,
                                  NodeConfiguration nodeConfig,
                                  BalancerConfiguration balancerConfig,
                                  MCMPHandlerConfiguration mcmpConfig,
                                  HAConfiguration haConfig,
                                  MCMPHandler localHandler,
                                  ServerProvider<Server> serverProvider,
                                  MCMPRequestFactory requestFactory,
                                  HASingletonAwareResetRequestSource resetRequestSource,
                                  ClusteredMCMPHandler clusteredHandler,
                                  LifecycleListener lifecycleListener,
                                  LoadBalanceFactorProvider loadFactorProvider,
                                  HASingletonElectionPolicy electionPolicy)
   {
      super(new HAServiceEventFactory());
      
      this.setHAPartition(partition);
      
      this.loadBalanceFactorProvider = loadFactorProvider;
      this.localHandler = localHandler;
      this.serverProvider = serverProvider;
      this.requestFactory = requestFactory;
      this.resetRequestSource = resetRequestSource;
      this.clusteredHandler = clusteredHandler;
      this.lifecycleListener = lifecycleListener;

      this.domain = nodeConfig.getDomain();
      this.masterPerDomain = haConfig.isMasterPerDomain();
      
      this.setElectionPolicy(electionPolicy);
      
      this.drmEntry = new ModClusterServiceDRMEntry(partition.getClusterNode(), null);
      
      this.rpcHandler = new RpcHandler();
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ServerProvider#getServer()
    */
   public Server getServer()
   {
      return this.serverProvider.getServer();
   }

   /**
    * @{inheritDoc}
    * @see org.apache.catalina.LifecycleListener#lifecycleEvent(org.apache.catalina.LifecycleEvent)
    */
   public void lifecycleEvent(LifecycleEvent event)
   {
      this.lifecycleListener.lifecycleEvent(event);
   }
   
   // -------------------------------------------------- ModClusterServiceMBean

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.load.LoadBalanceFactorProviderFactory#createLoadBalanceFactorProvider()
    */
   public LoadBalanceFactorProvider createLoadBalanceFactorProvider()
   {
      return this.loadBalanceFactorProvider;
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#addProxy(java.lang.String, int)
    */
   public void addProxy(String host, int port)
   {
      this.clusteredHandler.addProxy(host, port);
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#removeProxy(java.lang.String, int)
    */
   public void removeProxy(String host, int port)
   {
      this.clusteredHandler.removeProxy(host, port);
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#getProxyInfo()
    */
   public String getProxyInfo()
   {
      return this.clusteredHandler.getProxyInfo();
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#getProxyConfiguration()
    */
   public String getProxyConfiguration()
   {
      return this.clusteredHandler.getProxyConfiguration();
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#refresh()
    */
   public void refresh()
   {
      this.clusteredHandler.markProxiesInError();
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#reset()
    */
   public void reset()
   {
      this.clusteredHandler.reset();
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#disable()
    */
   public boolean disable()
   {
      for (Service service: this.serverProvider.getServer().findServices())
      {
         Engine engine = (Engine) service.getContainer();
         // Send DISABLE-APP * request
         MCMPRequest request = this.requestFactory.createDisableRequest(engine);
         this.clusteredHandler.sendRequest(request);
      }
      
      return this.clusteredHandler.isProxyHealthOK();
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#enable()
    */
   public boolean enable()
   {
      for (Service service: this.serverProvider.getServer().findServices())
      {
         Engine engine = (Engine) service.getContainer();
         // Send ENABLE-APP * request
         MCMPRequest request = this.requestFactory.createEnableRequest(engine);
         this.clusteredHandler.sendRequest(request);
      }
      
      return this.clusteredHandler.isProxyHealthOK();
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#disable(java.lang.String, java.lang.String)
    */
   public boolean disable(String host, String path)
   {
      Context context = Utils.getContext(Utils.getHost(this.serverProvider.getServer(), host), path);
      
      // Send DISABLE-APP /... request
      MCMPRequest request = this.requestFactory.createDisableRequest(context);
      this.clusteredHandler.sendRequest(request);
      
      return this.clusteredHandler.isProxyHealthOK();
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#enable(java.lang.String, java.lang.String)
    */
   public boolean enable(String host, String path)
   {
      Context context = Utils.getContext(Utils.getHost(this.serverProvider.getServer(), host), path);

      // Send ENABLE-APP /... request
      MCMPRequest request = this.requestFactory.createEnableRequest(context);
      this.clusteredHandler.sendRequest(request);
      
      return this.clusteredHandler.isProxyHealthOK();
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
    * @return a {@link SimpleCachableMarshalledValue} containing a {@link ModClusterServiceDRMEntry}
    */
   @Override
   protected Serializable getReplicant()
   {
      return this.createReplicant(this.drmEntry);
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.ha.framework.server.HAServiceImpl#replicantsChanged(java.lang.String, java.util.List, int, boolean)
    */
/*
   @SuppressWarnings("unchecked")
   @Override
   public void replicantsChanged(String key, List newReplicants, int newViewId, boolean merge)
   {
      if (this.getHAServiceKey().equals(key))
      {
         this.partitionTopologyChanged(newReplicants, newViewId, merge);
      }
   }

   private MCMPServerState getStatus(String node)
   {
      return null;
   }
*/   
   /**
    * {@inheritDoc}
    * @return a list of cluster nodes from which to elect a new master
    */
   @Override
   protected List<ClusterNode> getElectionCandidates()
   {
      return this.narrowCandidateList(this.lookupDRMEntries());
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
   public List<RpcResponse<ModClusterServiceStatus>> getClusterCoordinatorState(Set<MCMPServerState> masterList)
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
   
   void updateLocalDRM(ModClusterServiceDRMEntry entry)
   {
      DistributedReplicantManager drm = this.getHAPartition().getDistributedReplicantManager();
      
      try
      {
         drm.add(this.getHAServiceKey(), this.createReplicant(entry));
      }
      catch (Exception e)
      {
         throw Utils.convertToUnchecked(e);
      }
   }

   @SuppressWarnings("unchecked")
   List<ModClusterServiceDRMEntry> lookupDRMEntries()
   {
      DistributedReplicantManager drm = this.getHAPartition().getDistributedReplicantManager();
      List<CachableMarshalledValue> values = drm.lookupReplicants(this.getHAServiceKey());
      
      if (values == null) return null;
      
      List<ModClusterServiceDRMEntry> entries = new ArrayList<ModClusterServiceDRMEntry>(values.size());
      
      for (CachableMarshalledValue value: values)
      {
         entries.add(this.toDRMEntry(value));
      }
      
      return entries;
   }
   
   ModClusterServiceDRMEntry lookupLocalDRMEntry()
   {
      DistributedReplicantManager drm = this.getHAPartition().getDistributedReplicantManager();
      
      return this.toDRMEntry((CachableMarshalledValue) drm.lookupLocalReplicant(this.getHAServiceKey()));
   }
   
   private Serializable createReplicant(ModClusterServiceDRMEntry entry)
   {
      return new SimpleCachableMarshalledValue(entry);
   }
   
   private ModClusterServiceDRMEntry toDRMEntry(CachableMarshalledValue value)
   {
      if (value == null) return null;
      
      try
      {
         Object entry = value.get();
         
         // MODCLUSTER-88: This can happen if service was redeployed, and DRM contains objects from the obsolete classloader
         if (!(entry instanceof ModClusterServiceDRMEntry))
         {
            // Force re-deserialization w/current classloader
            value.toByteArray();
            
            entry = value.get();
         }
         
         return (ModClusterServiceDRMEntry) entry;
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
   @SuppressWarnings("synthetic-access")
   protected class RpcHandler extends HASingletonImpl<HAServiceEvent>.RpcHandler implements ModClusterServiceRpcHandler<RpcResponse<ModClusterServiceStatus>, MCMPServer>, ClusteredMCMPHandlerRpcHandler, ResetRequestSourceRpcHandler<RpcResponse<List<MCMPRequest>>>
   {
      private final HAModClusterService coord = HAModClusterService.this;
      private final ClusterNode node = this.coord.getHAPartition().getClusterNode();
      private final RpcResponse<Void> voidResponse = new DefaultRpcResponse<Void>(this.node);
      
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
      public RpcResponse<Void> mcmpServerDiscoveryEvent(MCMPServerDiscoveryEvent event)
      {
         if (!this.coord.isMasterNode()) return null;
         
         synchronized (HAModClusterService.this.proxyChangeDigest)
         {
            InetSocketAddress socketAddress = event.getMCMPServer();
            
            if (event.isAddition())
            {
               this.coord.localHandler.addProxy(socketAddress.getAddress(), socketAddress.getPort());
            }
            else
            {
               this.coord.localHandler.removeProxy(socketAddress.getAddress(), socketAddress.getPort());
            }
            
            HAModClusterService.this.proxyChangeDigest.put(event.getSender(), event);
            
            return this.voidResponse;
         }
      }
      
      /**
       * @see org.jboss.modcluster.ha.rpc.ModClusterServiceRpcHandler#getClusterCoordinatorState(java.util.Set)
       */
      public RpcResponse<ModClusterServiceStatus> getClusterCoordinatorState(Set<MCMPServer> masterList)
      {
         // TODO is this the correct response here?
         if (this.coord.isMasterNode()) return null;
         
         Set<MCMPServerState> ourStates = this.coord.clusteredHandler.updateServersFromMasterNode(masterList);
         
         boolean needReset = this.coord.clusteredHandler.isResetNecessary();
         
         Map<String, Set<ResetRequestSource.VirtualHost>> map = Collections.emptyMap();
         List<MCMPRequest> resetRequests = needReset ? this.coord.resetRequestSource.getLocalResetRequests(map) : null;
         
         List<MCMPServerDiscoveryEvent> events = this.coord.clusteredHandler.getPendingDiscoveryEvents();
         
         DefaultRpcResponse<ModClusterServiceStatus> response = new DefaultRpcResponse<ModClusterServiceStatus>(this.node);
         
         response.setResult(new ModClusterServiceStatus(this.coord.latestLoad, ourStates, events, resetRequests));
         
         if (needReset)
         {
            this.coord.clusteredHandler.resetInitiated();
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
         PeerMCMPDiscoveryStatus status = statuses.get(cn);
         if (status != null)
         {
            // Notify our handler that discovery events have been processed
            this.coord.clusteredHandler.discoveryEventsReceived(status);
            
            // Notify our handler that any reset requests have been processed
            this.coord.clusteredHandler.resetCompleted();
            
            ModClusterServiceDRMEntry previousStatus = this.coord.lookupLocalDRMEntry();
            if (!status.equals(previousStatus))
            {
               try
               {
                  this.coord.updateLocalDRM(new ModClusterServiceDRMEntry(cn, status.getMCMPServerStates(), previousStatus.getJvmRoutes()));
               }
               catch (Exception e)
               {
                  this.coord.log.error(HAModClusterService.this.sm.getString("modcluster.error.drm"), e);
               }
            }
         }
      }

      /**
       * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#getProxyConfiguration()
       */
      public RpcResponse<String> getProxyConfiguration()
      {
         if (!this.coord.isMasterNode()) return null;
         
         DefaultRpcResponse<String> response = new DefaultRpcResponse<String>(this.node);
         
         response.setResult(this.coord.localHandler.getProxyConfiguration());
         
         return response;
      }

      /**
       * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#getProxyInfo()
       */
      public RpcResponse<String> getProxyInfo()
      {
         if (!this.coord.isMasterNode()) return null;
         
         DefaultRpcResponse<String> response = new DefaultRpcResponse<String>(this.node);
         
         response.setResult(this.coord.localHandler.getProxyInfo());
         
         return response;
      }

      /**
       * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#isProxyHealthOK()
       */
      public RpcResponse<Boolean> isProxyHealthOK()
      {
         if (!this.coord.isMasterNode()) return null;
         
         DefaultRpcResponse<Boolean> response = new DefaultRpcResponse<Boolean>(this.node);
         
         response.setResult(Boolean.valueOf(this.coord.localHandler.isProxyHealthOK()));
         
         return response;
      }

      /**
       * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#markProxiesInError()
       */
      public RpcResponse<Void> markProxiesInError()
      {
         if (!this.coord.isMasterNode()) return null;
         
         this.coord.localHandler.markProxiesInError();
         
         return this.voidResponse;
      }

      /**
       * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#reset()
       */
      public RpcResponse<Void> reset()
      {
         if (!this.coord.isMasterNode()) return null;
         
         this.coord.localHandler.reset();
         
         return this.voidResponse;
      }

      /**
       * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#sendRequest(org.jboss.modcluster.mcmp.MCMPRequest)
       */
      public RpcResponse<Map<MCMPServerState, String>> sendRequest(MCMPRequest request)
      {
         if (!this.coord.isMasterNode()) return null;
         
         DefaultRpcResponse<Map<MCMPServerState, String>> response = new DefaultRpcResponse<Map<MCMPServerState, String>>(this.node);
         
         response.setResult(this.coord.localHandler.sendRequest(request));
         
         return response;
      }

      /**
       * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#sendRequests(java.util.List)
       */
      public RpcResponse<Map<MCMPServerState, List<String>>> sendRequests(List<MCMPRequest> requests)
      {
         if (!this.coord.isMasterNode()) return null;
         
         DefaultRpcResponse<Map<MCMPServerState, List<String>>> response = new DefaultRpcResponse<Map<MCMPServerState, List<String>>>(this.node);
         
         response.setResult(this.coord.localHandler.sendRequests(requests));
         
         return response;
      }
      
      /**
       * @see org.jboss.modcluster.ha.rpc.ResetRequestSourceRpcHandler#getResetRequests()
       */
      public RpcResponse<List<MCMPRequest>> getResetRequests(Map<String, Set<ResetRequestSource.VirtualHost>> infoResponse)
      {
         DefaultRpcResponse<List<MCMPRequest>> response = new DefaultRpcResponse<List<MCMPRequest>>(this.node);
         response.setResult(this.coord.resetRequestSource.getLocalResetRequests(infoResponse));
         return response;
      }
   }
   
   @SuppressWarnings("synthetic-access")
   private class ClusteredCatalinaEventHandler extends CatalinaEventHandler
   {
      private final HAModClusterService coord = HAModClusterService.this;
      
      /**
       * Create a new ClusteredJBossWebEventHandler.
       * 
       * @param config
       * @param loadFactorProvider
       */
      public ClusteredCatalinaEventHandler(NodeConfiguration nodeConfig, BalancerConfiguration balancerConfig,
            MCMPHandlerConfiguration mcmpConfig, MCMPHandler clusteredHandler, ResetRequestSource source,
            MCMPRequestFactory requestFactory, LoadBalanceFactorProviderFactory loadFactorProviderFactory,
            AdvertiseListenerFactory listenerFactory)
      {
         super(nodeConfig, balancerConfig, mcmpConfig, clusteredHandler, source, requestFactory, loadFactorProviderFactory, listenerFactory);
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
         this.checkInit();
         
         this.coord.log.debug(this.coord.sm.getString("modcluster.engine.status", engine.getName()));
         
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
      
      void updateClusterStatus()
      {
         Set<MCMPServerState> masterList = null;
         Map<ClusterNode, MCMPServerDiscoveryEvent> latestEvents = null;
         Map<ClusterNode, ModClusterServiceDRMEntry> nonresponsive = new HashMap<ClusterNode, ModClusterServiceDRMEntry>();
         Map<String, Integer> loadBalanceFactors = new HashMap<String, Integer>();
         Map<ClusterNode, PeerMCMPDiscoveryStatus> statuses = new HashMap<ClusterNode, PeerMCMPDiscoveryStatus>();
         List<MCMPRequest> resetRequests = new ArrayList<MCMPRequest>();
         HAPartition partition = this.coord.getHAPartition();
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
            
            List<ModClusterServiceDRMEntry> replicants = this.coord.lookupDRMEntries();
            nonresponsive.clear();
            
            for (ModClusterServiceDRMEntry replicant: replicants)
            {
               nonresponsive.put(replicant.getPeer(), replicant);
            }
            nonresponsive.remove(partition.getClusterNode());
            
            // FIXME -- what about our own dropped discovery events if we just became master?
            List<RpcResponse<ModClusterServiceStatus>> responses = this.coord.getClusterCoordinatorState(masterList);
            
            // Gather up all the reset requests in one list
            // FIXME -- what about our own dropped requests if we just became master?
            resetRequests.clear();
            
            // Gather all the load balance factors
            loadBalanceFactors.clear();
            
            // Add our own lbf - it is not returned via getClusterCoordinatorState(...)
            for (String jvmRoute: this.coord.drmEntry.getJvmRoutes())
            {
               loadBalanceFactors.put(jvmRoute, Integer.valueOf(this.coord.latestLoad));
            }
            
            // Gather the info on who knows about what proxies
            statuses.clear();
            
            for (RpcResponse<ModClusterServiceStatus> response: responses)
            {
               if (response == null) continue;
               
               ClusterNode node = response.getSender();
               
               try
               {
                  ModClusterServiceStatus state = response.getResult();
                  
                  // Check for discovery events we haven't processed
                  MCMPServerDiscoveryEvent latestEvent = latestEvents.get(node);

                  for (MCMPServerDiscoveryEvent event: state.getUnacknowledgedEvents())
                  {
                     if ((latestEvent == null) || (latestEvent.compareTo(event) < 0))
                     {
                        InetSocketAddress socketAddress = event.getMCMPServer();
                        if (event.isAddition())
                        {
                           this.coord.localHandler.addProxy(socketAddress.getAddress(), socketAddress.getPort());
                        }
                        else
                        {
                           this.coord.localHandler.removeProxy(socketAddress.getAddress(), socketAddress.getPort());
                        }
                        resync = true;
                     }
                  }
                  
                  if (!resync) // don't bother if we are going to start over
                  {
                     statuses.put(node, new PeerMCMPDiscoveryStatus(node, state.getStates(), latestEvent));
                     
                     List<MCMPRequest> toAdd = state.getResetRequests();
                     if (toAdd != null)
                     {
                        resetRequests.addAll(toAdd);
                     }
                     
                     ModClusterServiceDRMEntry removed = nonresponsive.remove(node);
                     if (removed != null)
                     {
                        Integer lbf = Integer.valueOf(state.getLoadBalanceFactor());
                        for (String jvmRoute: removed.getJvmRoutes())
                        {
                           loadBalanceFactors.put(jvmRoute, lbf);
                        }
                     }
                  }
               }
               catch (Exception e)
               {
                  this.coord.log.warn(this.coord.sm.getString("modcluster.error.rpc.known", "getClusterCoordinatorState", node), e);
                  
                  // Don't remove from nonresponsive list and we'll pass back an error
                  // status (null server list) to this peer
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
            statusRequests.add(this.coord.requestFactory.createStatusRequest(entry.getKey(), entry.getValue().intValue()));
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
         ModClusterServiceDRMEntry ourCurrentStatus = this.coord.lookupLocalDRMEntry();
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
