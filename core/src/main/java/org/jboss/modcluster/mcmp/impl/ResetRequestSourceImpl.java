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
package org.jboss.modcluster.mcmp.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * 
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

    /**
     * @{inheritDoc
     * @see org.jboss.modcluster.mcmp.ResetRequestSource#init(java.util.Map)
     */
    @Override
    public void init(Server server, ContextFilter contextFilter) {
        this.contextFilter = contextFilter;
        this.server = server;
    }

    /**
     * @{inheritDoc
     * @see org.jboss.modcluster.mcmp.ResetRequestSource#getResetRequests(java.util.Map)
     */
    @Override
    public List<MCMPRequest> getResetRequests(Map<String, Set<VirtualHost>> response) {
        List<MCMPRequest> requests = new ArrayList<MCMPRequest>();

        if (this.server == null)
            return requests;

        boolean contextAutoEnableAllowed = this.contextFilter.isAutoEnableContexts();

        List<MCMPRequest> engineRequests = new LinkedList<MCMPRequest>();

        for (Engine engine : this.server.getEngines()) {
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
                            if (status == ResetRequestSource.Status.ENABLED) {
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
