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
import org.junit.Test;

/**
 * @author Radoslav Husar
 */
public class ModClusterConfigurationBuilderTest {

    @Test
    public void testBuilder() {
        final ModClusterConfiguration configuration = new ModClusterConfigurationBuilder()
                .advertise()
                .setAdvertiseSocketAddress(null)
                .setAdvertiseInterface(null)
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
