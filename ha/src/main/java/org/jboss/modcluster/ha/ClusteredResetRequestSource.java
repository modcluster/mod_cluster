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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.ha.framework.interfaces.HAServiceKeyProvider;
import org.jboss.ha.framework.interfaces.HASingletonMBean;
import org.jboss.logging.Logger;
import org.jboss.modcluster.Strings;
import org.jboss.modcluster.Utils;
import org.jboss.modcluster.config.BalancerConfiguration;
import org.jboss.modcluster.config.NodeConfiguration;
import org.jboss.modcluster.ha.rpc.ResetRequestSourceRpcHandler;
import org.jboss.modcluster.ha.rpc.RpcResponse;
import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPRequestFactory;
import org.jboss.modcluster.mcmp.ResetRequestSource;
import org.jboss.modcluster.mcmp.impl.ResetRequestSourceImpl;

/**
 * {@link ResetRequestSource} that provides different reset requests depending on whether or not it believes it is running on
 * the singleton master.
 * 
 * @author Brian Stansberry
 */
public class ClusteredResetRequestSource extends ResetRequestSourceImpl {
    static final String METHOD_NAME = "getResetRequests";
    static final Class<?>[] TYPES = new Class[] { Map.class };

    private static final Logger log = Logger.getLogger(ClusteredResetRequestSource.class);

    private final HASingletonMBean singleton;
    private final ResetRequestSourceRpcHandler<List<RpcResponse<List<MCMPRequest>>>> rpcStub;

    public ClusteredResetRequestSource(NodeConfiguration nodeConfig, BalancerConfiguration balancerConfig,
            MCMPRequestFactory requestFactory, HASingletonMBean singleton, HAServiceKeyProvider serviceKeyProvider) {
        super(nodeConfig, balancerConfig, requestFactory);

        this.singleton = singleton;
        this.rpcStub = new RpcStub(serviceKeyProvider);
    }

    @Override
    public List<MCMPRequest> getResetRequests(Map<String, Set<VirtualHost>> infoResponse) {
        List<MCMPRequest> resets = super.getResetRequests(infoResponse);

        if (this.singleton.isMasterNode()) {
            List<RpcResponse<List<MCMPRequest>>> responses = this.rpcStub.getResetRequests(infoResponse);

            for (RpcResponse<List<MCMPRequest>> response : responses) {
                try {
                    List<MCMPRequest> result = response.getResult();

                    if (result != null) {
                        resets.addAll(result);
                    }
                } catch (RuntimeException e) {
                    // FIXME what to do?
                    log.warn(Strings.ERROR_RPC_KNOWN.getString(METHOD_NAME, response.getSender()), e);
                }
            }
        }

        return resets;
    }

    private static class RpcStub implements ResetRequestSourceRpcHandler<List<RpcResponse<List<MCMPRequest>>>> {
        private final HAServiceKeyProvider serviceKeyProvider;

        RpcStub(HAServiceKeyProvider serviceKeyProvider) {
            this.serviceKeyProvider = serviceKeyProvider;
        }

        /**
         * @see org.jboss.modcluster.ha.rpc.ResetRequestSourceRpcHandler#getResetRequests()
         */
        @SuppressWarnings({ "unchecked", "deprecation" })
        public List<RpcResponse<List<MCMPRequest>>> getResetRequests(Map<String, Set<ResetRequestSource.VirtualHost>> response) {
            try {
                return (List<RpcResponse<List<MCMPRequest>>>) this.serviceKeyProvider.getHAPartition().callMethodOnCluster(
                        this.serviceKeyProvider.getHAServiceKey(), METHOD_NAME, new Object[] { response }, TYPES, true);
            } catch (Exception e) {
                // FIXME what to do?
                throw Utils.convertToUnchecked(e);
            }
        }
    }
}
