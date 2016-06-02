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
import org.jboss.modcluster.config.JvmRouteFactory;
import org.jboss.modcluster.config.impl.ModClusterConfig;
import org.jboss.modcluster.load.LoadBalanceFactorProvider;
import org.jboss.modcluster.load.LoadBalanceFactorProviderFactory;
import org.jboss.modcluster.load.impl.DynamicLoadBalanceFactorProvider;
import org.jboss.modcluster.load.metric.LoadMetric;
import org.jboss.modcluster.load.metric.impl.BusyConnectorsLoadMetric;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;

/**
 * Non-clustered mod_cluster lifecycle listener for use in JBoss Web standalone and Tomcat.
 * 
 * @author Paul Ferraro
 */
public class ModClusterListener extends ModClusterConfig implements LifecycleListener, LoadBalanceFactorProviderFactory, ModClusterServiceMBean {
    private static final Logger log = Logger.getLogger(ModClusterListener.class);

    private final ModClusterServiceMBean service;
    private final LifecycleListener listener;

    Class<? extends LoadMetric> loadMetricClass = BusyConnectorsLoadMetric.class;
    private int decayFactor = DynamicLoadBalanceFactorProvider.DEFAULT_DECAY_FACTOR;
    private int history = DynamicLoadBalanceFactorProvider.DEFAULT_HISTORY;
    private double capacity = LoadMetric.DEFAULT_CAPACITY;

    /**
     * Constructs a new ModClusterListener
     */
    public ModClusterListener() {
        ModClusterService service = new ModClusterService(this, this);

        this.service = service;
        this.listener = this.loadFactory().createListener(service);
    }

    private LifecycleListenerFactory loadFactory() {
        PrivilegedAction<LifecycleListenerFactory> action = new PrivilegedAction<LifecycleListenerFactory>() {
            @Override
            public LifecycleListenerFactory run() {
                for (LifecycleListenerFactory factory: ServiceLoader.load(LifecycleListenerFactory.class, LifecycleListenerFactory.class.getClassLoader())) {
                    return factory;
                }
                throw new ServiceConfigurationError(LifecycleListenerFactory.class.getName());
            }
        };
        return AccessController.doPrivileged(action);
    }

    protected ModClusterListener(ModClusterServiceMBean mbean, LifecycleListener listener) {
        this.service = mbean;
        this.listener = listener;
    }

    /**
     * @{inheritDoc
     * @see LoadBalanceFactorProviderFactory#createLoadBalanceFactorProvider()
     */
    @Override
    public LoadBalanceFactorProvider createLoadBalanceFactorProvider() {
        PrivilegedAction<LoadMetric> action = new PrivilegedAction<LoadMetric>() {
            @Override
            public LoadMetric run() {
                try {
                    return ModClusterListener.this.loadMetricClass.newInstance();
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException(e);
                } catch (InstantiationException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        };
        LoadMetric metric = AccessController.doPrivileged(action);

        metric.setCapacity(this.capacity);

        DynamicLoadBalanceFactorProvider provider = new DynamicLoadBalanceFactorProvider(Collections.singleton(metric));

        provider.setDecayFactor(this.decayFactor);
        provider.setHistory(this.history);

        return provider;
    }

    /**
     * {@inheritDoc}
     *
     * @see LifecycleListener#lifecycleEvent(LifecycleEvent)
     */
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

                    Registry.getRegistry(null, null).registerComponent(this, name, null);
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
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException(e);
                } catch (InstantiationException e) {
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
     * @throws ClassNotFoundException
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
    public int getLoadDecayFactor() {
        return this.decayFactor;
    }

    /**
     * Sets the factor by which the contribution of historical load values to the load factor calculation should exponentially
     * decay.
     *
     * @param decayFactor a positive number
     */
    public void setLoadDecayFactor(int decayFactor) {
        this.decayFactor = decayFactor;
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

    /**
     * @{inheritDoc
     * @see org.jboss.modcluster.config.LoadMetricConfiguration#getLoadMetricCapacity()
     */
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

    /**
     * {@inhericDoc}
     *
     * @see ModClusterServiceMBean#addProxy(String, int)
     */
    @Override
    public void addProxy(String host, int port) {
        this.service.addProxy(host, port);
    }

    /**
     * {@inhericDoc}
     *
     * @see ModClusterServiceMBean#disable()
     */
    @Override
    public boolean disable() {
        return this.service.disable();
    }

    /**
     * {@inhericDoc}
     *
     * @see ModClusterServiceMBean#disableContext(String, String)
     */
    @Override
    public boolean disableContext(String hostName, String contextPath) {
        return this.service.disableContext(hostName, contextPath);
    }

    /**
     * {@inhericDoc}
     *
     * @see ModClusterServiceMBean#ping()
     */
    @Override
    public Map<InetSocketAddress, String> ping() {
        return this.service.ping();
    }

    /**
     * {@inhericDoc}
     *
     * @see ModClusterServiceMBean#ping(String)
     */
    @Override
    public Map<InetSocketAddress, String> ping(String jvmRoute) {
        return this.service.ping(jvmRoute);
    }

    /**
     * {@inhericDoc}
     *
     * @see ModClusterServiceMBean#ping(String, String, int)
     */
    @Override
    public Map<InetSocketAddress, String> ping(String scheme, String host, int port) {
        return this.service.ping(scheme, host, port);
    }

    /**
     * {@inhericDoc}
     *
     * @see ModClusterServiceMBean#enable()
     */
    @Override
    public boolean enable() {
        return this.service.enable();
    }

    /**
     * {@inhericDoc}
     *
     * @see ModClusterServiceMBean#enableContext(String, String)
     */
    @Override
    public boolean enableContext(String hostName, String contextPath) {
        return this.service.enableContext(hostName, contextPath);
    }

    /**
     * {@inhericDoc}
     *
     * @see ModClusterServiceMBean#getProxyConfiguration()
     */
    @Override
    public Map<InetSocketAddress, String> getProxyConfiguration() {
        return this.service.getProxyConfiguration();
    }
    public String getProxyConfigurationString() {
        String result = null;

        Map<InetSocketAddress, String> map = this.service.getProxyConfiguration();
        if (map.isEmpty())
                return null;
        Object results[] = map.values().toArray();
        result = (String) results[0];
        return result;
    }

    /**
     * {@inhericDoc}
     *
     * @see ModClusterServiceMBean#getProxyInfo()
     */
    @Override
    public Map<InetSocketAddress, String> getProxyInfo() {
        return this.service.getProxyInfo();
    }
    public String getProxyInfoString() {
        String result = null;

        Map<InetSocketAddress, String> map = this.service.getProxyInfo();
        if (map.isEmpty())
                return null;
        Object results[] = map.values().toArray();
        result = (String) results[0];
        return result;
    }

    /**
     * {@inhericDoc}
     *
     * @see ModClusterServiceMBean#refresh()
     */
    @Override
    public void refresh() {
        this.service.refresh();
    }

    /**
     * {@inhericDoc}
     *
     * @see ModClusterServiceMBean#removeProxy(String, int)
     */
    @Override
    public void removeProxy(String host, int port) {
        this.service.removeProxy(host, port);
    }

    /**
     * {@inhericDoc}
     *
     * @see ModClusterServiceMBean#reset()
     */
    @Override
    public void reset() {
        this.service.reset();
    }

    /**
     * {@inhericDoc}
     *
     * @see ModClusterServiceMBean#stop(long, TimeUnit)
     */
    @Override
    public boolean stop(long timeout, TimeUnit unit) {
        return this.service.stop(timeout, unit);
    }
    public boolean stop(long timeout) {
        return this.service.stop(timeout, TimeUnit.SECONDS);
    }

    /**
     * {@inhericDoc}
     *
     * @see ModClusterServiceMBean#stopContext(String, String, long,
     *      TimeUnit)
     */
    @Override
    public boolean stopContext(String host, String path, long timeout, TimeUnit unit) {
        return this.service.stopContext(host, path, timeout, unit);
    }

    public boolean stopContext(String host, String path, long timeout) {
        return this.service.stopContext(host, path, timeout, TimeUnit.SECONDS);
    }
}
