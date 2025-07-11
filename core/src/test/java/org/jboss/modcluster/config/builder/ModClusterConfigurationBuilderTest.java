/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.config.builder;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;

import org.jboss.modcluster.ModClusterService;
import org.jboss.modcluster.config.ModClusterConfiguration;
import org.jboss.modcluster.config.ProxyConfiguration;
import org.jboss.modcluster.config.impl.SessionDrainingStrategyEnum;
import org.jboss.modcluster.config.impl.UUIDJvmRouteFactory;
import org.jboss.modcluster.load.impl.SimpleLoadBalanceFactorProvider;
import org.junit.jupiter.api.Test;

/**
 * @author Radoslav Husar
 */
public class ModClusterConfigurationBuilderTest {

    @Test
    void testBuilder() {
        final ModClusterConfiguration configuration = new ModClusterConfigurationBuilder()
                .advertise()
                .setAdvertiseSocketAddress(null)
                .setAdvertiseInterface((NetworkInterface) null)
                .setAdvertiseThreadFactory(null)
                .setAdvertiseSecurityKey("key")

                .balancer()
                .setStickySession(true)
                .setStickySessionRemove(true)
                .setStickySessionForce(true)
                .setWorkerTimeout(10)
                .setMaxAttempts(5)

                .node()
                .setLoadBalancingGroup("lbgroup")
                .setFlushPackets(true)
                .setFlushWait(1)
                .setPing(1)
                .setSmax(2)
                .setTtl(1)
                .setNodeTimeout(1)
                .setBalancer("test")

                .mcmp()
                .setProxyConfigurations(Collections.<ProxyConfiguration>emptyList())
                .setProxyURL("/")
                .setSocketTimeout(1)
                .setSocketFactory(SocketFactory.getDefault())
                .setExcludedContextsPerHost(new HashMap<String, Set<String>>())
                .setAdvertise(false)
                .setAutoEnableContexts(false)
                .setStopContextTimeout(1)
                .setStopContextTimeoutUnit(TimeUnit.DAYS)
                .setJvmRouteFactory(new UUIDJvmRouteFactory())
                .setSessionDrainingStrategy(SessionDrainingStrategyEnum.NEVER)

                .advertise()

                .build();

        new ModClusterService(configuration, new SimpleLoadBalanceFactorProvider());
    }
}
