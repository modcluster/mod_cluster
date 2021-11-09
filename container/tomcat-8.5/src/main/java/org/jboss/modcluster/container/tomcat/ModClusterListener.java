/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.modcluster.container.tomcat;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Server;
import org.apache.catalina.core.StandardServer;
import org.apache.tomcat.util.modeler.Registry;
import org.jboss.logging.Logger;
import org.jboss.modcluster.ModClusterService;
import org.jboss.modcluster.ModClusterServiceMBean;
import org.jboss.modcluster.Utils;
import org.jboss.modcluster.config.JvmRouteFactory;
import org.jboss.modcluster.config.ProxyConfiguration;
import org.jboss.modcluster.config.impl.ModClusterConfig;
import org.jboss.modcluster.config.impl.SessionDrainingStrategyEnum;
import org.jboss.modcluster.load.LoadBalanceFactorProvider;
import org.jboss.modcluster.load.LoadBalanceFactorProviderFactory;
import org.jboss.modcluster.load.impl.DynamicLoadBalanceFactorProvider;
import org.jboss.modcluster.load.metric.LoadMetric;
import org.jboss.modcluster.load.metric.impl.BusyConnectorsLoadMetric;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Mod_cluster lifecycle listener for use in Tomcat.
 *
 * @author Paul Ferraro
 */
public class ModClusterListener extends ModClusterConfig implements TomcatConnectorConfiguration, LifecycleListener, LoadBalanceFactorProviderFactory, ModClusterServiceMBean {
    private static final Logger log = Logger.getLogger(ModClusterListener.class);

    private final ModClusterServiceMBean service;
    private final LifecycleListener listener;

    Class<? extends LoadMetric> loadMetricClass = BusyConnectorsLoadMetric.class;
    private int initialLoad = DynamicLoadBalanceFactorProvider.DEFAULT_INITIAL_LOAD;
    private float decayFactor = DynamicLoadBalanceFactorProvider.DEFAULT_DECAY_FACTOR;
    private int history = DynamicLoadBalanceFactorProvider.DEFAULT_HISTORY;
    private double capacity = LoadMetric.DEFAULT_CAPACITY;

    public ModClusterListener() {
        ModClusterService service = new ModClusterService(this, this);

        this.service = service;
        this.listener = ServiceLoaderTomcatFactory.load(LifecycleListenerFactory.class, TomcatEventHandlerAdapterFactory.class).createListener(service, this);
    }

    protected ModClusterListener(ModClusterServiceMBean mbean, LifecycleListener listener) {
        this.service = mbean;
        this.listener = listener;
    }

    @Override
    public LoadBalanceFactorProvider createLoadBalanceFactorProvider() {
        PrivilegedAction<LoadMetric> action = new PrivilegedAction<LoadMetric>() {
            @Override
            public LoadMetric run() {
                try {
                    return ModClusterListener.this.loadMetricClass.newInstance();
                } catch (IllegalAccessException | InstantiationException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        };
        LoadMetric metric = AccessController.doPrivileged(action);

        metric.setCapacity(this.capacity);

        DynamicLoadBalanceFactorProvider provider = new DynamicLoadBalanceFactorProvider(Collections.singleton(metric), initialLoad);

        provider.setDecayFactor(this.decayFactor);
        provider.setHistory(this.history);

        return provider;
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        this.listener.lifecycleEvent(event);

        Lifecycle source = event.getLifecycle();

        if (source instanceof Server) {
            Server server = (Server) source;
            String type = event.getType();

            // Register/unregister ModClusterListener mbean on server start/stop
            if (Lifecycle.AFTER_START_EVENT.equals(type)) {
                try {
                    ObjectName name = this.getObjectName(server);

                    Registry.getRegistry(null, null).registerComponent(this, name, ModClusterListener.class.getName());
                } catch (Exception e) {
                    log.error(e.getLocalizedMessage(), e);
                }
            } else if (Lifecycle.STOP_EVENT.equals(type)) {
                try {
                    ObjectName name = this.getObjectName(server);

                    Registry.getRegistry(null, null).unregisterComponent(name);
                } catch (Exception e) {
                    log.error(e.getLocalizedMessage(), e);
                }
            }
        }
    }

    private ObjectName getObjectName(Server server) throws MalformedObjectNameException {
        String domain = (server instanceof StandardServer) ? ((StandardServer) server).getDomain() : ManagementFactory.getPlatformMBeanServer().getDefaultDomain();
        return ObjectName.getInstance(domain, "type", "ModClusterListener");
    }

    public Class<? extends JvmRouteFactory> getJvmRouteFactoryClass() {
        return this.getJvmRouteFactory().getClass();
    }

    public void setJvmRouteFactoryClass(final Class<? extends JvmRouteFactory> factoryClass) {
        PrivilegedAction<JvmRouteFactory> action = new PrivilegedAction<JvmRouteFactory>() {
            @Override
            public JvmRouteFactory run() {
                try {
                    return factoryClass.newInstance();
                } catch (IllegalAccessException | InstantiationException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        };
        this.setJvmRouteFactory(AccessController.doPrivileged(action));
    }

    /**
     * Returns the class name of the configured load metric.
     *
     * @return the name of a class implementing {@link LoadMetric}
     */
    public String getLoadMetricClass() {
        return this.loadMetricClass.getName();
    }

    /**
     * Sets the class of the desired load metric
     *
     * @param loadMetricClass a class implementing {@link LoadMetric}
     * @throws IllegalArgumentException if metric class could not be loaded
     */
    public void setLoadMetricClass(final String loadMetricClass) {
        PrivilegedAction<Class<? extends LoadMetric>> action = new PrivilegedAction<Class<? extends LoadMetric>>() {
            @Override
            public Class<? extends LoadMetric> run() {
                try {
                    return ModClusterListener.class.getClassLoader().loadClass(loadMetricClass).asSubclass(LoadMetric.class);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        };
        this.loadMetricClass = AccessController.doPrivileged(action);
    }

    /**
     * Returns the factor by which the contribution of historical load values to the load factor calculation should
     * exponentially decay.
     *
     * @return the configured load decay factor
     */
    public float getLoadDecayFactor() {
        return this.decayFactor;
    }

    /**
     * Sets the factor by which the contribution of historical load values to the load factor calculation should exponentially
     * decay.
     *
     * @param decayFactor a positive number
     */
    public void setLoadDecayFactor(float decayFactor) {
        this.decayFactor = decayFactor;
    }

    /**
     * String-based variant of {@link ModClusterListener#setLoadDecayFactor(float)} to set float decay factor used by Tomcat modeler.
     *
     * @param decayFactor a positive number
     */
    public void setLoadDecayFactor(String decayFactor) {
        this.decayFactor = Float.parseFloat(decayFactor);
    }

    /**
     * Returns the number of historic load values used when calculating the load factor.
     *
     * @return the configured load history
     */
    public int getLoadHistory() {
        return this.history;
    }

    /**
     * Sets the number of historic load values used when calculating the load factor.
     *
     * @param history
     */
    public void setLoadHistory(int history) {
        this.history = history;
    }

    public double getLoadMetricCapacity() {
        return this.capacity;
    }

    /**
     * Sets the capacity (i.e. maximum expected value) of the load values returned by the load metric specified by
     * {@link #getLoadMetricClass()}
     *
     * @param capacity a load capacity
     */
    public void setLoadMetricCapacity(String capacity) {
        this.capacity = Double.parseDouble(capacity);
    }

    public int getInitialLoad() {
        return initialLoad;
    }

    public void setInitialLoad(int initialLoad) {
        this.initialLoad = initialLoad;
    }

    // ---------------------------------------- ModClusterServiceMBean ----------------------------------------

    @Override
    public void addProxy(String host, int port) {
        this.service.addProxy(host, port);
    }

    @Override
    public boolean disable() {
        return this.service.disable();
    }

    @Override
    public boolean disableContext(String hostName, String contextPath) {
        return this.service.disableContext(hostName, contextPath);
    }

    @Override
    public Map<InetSocketAddress, String> ping() {
        return this.service.ping();
    }

    @Override
    public Map<InetSocketAddress, String> ping(String jvmRoute) {
        return this.service.ping(jvmRoute);
    }

    @Override
    public Map<InetSocketAddress, String> ping(String scheme, String host, int port) {
        return this.service.ping(scheme, host, port);
    }

    @Override
    public boolean enable() {
        return this.service.enable();
    }

    @Override
    public boolean enableContext(String hostName, String contextPath) {
        return this.service.enableContext(hostName, contextPath);
    }

    @Override
    public Map<InetSocketAddress, String> getProxyConfiguration() {
        return this.service.getProxyConfiguration();
    }

    @Override
    public Map<InetSocketAddress, String> getProxyInfo() {
        return this.service.getProxyInfo();
    }

    @Override
    public void refresh() {
        this.service.refresh();
    }

    @Override
    public void removeProxy(String host, int port) {
        this.service.removeProxy(host, port);
    }

    @Override
    public void reset() {
        this.service.reset();
    }

    @Override
    public boolean stop(long timeout, TimeUnit unit) {
        return this.service.stop(timeout, unit);
    }

    @Override
    public boolean stopContext(String host, String path, long timeout, TimeUnit unit) {
        return this.service.stopContext(host, path, timeout, unit);
    }

    // ---------------------- RHQ support methods introduced in https://bugzilla.redhat.com/show_bug.cgi?id=822250 ----------------------

    // FIXME Why do these two following methods return only the first proxy configuration?
    // FIXME Can we fix this without breaking RHQ?
    public String getProxyConfigurationString() {
        String result = null;

        Map<InetSocketAddress, String> map = this.service.getProxyConfiguration();
        if (map.isEmpty())
            return null;
        Object[] results = map.values().toArray();
        result = (String) results[0];
        return result;
    }

    public String getProxyInfoString() {
        String result = null;

        Map<InetSocketAddress, String> map = this.service.getProxyInfo();
        if (map.isEmpty())
            return null;
        Object[] results = map.values().toArray();
        result = (String) results[0];
        return result;
    }

    public boolean stop(long timeout) {
        return this.service.stop(timeout, TimeUnit.SECONDS);
    }

    public boolean stopContext(String host, String path, long timeout) {
        return this.service.stopContext(host, path, timeout, TimeUnit.SECONDS);
    }

    // ---------------------- Tomcat connector configuration ----------------------

    private String connectorAddress;

    @Override
    public String getConnectorAddress() {
        return connectorAddress;
    }

    public void setConnectorAddress(String connectorAddress) {
        this.connectorAddress = connectorAddress;
    }

    private Integer connectorPort;

    @Override
    public Integer getConnectorPort() {
        return connectorPort;
    }

    public void setConnectorPort(int connectorPort) {
        this.connectorPort = connectorPort;
    }

    // ---------------------------------------- String-based Tomcat modeler and server.xml methods ----------------------------------------

    public String getAdvertiseGroupAddress() {
        return this.getAdvertiseSocketAddress().getHostName();
    }

    public void setAdvertiseGroupAddress(String advertiseGroupAddress) {
        try {
            this.setAdvertiseSocketAddress(new InetSocketAddress(InetAddress.getByName(advertiseGroupAddress), this.getAdvertiseSocketAddress().getPort()));
         } catch (UnknownHostException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public int getAdvertisePort() {
        return this.getAdvertiseSocketAddress().getPort();
    }

    public void setAdvertisePort(int advertisePort) {
        this.setAdvertiseSocketAddress(new InetSocketAddress(this.getAdvertiseSocketAddress().getAddress(), advertisePort));
     }

    public void setAdvertiseInterface(String advertiseInterface) {
        try {
            this.setAdvertiseInterface(InetAddress.getByName(advertiseInterface));
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void setAdvertiseInterfaceName(String advertiseInterfaceName) {
        try {
            this.setAdvertiseInterface(NetworkInterface.getByName(advertiseInterfaceName));
        } catch (SocketException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void setProxyList(String addresses) {
        Collection<ProxyConfiguration> proxyConfigurations;
        if ((addresses == null) || (addresses.length() == 0)) {
            proxyConfigurations = Collections.emptySet();
        } else {
            String[] tokens = addresses.split(",");
            proxyConfigurations = new ArrayList<>(tokens.length);

            for (String token: tokens) {
                try {
                    final InetSocketAddress remoteAddress = Utils.parseSocketAddress(token.trim(), ModClusterService.DEFAULT_PORT);
                    proxyConfigurations.add(new ProxyConfiguration() {
                        @Override
                        public InetSocketAddress getRemoteAddress() {
                            return remoteAddress;
                        }

                        @Override
                        public InetSocketAddress getLocalAddress() {
                            return null;
                        }
                    });
                } catch (UnknownHostException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }
        this.setProxyConfigurations(proxyConfigurations);
    }

    public String getProxyList() {
        if (this.getProxyConfiguration().isEmpty()) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        for (ProxyConfiguration proxy : this.getProxyConfigurations()) {
            InetSocketAddress socketAddress = proxy.getRemoteAddress();
            if (builder.length() > 0) {
                builder.append(",");
            }
            InetAddress address = socketAddress.getAddress();
            String host = address.toString();
            int index = host.indexOf("/");
            // Prefer host name, but perform reverse DNS lookup to find it
            host = (index > 0) ? host.substring(0, index) : host.substring(1);
            if (host.contains(":")) {
                // Escape IPv6
                builder.append('[').append(host).append(']');
            } else {
                builder.append(host);
            }
            builder.append(':').append(socketAddress.getPort());
        }
        return builder.toString();
    }

    private static final String ROOT_CONTEXT = "ROOT";
    private static final String CONTEXT_DELIMITER = ",";
    private static final String HOST_CONTEXT_DELIMITER = ":";

    public void setExcludedContexts(String contexts) {
        Map<String, Set<String>> excludedContextsPerHost = Collections.emptyMap();
        if (contexts == null) {
            excludedContextsPerHost = Collections.emptyMap();
        } else {
            String trimmedContexts = contexts.trim();

            if (trimmedContexts.isEmpty()) {
                this.setExcludedContextsPerHost(Collections.<String, Set<String>>emptyMap());
            } else {
                excludedContextsPerHost = new HashMap<>();

                for (String context : trimmedContexts.split(CONTEXT_DELIMITER)) {
                    String[] parts = context.trim().split(HOST_CONTEXT_DELIMITER);

                    if (parts.length > 2) {
                        throw new IllegalArgumentException(trimmedContexts + " is not a valid value for excludedContexts");
                    }

                    String host = null;
                    String trimmedContext = parts[0].trim();

                    if (parts.length == 2) {
                        host = trimmedContext;
                        trimmedContext = parts[1].trim();
                    }

                    String path;
                    switch (trimmedContext) {
                        case "ROOT":
                            log.warn("Value 'ROOT' for excludedContexts is deprecated, to exclude the root context use '/' instead.");
                        case "/":
                            path = "";
                            break;
                        default:
                            // normalize the context by pre-pending or removing trailing slash
                            trimmedContext = trimmedContext.startsWith("/") ? trimmedContext : ("/" + trimmedContext);
                            path = trimmedContext.endsWith("/") ? trimmedContext.substring(0, trimmedContext.length() - 1) : trimmedContext;
                            break;
                    }

                    Set<String> paths = excludedContextsPerHost.get(host);

                    if (paths == null) {
                        paths = new HashSet<>();
                        excludedContextsPerHost.put(host, paths);
                    }

                    paths.add(path);
                }
            }
        }
        this.setExcludedContextsPerHost(excludedContextsPerHost);
    }

    public String getExcludedContexts() {
        if (this.getExcludedContextsPerHost() == null) return null;

        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Set<String>> entry: this.getExcludedContextsPerHost().entrySet()) {
            String host = entry.getKey();
            for (String path: entry.getValue()) {
                if (builder.length() > 0) {
                    builder.append(CONTEXT_DELIMITER);
                }
                if (host != null) {
                    builder.append(host).append(HOST_CONTEXT_DELIMITER);
                }
                builder.append(path.isEmpty() ? ROOT_CONTEXT : path.substring(1));
            }
        }
        return builder.toString();
    }

    public void setSessionDrainingStrategy(String sessionDrainingStrategy) {
        if (sessionDrainingStrategy.equalsIgnoreCase("NEVER")) {
            this.setSessionDrainingStrategy(SessionDrainingStrategyEnum.NEVER);
        } else if (sessionDrainingStrategy.equalsIgnoreCase("ALWAYS")) {
            this.setSessionDrainingStrategy(SessionDrainingStrategyEnum.ALWAYS);
        } else {
            this.setSessionDrainingStrategy(SessionDrainingStrategyEnum.DEFAULT);
        }
    }
}
