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

import org.jboss.modcluster.config.ModClusterConfiguration;
import org.jboss.modcluster.config.impl.ModClusterConfigurationImpl;

/**
 * Builder for the main mod_cluster configuration object.
 *
 * @author Radoslav Husar
 * @since 1.3.6.Final
 */
public class ModClusterConfigurationBuilder implements ConfigurationBuilder {

    private final AdvertiseConfigurationBuilder advertise;
    private final BalancerConfigurationBuilder balancer;
    private final NodeConfigurationBuilder node;
    private final MCMPHandlerConfigurationBuilder mcmp;

    public ModClusterConfigurationBuilder() {
        advertise = new AdvertiseConfigurationBuilder(this);
        balancer = new BalancerConfigurationBuilder(this);
        node = new NodeConfigurationBuilder(this);
        mcmp = new MCMPHandlerConfigurationBuilder(this);
    }

    @Override
    public NodeConfigurationBuilder node() {
        return node;
    }

    @Override
    public AdvertiseConfigurationBuilder advertise() {
        return advertise;
    }

    @Override
    public BalancerConfigurationBuilder balancer() {
        return balancer;
    }

    @Override
    public MCMPHandlerConfigurationBuilder mcmp() {
        return mcmp;
    }

    @Override
    public ModClusterConfiguration build() {
        return new ModClusterConfigurationImpl(advertise.create(), balancer.create(), node.create(), mcmp.create());
    }
}