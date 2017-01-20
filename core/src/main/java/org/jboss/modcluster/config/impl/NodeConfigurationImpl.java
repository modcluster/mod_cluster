package org.jboss.modcluster.config.impl;

import org.jboss.modcluster.config.NodeConfiguration;

/**
 * @author Radoslav Husar
 * @since 1.3.6.Final
 */
public class NodeConfigurationImpl implements NodeConfiguration {

    private final String loadBalancingGroup;
    private final boolean flushPackets;
    private final int flushWait;
    private final int ping;
    private final int smax;
    private final int ttl;
    private final int nodeTimeout;
    private final String balancer;

    public NodeConfigurationImpl(String loadBalancingGroup, boolean flushPackets, int flushWait, int ping, int smax, int ttl, int nodeTimeout, String balancer) {
        this.loadBalancingGroup = loadBalancingGroup;
        this.flushPackets = flushPackets;
        this.flushWait = flushWait;
        this.ping = ping;
        this.smax = smax;
        this.ttl = ttl;
        this.nodeTimeout = nodeTimeout;
        this.balancer = balancer;
    }

    @Override
    public String getLoadBalancingGroup() {
        return loadBalancingGroup;
    }

    @Override
    public boolean getFlushPackets() {
        return flushPackets;
    }

    @Override
    public int getFlushWait() {
        return flushWait;
    }

    @Override
    public int getPing() {
        return ping;
    }

    @Override
    public int getSmax() {
        return smax;
    }

    @Override
    public int getTtl() {
        return ttl;
    }

    @Override
    public int getNodeTimeout() {
        return nodeTimeout;
    }

    @Override
    public String getBalancer() {
        return balancer;
    }
}
