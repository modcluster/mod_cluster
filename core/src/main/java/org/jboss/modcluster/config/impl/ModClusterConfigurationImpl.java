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

package org.jboss.modcluster.config.impl;

import org.jboss.modcluster.config.AdvertiseConfiguration;
import org.jboss.modcluster.config.BalancerConfiguration;
import org.jboss.modcluster.config.MCMPHandlerConfiguration;
import org.jboss.modcluster.config.ModClusterConfiguration;
import org.jboss.modcluster.config.NodeConfiguration;

/**
 * @author Radoslav Husar
 * @since 1.3.6.Final
 */
public class ModClusterConfigurationImpl implements ModClusterConfiguration {

    private final AdvertiseConfiguration advertiseConfiguration;
    private final BalancerConfiguration balancerConfiguration;
    private final NodeConfiguration nodeConfiguration;
    private final MCMPHandlerConfiguration mcmpHandlerConfiguration;

    public ModClusterConfigurationImpl(AdvertiseConfiguration advertiseConfiguration, BalancerConfiguration balancerConfiguration, NodeConfiguration nodeConfiguration, MCMPHandlerConfiguration mcmpHandlerConfiguration) {
        this.advertiseConfiguration = advertiseConfiguration;
        this.balancerConfiguration = balancerConfiguration;
        this.nodeConfiguration = nodeConfiguration;
        this.mcmpHandlerConfiguration = mcmpHandlerConfiguration;
    }

    @Override
    public AdvertiseConfiguration getAdvertiseConfiguration() {
        return advertiseConfiguration;
    }

    @Override
    public BalancerConfiguration getBalancerConfiguration() {
        return balancerConfiguration;
    }

    @Override
    public NodeConfiguration getNodeConfiguration() {
        return nodeConfiguration;
    }

    @Override
    public MCMPHandlerConfiguration getMCMPHandlerConfiguration() {
        return mcmpHandlerConfiguration;
    }
}
