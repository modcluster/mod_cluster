/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.mcmp.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Host;
import org.jboss.modcluster.container.Server;
import org.jboss.modcluster.config.BalancerConfiguration;
import org.jboss.modcluster.config.NodeConfiguration;
import org.jboss.modcluster.mcmp.ContextFilter;
import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPRequestFactory;
import org.jboss.modcluster.mcmp.ResetRequestSource;

/**
 * @author Paul Ferraro
 */
public class ResetRequestSourceImpl implements ResetRequestSource {
    private final NodeConfiguration nodeConfig;
    private final BalancerConfiguration balancerConfig;
    private final MCMPRequestFactory requestFactory;

    private volatile ContextFilter contextFilter;
    private volatile Server server;

    public ResetRequestSourceImpl(NodeConfiguration nodeConfig, BalancerConfiguration balancerConfig,
            MCMPRequestFactory requestFactory) {
        this.nodeConfig = nodeConfig;
        this.balancerConfig = balancerConfig;
        this.requestFactory = requestFactory;
    }

    @Override
    public void init(Server server, ContextFilter contextFilter) {
        this.contextFilter = contextFilter;
        this.server = server;
    }

    @Override
    public List<MCMPRequest> getResetRequests(Map<String, Set<VirtualHost>> response) {
        List<MCMPRequest> requests = new ArrayList<MCMPRequest>();

        if (this.server == null)
            return requests;

        boolean contextAutoEnableAllowed = this.contextFilter.isAutoEnableContexts();

        List<MCMPRequest> engineRequests = new LinkedList<MCMPRequest>();

        for (Engine engine : this.server.getEngines()) {
            Connector connector = engine.getProxyConnector();
            if (connector == null) {
                // Skip config for Engines that don't have any available connector
                continue;
            }
            engineRequests.add(this.requestFactory.createConfigRequest(engine, this.nodeConfig, this.balancerConfig));

            String jvmRoute = engine.getJvmRoute();

            Set<ResetRequestSource.VirtualHost> responseHosts = response.containsKey(jvmRoute) ? response.get(jvmRoute)
                    : Collections.<ResetRequestSource.VirtualHost> emptySet();

            for (Host host : engine.getHosts()) {
                String hostName = host.getName();
                Set<String> aliases = host.getAliases();

                VirtualHost responseHost = null;

                for (VirtualHost virtualHost : responseHosts) {
                    if (virtualHost.getAliases().contains(hostName)) {
                        responseHost = virtualHost;
                        break;
                    }
                }

                Set<String> responseAliases = Collections.emptySet();
                Map<String, ResetRequestSource.Status> responseContexts = Collections.emptyMap();

                if (responseHost != null) {
                    responseAliases = responseHost.getAliases();
                    responseContexts = responseHost.getContexts();

                    // If the host(or aliases) is missing - force full reset
                    if (!aliases.equals(responseAliases)) {
                        engineRequests.add(0, this.requestFactory.createRemoveRequest(engine));
                    }
                }

                Set<String> obsoleteContexts = new HashSet<String>(responseContexts.keySet());
                Set<String> excludedHostContexts = this.contextFilter.getExcludedContexts(host);

                for (Context context : host.getContexts()) {
                    String contextPath = context.getPath();

                    if ((excludedHostContexts == null) || !excludedHostContexts.contains(contextPath)) {
                        String path = (contextPath.length() == 0) ? "/" : contextPath;

                        obsoleteContexts.remove(path);

                        ResetRequestSource.Status status = responseContexts.get(path);

                        if (context.isStarted()) {
                            if (status != ResetRequestSource.Status.ENABLED) {
                                engineRequests.add(contextAutoEnableAllowed ? this.requestFactory.createEnableRequest(context)
                                        : this.requestFactory.createDisableRequest(context));
                            }
                        } else {
                            if (status == ResetRequestSource.Status.ENABLED || status == null) {
                                // Two cases are handled here:
                                // 1. Context is not started, but proxy reports the context as ENABLED
                                //    => send STOP request, so that proxy disables the context.
                                // 2. Context is not started, proxy is not aware of the context (status == null)
                                //    => send STOP reqeust, so that proxy registers the context.
                                engineRequests.add(this.requestFactory.createStopRequest(context));
                            }
                        }
                    }
                }

                if (!obsoleteContexts.isEmpty()) {
                    // If all contexts from response no longer exist - remove all
                    if (obsoleteContexts.size() == responseContexts.size()) {
                        // Send REMOVE-APP * request first
                        engineRequests.add(0, this.requestFactory.createRemoveRequest(engine));
                    }
                    // otherwise only remove those that no longer exist
                    else {
                        for (String context : obsoleteContexts) {
                            engineRequests.add(0,
                                    this.requestFactory.createRemoveContextRequest(jvmRoute, responseAliases, context));
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
