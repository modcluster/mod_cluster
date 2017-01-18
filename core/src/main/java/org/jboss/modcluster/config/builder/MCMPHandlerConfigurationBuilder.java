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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;

import org.jboss.modcluster.config.JvmRouteFactory;
import org.jboss.modcluster.config.MCMPHandlerConfiguration;
import org.jboss.modcluster.config.ProxyConfiguration;
import org.jboss.modcluster.config.SessionDrainingStrategy;
import org.jboss.modcluster.config.impl.MCMPHandlerConfigurationImpl;
import org.jboss.modcluster.config.impl.SessionDrainingStrategyEnum;
import org.jboss.modcluster.config.impl.SystemPropertyJvmRouteFactory;
import org.jboss.modcluster.config.impl.UUIDJvmRouteFactory;

/**
 * @author Radoslav Husar
 * @since 1.3.6.Final
 */
public class MCMPHandlerConfigurationBuilder extends AbstractConfigurationBuilder implements Creator<MCMPHandlerConfiguration> {

    private Collection<ProxyConfiguration> proxyConfigurations = Collections.emptySet();
    private String proxyURL;
    private int socketTimeout = 20000;
    private SocketFactory socketFactory = SocketFactory.getDefault();
    private Map<String, Set<String>> excludedContextsPerHost = Collections.emptyMap();
    private Boolean advertise;
    private boolean autoEnableContexts = true;
    private long stopContextTimeout = 10;
    private TimeUnit stopContextTimeoutUnit = TimeUnit.SECONDS;
    private JvmRouteFactory jvmRouteFactory = new SystemPropertyJvmRouteFactory(new UUIDJvmRouteFactory());
    private SessionDrainingStrategy sessionDrainingStrategy = SessionDrainingStrategyEnum.DEFAULT;

    MCMPHandlerConfigurationBuilder(ConfigurationBuilder parentBuilder) {
        super(parentBuilder);
    }

    public MCMPHandlerConfigurationBuilder setProxyConfigurations(Collection<ProxyConfiguration> proxyConfigurations) {
        this.proxyConfigurations = proxyConfigurations;
        return this;
    }

    public MCMPHandlerConfigurationBuilder setProxyURL(String proxyURL) {
        this.proxyURL = proxyURL;
        return this;
    }

    public MCMPHandlerConfigurationBuilder setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
        return this;
    }

    public MCMPHandlerConfigurationBuilder setSocketFactory(SocketFactory socketFactory) {
        this.socketFactory = socketFactory;
        return this;
    }

    public MCMPHandlerConfigurationBuilder setExcludedContextsPerHost(Map<String, Set<String>> excludedContextsPerHost) {
        this.excludedContextsPerHost = excludedContextsPerHost;
        return this;
    }

    public MCMPHandlerConfigurationBuilder setAdvertise(Boolean advertise) {
        this.advertise = advertise;
        return this;
    }

    public MCMPHandlerConfigurationBuilder setAutoEnableContexts(boolean autoEnableContexts) {
        this.autoEnableContexts = autoEnableContexts;
        return this;
    }

    public MCMPHandlerConfigurationBuilder setStopContextTimeout(long stopContextTimeout) {
        this.stopContextTimeout = stopContextTimeout;
        return this;
    }

    public MCMPHandlerConfigurationBuilder setStopContextTimeoutUnit(TimeUnit stopContextTimeoutUnit) {
        this.stopContextTimeoutUnit = stopContextTimeoutUnit;
        return this;
    }

    public MCMPHandlerConfigurationBuilder setJvmRouteFactory(JvmRouteFactory jvmRouteFactory) {
        this.jvmRouteFactory = jvmRouteFactory;
        return this;
    }

    public MCMPHandlerConfigurationBuilder setSessionDrainingStrategy(SessionDrainingStrategy sessionDrainingStrategy) {
        this.sessionDrainingStrategy = sessionDrainingStrategy;
        return this;
    }

    @Override
    public MCMPHandlerConfiguration create() {
        return new MCMPHandlerConfigurationImpl(proxyConfigurations, proxyURL, socketTimeout, socketFactory, excludedContextsPerHost, advertise, autoEnableContexts, stopContextTimeout, stopContextTimeoutUnit, jvmRouteFactory, sessionDrainingStrategy);
    }
}