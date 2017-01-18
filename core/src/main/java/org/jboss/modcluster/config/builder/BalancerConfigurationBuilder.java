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

import org.jboss.modcluster.config.BalancerConfiguration;
import org.jboss.modcluster.config.impl.BalancerConfigurationImpl;

/**
 * @author Radoslav Husar
 * @since 1.3.6.Final
 */
public class BalancerConfigurationBuilder extends AbstractConfigurationBuilder implements Creator<BalancerConfiguration> {

    private boolean stickySession = true;
    private boolean stickySessionRemove = false;
    private boolean stickySessionForce = false;
    private int workerTimeout = -1;
    private int maxAttempts = -1;

    BalancerConfigurationBuilder(ConfigurationBuilder parentBuilder) {
        super(parentBuilder);
    }

    public BalancerConfigurationBuilder setStickySession(boolean stickySession) {
        this.stickySession = stickySession;
        return this;
    }

    public BalancerConfigurationBuilder setStickySessionRemove(boolean stickySessionRemove) {
        this.stickySessionRemove = stickySessionRemove;
        return this;
    }

    public BalancerConfigurationBuilder setStickySessionForce(boolean stickySessionForce) {
        this.stickySessionForce = stickySessionForce;
        return this;
    }

    public BalancerConfigurationBuilder setWorkerTimeout(int workerTimeout) {
        this.workerTimeout = workerTimeout;
        return this;
    }

    public BalancerConfigurationBuilder setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
        return this;
    }

    @Override
    public BalancerConfiguration create() {
        return new BalancerConfigurationImpl(stickySession, stickySessionRemove, stickySessionForce, workerTimeout, maxAttempts);
    }
}