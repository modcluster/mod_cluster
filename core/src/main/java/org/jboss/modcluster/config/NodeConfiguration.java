/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.config;

public interface NodeConfiguration {
    /**
     * Indicates the group of servers to which this node belongs. If defined, mod_cluster will always attempt to failover a
     * given request to a node in the same group as the failed node. This property is equivalent to the mod_jk domain directive.
     */
    String getLoadBalancingGroup();

    /**
     * Allows controlling flushing of packets.
     */
    boolean getFlushPackets();

    /**
     * Time to wait before flushing packets.
     */
    int getFlushWait();

    /**
     * Time to wait for a pong answer to a ping.
     */
    int getPing();

    /**
     * Soft maximum inactive connection count.
     */
    int getSmax();

    /**
     * Maximum time on seconds for idle connections above smax.
     */
    int getTtl();

    /**
     * Maximum time on seconds for idle connections the proxy will wait to connect to the node.
     */
    int getNodeTimeout();

    /**
     * Name of the balancer.
     */
    String getBalancer();
}