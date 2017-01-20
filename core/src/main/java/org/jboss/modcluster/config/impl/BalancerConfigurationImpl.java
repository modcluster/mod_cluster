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

import org.jboss.modcluster.config.BalancerConfiguration;

/**
 * @author Radoslav Husar
 * @since 1.3.6.Final
 */
public class BalancerConfigurationImpl implements BalancerConfiguration {

    private final boolean stickySession;
    private final boolean stickySessionRemove;
    private final boolean stickySessionForce;
    private final int workerTimeout;
    private final int maxAttempts;

    public BalancerConfigurationImpl(boolean stickySession, boolean stickySessionRemove, boolean stickySessionForce, int workerTimeout, int maxAttempts) {
        this.stickySession = stickySession;
        this.stickySessionRemove = stickySessionRemove;
        this.stickySessionForce = stickySessionForce;
        this.workerTimeout = workerTimeout;
        this.maxAttempts = maxAttempts;
    }

    @Override
    public boolean getStickySession() {
        return stickySession;
    }

    @Override
    public boolean getStickySessionRemove() {
        return stickySessionRemove;
    }

    @Override
    public boolean getStickySessionForce() {
        return stickySessionForce;
    }

    @Override
    public int getWorkerTimeout() {
        return workerTimeout;
    }

    @Override
    public int getMaxAttempts() {
        return maxAttempts;
    }
}
