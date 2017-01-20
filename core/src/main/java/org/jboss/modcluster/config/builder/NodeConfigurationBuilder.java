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

    public NodeConfigurationBuilder setLoadBalancingGroup(String loadBalancingGroup) {
        this.loadBalancingGroup = loadBalancingGroup;
        return this;
    }

    public NodeConfigurationBuilder setFlushPackets(boolean flushPackets) {
        this.flushPackets = flushPackets;
        return this;
    }

    public NodeConfigurationBuilder setFlushWait(int flushWait) {
        this.flushWait = flushWait;
        return this;
    }

    public NodeConfigurationBuilder setPing(int ping) {
        this.ping = ping;
        return this;
    }

    public NodeConfigurationBuilder setSmax(int smax) {
        this.smax = smax;
        return this;
    }

    public NodeConfigurationBuilder setTtl(int ttl) {
        this.ttl = ttl;
        return this;
    }

    public NodeConfigurationBuilder setNodeTimeout(int nodeTimeout) {
        this.nodeTimeout = nodeTimeout;
        return this;
    }

    public NodeConfigurationBuilder setBalancer(String balancer) {
        this.balancer = balancer;
        return this;
    }

    @Override
    public NodeConfiguration create() {
        return new NodeConfigurationImpl(loadBalancingGroup, flushPackets, flushWait, ping, smax, ttl, nodeTimeout, balancer);
    }
}