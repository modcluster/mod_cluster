/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.modcluster.config.builder;

import org.jboss.modcluster.config.NodeConfiguration;
import org.jboss.modcluster.config.impl.NodeConfigurationImpl;

/**
 * Builder for proxy node configuration.
 *
 * @author Radoslav Husar
 * @since 1.3.6.Final
 */
public class NodeConfigurationBuilder extends AbstractConfigurationBuilder implements Creator<NodeConfiguration> {

    private String loadBalancingGroup;
    private boolean flushPackets = false;
    private int flushWait = -1;
    private int ping = -1;
    private int smax = -1;
    private int ttl = -1;
    private int nodeTimeout = -1;
    private String balancer;

    NodeConfigurationBuilder(ConfigurationBuilder parentBuilder) {
        super(parentBuilder);
    }

    /**
     * Indicates the group of servers to which this node belongs. If defined, mod_cluster will always attempt to failover a
     * given request to a node in the same group as the failed node. This property is equivalent to the mod_jk domain directive.
     */
    public NodeConfigurationBuilder setLoadBalancingGroup(String loadBalancingGroup) {
        this.loadBalancingGroup = loadBalancingGroup;
        return this;
    }

    /**
     * Controls flushing of packets.
     */
    public NodeConfigurationBuilder setFlushPackets(boolean flushPackets) {
        this.flushPackets = flushPackets;
        return this;
    }

    /**
     * Sets time to wait before flushing packets.
     */
    public NodeConfigurationBuilder setFlushWait(int flushWait) {
        this.flushWait = flushWait;
        return this;
    }

    /**
     * Time to wait for a pong answer to a ping.
     */
    public NodeConfigurationBuilder setPing(int ping) {
        this.ping = ping;
        return this;
    }

    /**
     * Soft maximum inactive connection count.
     */
    public NodeConfigurationBuilder setSmax(int smax) {
        this.smax = smax;
        return this;
    }

    /**
     * Maximum time on seconds for idle connections above smax.
     */
    public NodeConfigurationBuilder setTtl(int ttl) {
        this.ttl = ttl;
        return this;
    }

    /**
     * Maximum time on seconds for idle connections the proxy will wait to connect to the node.
     */
    public NodeConfigurationBuilder setNodeTimeout(int nodeTimeout) {
        this.nodeTimeout = nodeTimeout;
        return this;
    }

    /**
     * Name of the balancer.
     */
    public NodeConfigurationBuilder setBalancer(String balancer) {
        this.balancer = balancer;
        return this;
    }

    @Override
    public NodeConfiguration create() {
        return new NodeConfigurationImpl(loadBalancingGroup, flushPackets, flushWait, ping, smax, ttl, nodeTimeout, balancer);
    }
}