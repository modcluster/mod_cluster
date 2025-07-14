/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
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
 * Builder for MCMP (Mod-Cluster Management Protocol) handler configuration.
 *
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

    /**
     * Sets a static list of proxies to register with as a collection of {@link ProxyConfiguration}s.
     */
    public MCMPHandlerConfigurationBuilder setProxyConfigurations(Collection<ProxyConfiguration> proxyConfigurations) {
        this.proxyConfigurations = proxyConfigurations;
        return this;
    }

    /**
     * Sets URL prefix to send with commands to mod_cluster. Default is no prefix.
     */
    public MCMPHandlerConfigurationBuilder setProxyURL(String proxyURL) {
        this.proxyURL = proxyURL;
        return this;
    }

    /**
     * Sets connection timeout for communication with the proxy.
     */
    public MCMPHandlerConfigurationBuilder setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
        return this;
    }

    /**
     * Sets socket factory for communication with the proxy; supply an SSL socket factory to use SSL to connect to the proxy.
     */
    public MCMPHandlerConfigurationBuilder setSocketFactory(SocketFactory socketFactory) {
        this.socketFactory = socketFactory;
        return this;
    }

    /**
     * Sets a map of contexts per host that should never be registered by mod_cluster with the proxy.
     */
    public MCMPHandlerConfigurationBuilder setExcludedContextsPerHost(Map<String, Set<String>> excludedContextsPerHost) {
        this.excludedContextsPerHost = excludedContextsPerHost;
        return this;
    }

    /**
     * Sets whether to receive advertisements from httpd proxies.
     */
    public MCMPHandlerConfigurationBuilder setAdvertise(Boolean advertise) {
        this.advertise = advertise;
        return this;
    }

    /**
     * Sets whether or not to automatically enable contexts. If false, context will need to be enabled manually.
     */
    public MCMPHandlerConfigurationBuilder setAutoEnableContexts(boolean autoEnableContexts) {
        this.autoEnableContexts = autoEnableContexts;
        return this;
    }

    /**
     * Sets the number of {@link #setStopContextTimeoutUnit(TimeUnit)} to wait for pending requests to complete when stopping a context.
     */
    public MCMPHandlerConfigurationBuilder setStopContextTimeout(long stopContextTimeout) {
        this.stopContextTimeout = stopContextTimeout;
        return this;
    }

    /**
     * Sets the unit of time to which {@link #setStopContextTimeout(long)} pertains.
     */
    public MCMPHandlerConfigurationBuilder setStopContextTimeoutUnit(TimeUnit stopContextTimeoutUnit) {
        this.stopContextTimeoutUnit = stopContextTimeoutUnit;
        return this;
    }

    /**
     * Sets a factory for generating a JVM route.
     */
    public MCMPHandlerConfigurationBuilder setJvmRouteFactory(JvmRouteFactory jvmRouteFactory) {
        this.jvmRouteFactory = jvmRouteFactory;
        return this;
    }

    /**
     * Configures the strategy for draining sessions from a context.
     */
    public MCMPHandlerConfigurationBuilder setSessionDrainingStrategy(SessionDrainingStrategy sessionDrainingStrategy) {
        this.sessionDrainingStrategy = sessionDrainingStrategy;
        return this;
    }

    @Override
    public MCMPHandlerConfiguration create() {
        return new MCMPHandlerConfigurationImpl(proxyConfigurations, proxyURL, socketTimeout, socketFactory, excludedContextsPerHost, advertise, autoEnableContexts, stopContextTimeout, stopContextTimeoutUnit, jvmRouteFactory, sessionDrainingStrategy);
    }
}