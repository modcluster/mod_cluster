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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import org.jboss.ha.framework.interfaces.HAServiceKeyProvider;
import org.jboss.ha.framework.interfaces.HASingletonMBean;
import org.jboss.logging.Logger;
import org.jboss.modcluster.Strings;
import org.jboss.modcluster.Utils;
import org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler;
import org.jboss.modcluster.ha.rpc.DefaultRpcResponse;
import org.jboss.modcluster.ha.rpc.MCMPServerDiscoveryEvent;
import org.jboss.modcluster.ha.rpc.PeerMCMPDiscoveryStatus;
import org.jboss.modcluster.ha.rpc.RpcResponse;
import org.jboss.modcluster.ha.rpc.RpcResponseFilter;
import org.jboss.modcluster.mcmp.MCMPConnectionListener;
import org.jboss.modcluster.mcmp.MCMPHandler;
import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPServer;
import org.jboss.modcluster.mcmp.MCMPServerState;

@ThreadSafe
public class ClusteredMCMPHandlerImpl implements ClusteredMCMPHandler {
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

    private enum ResetState {
        NONE, REQUIRED, PENDING
    }

    private AtomicReference<ResetState> resetState = new AtomicReference<ResetState>(ResetState.NONE);

    @GuardedBy("pendingDiscoveryEvents")
    private List<MCMPServerDiscoveryEvent> pendingDiscoveryEvents = new LinkedList<MCMPServerDiscoveryEvent>();

    private AtomicInteger discoveryEventIndex = new AtomicInteger();

    public ClusteredMCMPHandlerImpl(MCMPHandler localHandler, HASingletonMBean singleton,
            HAServiceKeyProvider serviceKeyProvider) {
        this.localHandler = localHandler;
        this.singleton = singleton;
        this.serviceKeyProvider = serviceKeyProvider;
    }

    // --------------------------------------------------- ClusteredMCMPHandler

    /**
     * @{inheritDoc
     * @see org.jboss.modcluster.ha.ClusteredMCMPHandler#getPendingDiscoveryEvents()
     */
    public List<MCMPServerDiscoveryEvent> getPendingDiscoveryEvents() {
        synchronized (this.pendingDiscoveryEvents) {
            return new ArrayList<MCMPServerDiscoveryEvent>(this.pendingDiscoveryEvents);
        }
    }

    /**
     * @{inheritDoc
     * @see org.jboss.modcluster.ha.ClusteredMCMPHandler#discoveryEventsReceived(org.jboss.modcluster.ha.rpc.MCMPServerDiscoveryEvent)
     */
    public void discoveryEventsReceived(PeerMCMPDiscoveryStatus status) {
        MCMPServerDiscoveryEvent latestEvent = status.getLatestDiscoveryEvent();

        if (latestEvent != null) {
            synchronized (this.pendingDiscoveryEvents) {
                Iterator<MCMPServerDiscoveryEvent> events = this.pendingDiscoveryEvents.iterator();

                while (events.hasNext() && (latestEvent.compareTo(events.next()) >= 0)) {
                    events.remove();
                }
            }
        }
    }

    /**
     * @{inheritDoc
     * @see org.jboss.modcluster.ha.ClusteredMCMPHandler#updateServersFromMasterNode(java.util.Set)
     */
    public Set<MCMPServerState> updateServersFromMasterNode(Set<MCMPServer> masterList) {
        for (MCMPServer server : masterList) {
            this.localHandler.addProxy(server.getSocketAddress(), server.isEstablished());
        }

        for (MCMPServer server : this.localHandler.getProxyStates()) {
            if (!masterList.contains(server)) {
                this.localHandler.removeProxy(server.getSocketAddress());
            }
        }

        this.localHandler.status();

        return this.localHandler.getProxyStates();
    }

    /**
     * @{inheritDoc
     * @see org.jboss.modcluster.ha.ClusteredMCMPHandler#isResetNecessary()
     */
    public boolean isResetNecessary() {
        return this.resetState.get() == ResetState.REQUIRED;
    }

    /**
     * @{inheritDoc
     * @see org.jboss.modcluster.ha.ClusteredMCMPHandler#resetInitiated()
     */
    public void resetInitiated() {
        this.resetState.set(ResetState.PENDING);
    }

    /**
     * @{inheritDoc
     * @see org.jboss.modcluster.ha.ClusteredMCMPHandler#resetCompleted()
     */
    public void resetCompleted() {
        this.resetState.compareAndSet(ResetState.PENDING, ResetState.NONE);
    }

    // ------------------------------------------------------------ MCMPHandler

    /**
     * @{inheritDoc
     * @see org.jboss.modcluster.mcmp.MCMPHandler#addProxy(java.net.InetAddress, int)
     */
    public void addProxy(InetSocketAddress socketAddress) {
        if (this.singleton.isMasterNode()) {
            this.localHandler.addProxy(socketAddress);
        } else {
            this.sendDiscoveryEventToPartition(socketAddress, true);
        }
    }

    /**
     * @{inheritDoc
     * @see org.jboss.modcluster.mcmp.MCMPHandler#addProxy(java.net.InetAddress, int, boolean)
     */
    public void addProxy(InetSocketAddress socketAddress, boolean established) {
        this.localHandler.addProxy(socketAddress, established);
    }

    /**
     * @{inheritDoc
     * @see org.jboss.modcluster.mcmp.MCMPHandler#removeProxy(java.net.InetAddress, int)
     */
    public void removeProxy(InetSocketAddress socketAddress) {
        if (this.singleton.isMasterNode()) {
            this.localHandler.removeProxy(socketAddress);
        } else {
            this.sendDiscoveryEventToPartition(socketAddress, false);
        }
    }

    /**
     * @{inheritDoc
     * @see org.jboss.modcluster.mcmp.MCMPHandler#getProxyStates()
     */
    public Set<MCMPServerState> getProxyStates() {
        return this.localHandler.getProxyStates();
    }

    /**
     * {@inhericDoc}
     * 
     * @see org.jboss.modcluster.mcmp.MCMPHandler#init(java.util.List)
     */
    public void init(Collection<InetSocketAddress> initialProxies, MCMPConnectionListener listener) {
        if (this.singleton.isMasterNode()) {
            this.localHandler.init(initialProxies, listener);
        } else {
            this.localHandler.init(new ArrayList<InetSocketAddress>(), listener);

            if (initialProxies != null) {
                for (InetSocketAddress proxy : initialProxies) {
                    this.sendDiscoveryEventToPartition(proxy, true);
                }
            }
        }
    }

    /**
     * {@inhericDoc}
     * 
     * @see org.jboss.modcluster.mcmp.MCMPHandler#isProxyHealthOK()
     */
    public boolean isProxyHealthOK() {
        if (this.singleton.isMasterNode()) {
            return this.localHandler.isProxyHealthOK();
        }

        return this.rpcStub.isProxyHealthOK().getResult().booleanValue();
    }

    /**
     * {@inhericDoc}
     * 
     * @see org.jboss.modcluster.mcmp.MCMPHandler#markProxiesInError()
     */
    public void markProxiesInError() {
        this.recordRequestFailure();

        if (this.singleton.isMasterNode()) {
            this.localHandler.markProxiesInError();
        } else {
            this.rpcStub.markProxiesInError().getResult();
        }
    }

    /**
     * {@inhericDoc}
     * 
     * @see org.jboss.modcluster.mcmp.MCMPHandler#reset()
     */
    public void reset() {
        if (this.singleton.isMasterNode()) {
            this.localHandler.reset();
        } else {
            this.rpcStub.reset().getResult();
        }
    }

    /**
     * {@inhericDoc}
     * 
     * @see org.jboss.modcluster.mcmp.MCMPHandler#sendRequest(org.jboss.modcluster.mcmp.MCMPRequest)
     */
    public Map<MCMPServerState, String> sendRequest(MCMPRequest request) {
        if (this.singleton.isMasterNode()) {
            return this.localHandler.sendRequest(request);
        }

        try {
            return this.rpcStub.sendRequest(request).getResult();
        } catch (RuntimeException e) {
            this.recordRequestFailure();

            log.warn(e.getMessage(), e);

            return null;
        }
    }

    /**
     * {@inhericDoc}
     * 
     * @see org.jboss.modcluster.mcmp.MCMPHandler#sendRequests(java.util.List)
     */
    public Map<MCMPServerState, List<String>> sendRequests(List<MCMPRequest> requests) {
        if (this.singleton.isMasterNode()) {
            return this.localHandler.sendRequests(requests);
        }

        try {
            return this.rpcStub.sendRequests(requests).getResult();
        } catch (RuntimeException e) {
            this.recordRequestFailure();

            log.warn(e.getMessage(), e);

            return null;
        }
    }

    /**
     * {@inhericDoc}
     * 
     * @see org.jboss.modcluster.mcmp.MCMPHandler#shutdown()
     */
    public void shutdown() {
        this.localHandler.shutdown();
    }

    /**
     * {@inhericDoc}
     * 
     * @see org.jboss.modcluster.mcmp.MCMPHandler#status()
     */
    public void status() {
        log.warn(Strings.ERROR_STATUS_UNSUPPORTED.getString());
    }

    private void sendDiscoveryEventToPartition(InetSocketAddress socketAddress, boolean addition) {
        synchronized (this.pendingDiscoveryEvents) {
            // Ensure discovery event enters queue sequentially by index
            MCMPServerDiscoveryEvent event = new MCMPServerDiscoveryEvent(this.serviceKeyProvider.getHAPartition()
                    .getClusterNode(), socketAddress, addition, this.discoveryEventIndex.incrementAndGet());

            this.pendingDiscoveryEvents.add(event);

            try {
                this.rpcStub.mcmpServerDiscoveryEvent(event).getResult();
            } catch (RuntimeException e) {
                // Just log it; we'll retry later
                Strings key = addition ? Strings.ERROR_DISCOVERY_ADD : Strings.ERROR_DISCOVERY_REMOVE;
                log.error(key.getString(socketAddress), e);
            }
        }
    }

    void recordRequestFailure() {
        this.resetState.set(ResetState.REQUIRED);
    }

    class RpcStub implements ClusteredMCMPHandlerRpcHandler {
        /**
         * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#isProxyHealthOK()
         */
        public RpcResponse<Boolean> isProxyHealthOK() {
            return this.invokeRpc("isProxyHealthOk");
        }

        /**
         * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#markProxiesInError()
         */
        public RpcResponse<Void> markProxiesInError() {
            return this.invokeRpc("markProxiesInError");
        }

        /**
         * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#mcmpServerDiscoveryEvent(org.jboss.modcluster.ha.rpc.MCMPServerDiscoveryEvent)
         */
        public RpcResponse<Void> mcmpServerDiscoveryEvent(MCMPServerDiscoveryEvent event) {
            try {
                return this.invokeRpc("mcmpServerDiscoveryEvent", new Object[] { event }, DISC_EVENT_TYPES);
            } catch (Exception e) {
                DefaultRpcResponse<Void> response = new DefaultRpcResponse<Void>(null);
                response.setException(e);
                return response;
            }
        }

        /**
         * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#reset()
         */
        public RpcResponse<Void> reset() {
            return this.invokeRpc("reset");
        }

        /**
         * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#sendRequest(org.jboss.modcluster.mcmp.MCMPRequest)
         */
        public RpcResponse<Map<MCMPServerState, String>> sendRequest(MCMPRequest request) {
            return this.invokeRpc("sendRequest", new Object[] { request }, MCMPREQ_TYPES, true);
        }

        /**
         * @see org.jboss.modcluster.ha.rpc.ClusteredMCMPHandlerRpcHandler#sendRequests(java.util.List)
         */
        public RpcResponse<Map<MCMPServerState, List<String>>> sendRequests(List<MCMPRequest> requests) {
            return this.invokeRpc("sendRequests", new Object[] { requests }, MCMPREQS_TYPES, true);
        }

        private <T> RpcResponse<T> invokeRpc(String methodName) {
            return this.invokeRpc(methodName, NULL_ARGS, NULL_TYPES, false);
        }

        private <T> RpcResponse<T> invokeRpc(String methodName, Object[] args, Class<?>[] types, boolean recordFailure) {
            try {
                return this.invokeRpc(methodName, args, types);
            } catch (Exception e) {
                if (recordFailure) {
                    ClusteredMCMPHandlerImpl.this.recordRequestFailure();
                }

                throw Utils.convertToUnchecked(e);
            }
        }

        @SuppressWarnings({ "unchecked", "deprecation" })
        private <T> RpcResponse<T> invokeRpc(String methodName, Object[] args, Class<?>[] types) throws Exception {
            List<?> responses = ClusteredMCMPHandlerImpl.this.serviceKeyProvider.getHAPartition().callMethodOnCluster(
                    ClusteredMCMPHandlerImpl.this.serviceKeyProvider.getHAServiceKey(), methodName, args, types, false,
                    new RpcResponseFilter());

            Throwable thrown = null;

            for (Object obj : responses) {
                if (obj instanceof RpcResponse<?>) {
                    return (RpcResponse<T>) obj;
                } else if (obj instanceof Throwable) {
                    if (thrown == null) {
                        thrown = (Throwable) obj;
                    }
                } else {
                    log.warn(Strings.ERROR_RPC_UNEXPECTED.getString(obj, methodName));
                }
            }

            if (thrown != null) {
                throw Utils.convertToUnchecked(thrown);
            }

            throw new IllegalStateException(Strings.ERROR_RPC_NO_RESPONSE.getString(methodName));
        }
    }
}
