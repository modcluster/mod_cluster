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
package org.jboss.modcluster;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.jboss.modcluster.advertise.AdvertiseListener;
import org.jboss.modcluster.advertise.AdvertiseListenerFactory;
import org.jboss.modcluster.advertise.impl.AdvertiseListenerFactoryImpl;
import org.jboss.modcluster.config.BalancerConfiguration;
import org.jboss.modcluster.config.MCMPHandlerConfiguration;
import org.jboss.modcluster.config.NodeConfiguration;
import org.jboss.modcluster.config.impl.ModClusterConfig;
import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.ContainerEventHandler;
import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Host;
import org.jboss.modcluster.container.Server;
import org.jboss.modcluster.load.LoadBalanceFactorProvider;
import org.jboss.modcluster.load.LoadBalanceFactorProviderFactory;
import org.jboss.modcluster.load.SimpleLoadBalanceFactorProviderFactory;
import org.jboss.modcluster.mcmp.ContextFilter;
import org.jboss.modcluster.mcmp.MCMPConnectionListener;
import org.jboss.modcluster.mcmp.MCMPHandler;
import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPRequestFactory;
import org.jboss.modcluster.mcmp.MCMPRequestType;
import org.jboss.modcluster.mcmp.MCMPResponseParser;
import org.jboss.modcluster.mcmp.MCMPServerState;
import org.jboss.modcluster.mcmp.ResetRequestSource;
import org.jboss.modcluster.mcmp.impl.DefaultMCMPHandler;
import org.jboss.modcluster.mcmp.impl.DefaultMCMPRequestFactory;
import org.jboss.modcluster.mcmp.impl.DefaultMCMPResponseParser;
import org.jboss.modcluster.mcmp.impl.ResetRequestSourceImpl;

public class ModClusterService implements ModClusterServiceMBean, ContainerEventHandler, LoadBalanceFactorProvider,
        MCMPConnectionListener, ContextFilter {
    public static final int DEFAULT_PORT = 8000;

    private final NodeConfiguration nodeConfig;
    private final BalancerConfiguration balancerConfig;
    private final MCMPHandlerConfiguration mcmpConfig;
    private final MCMPHandler mcmpHandler;
    private final ResetRequestSource resetRequestSource;
    private final MCMPRequestFactory requestFactory;
    private final MCMPResponseParser responseParser;
    private final AdvertiseListenerFactory listenerFactory;
    private final LoadBalanceFactorProviderFactory loadBalanceFactorProviderFactory;

    private final Map<Host, Set<String>> excludedContexts = new HashMap<Host, Set<String>>();
    private final ConcurrentMap<Context, EnablableRequestListener> requestListeners = new ConcurrentHashMap<Context, EnablableRequestListener>();

    private volatile boolean established = false;
    private volatile boolean autoEnableContexts = true;
    private volatile Server server;

    private volatile LoadBalanceFactorProvider loadBalanceFactorProvider;
    private volatile AdvertiseListener advertiseListener;

    public ModClusterService(ModClusterConfig config, LoadBalanceFactorProvider loadBalanceFactorProvider) {
        this(config, new SimpleLoadBalanceFactorProviderFactory(loadBalanceFactorProvider));
    }

    public ModClusterService(ModClusterConfig config, LoadBalanceFactorProviderFactory loadBalanceFactorProviderFactory) {
        this(config, loadBalanceFactorProviderFactory, new DefaultMCMPRequestFactory());
    }

    private ModClusterService(ModClusterConfig config, LoadBalanceFactorProviderFactory loadBalanceFactorProviderFactory,
            MCMPRequestFactory requestFactory) {
        this(config, loadBalanceFactorProviderFactory, requestFactory, new DefaultMCMPResponseParser(),
                new ResetRequestSourceImpl(config, config, requestFactory));
    }

    private ModClusterService(ModClusterConfig config, LoadBalanceFactorProviderFactory loadBalanceFactorProviderFactory,
            MCMPRequestFactory requestFactory, MCMPResponseParser responseParser, ResetRequestSource resetRequestSource) {
        this(config, config, config, loadBalanceFactorProviderFactory, requestFactory, responseParser, resetRequestSource,
                new DefaultMCMPHandler(config, resetRequestSource, requestFactory, responseParser),
                new AdvertiseListenerFactoryImpl());
    }

    protected ModClusterService(NodeConfiguration nodeConfig, BalancerConfiguration balancerConfig,
            MCMPHandlerConfiguration mcmpConfig, LoadBalanceFactorProviderFactory loadBalanceFactorProviderFactory,
            MCMPRequestFactory requestFactory, MCMPResponseParser responseParser, ResetRequestSource resetRequestSource,
            MCMPHandler mcmpHandler, AdvertiseListenerFactory listenerFactory) {
        this.nodeConfig = nodeConfig;
        this.balancerConfig = balancerConfig;
        this.mcmpConfig = mcmpConfig;
        this.mcmpHandler = mcmpHandler;
        this.resetRequestSource = resetRequestSource;
        this.requestFactory = requestFactory;
        this.responseParser = responseParser;
        this.loadBalanceFactorProviderFactory = loadBalanceFactorProviderFactory;
        this.listenerFactory = listenerFactory;
    }

    /**
     * @{inheritDoc
     * @see org.jboss.modcluster.ContainerEventHandler#init(java.lang.Object)
     */
    @Override
    public synchronized void init(Server server) {
        ModClusterLogger.LOGGER.init(Version.INSTANCE.toString());

        this.server = server;

        this.mcmpHandler.init(this.mcmpConfig.getProxies(), this);

        this.autoEnableContexts = this.mcmpConfig.isAutoEnableContexts();
        this.excludedContexts.clear();

        Map<String, Set<String>> excludedContextPaths = this.mcmpConfig.getExcludedContextsPerHost();

        if (!excludedContextPaths.isEmpty()) {
            for (Engine engine : server.getEngines()) {
                String defaultHost = engine.getDefaultHost();
                for (Host host : engine.getHosts()) {
                    Set<String> excluded = new HashSet<String>();
                    Set<String> paths = excludedContextPaths.get(host.getName());
                    if (paths != null) {
                        excluded.addAll(paths);
                    }
                    if (host.getName().equals(defaultHost)) {
                        paths = excludedContextPaths.get(null);
                        if (paths != null) {
                            excluded.addAll(paths);
                        }
                    }
                    if (!excluded.isEmpty()) {
                        this.excludedContexts.put(host, Collections.unmodifiableSet(excluded));
                    }
                }
            }
        }

        this.resetRequestSource.init(server, this);

        this.loadBalanceFactorProvider = this.loadBalanceFactorProviderFactory.createLoadBalanceFactorProvider();

        Boolean advertise = this.mcmpConfig.getAdvertise();

        if (Boolean.TRUE.equals(advertise) || (advertise == null && this.mcmpConfig.getProxies().isEmpty())) {
            try {
                this.advertiseListener = this.listenerFactory.createListener(this.mcmpHandler, this.mcmpConfig);

                this.advertiseListener.start();
            } catch (IOException e) {
                // TODO What now?
                ModClusterLogger.LOGGER.advertiseStartFailed(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.modcluster.mcmp.ContextFilter#getExcludedContexts()
     */
    @Override
    public Map<Host, Set<String>> getExcludedContexts() {
        return Collections.unmodifiableMap(this.excludedContexts);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.modcluster.mcmp.ContextFilter#isAutoEnableContexts()
     */
    @Override
    public boolean isAutoEnableContexts() {
        return this.autoEnableContexts;
    }

    /**
     * @{inheritDoc
     * @see org.jboss.modcluster.ContainerEventHandler#shutdown()
     */
    @Override
    public synchronized void shutdown() {
        ModClusterLogger.LOGGER.shutdown();

        this.server = null;

        if (this.advertiseListener != null) {
            this.advertiseListener.destroy();

            this.advertiseListener = null;
        }

        this.mcmpHandler.shutdown();
    }

    /**
     * @{inheritDoc
     * @see org.jboss.modcluster.ContainerEventHandler#start(java.lang.Object)
     */
    @Override
    public void start(Server server) {
        ModClusterLogger.LOGGER.startServer();

        if (this.established) {
            for (Engine engine : server.getEngines()) {
                this.config(engine);

                for (Host host : engine.getHosts()) {
                    for (Context context : host.getContexts()) {
                        this.add(context);
                    }
                }
            }
        }
    }

    /**
     * {@inhericDoc}
     * 
     * @see org.jboss.modcluster.ContainerEventHandler#stop(org.jboss.modcluster.Server)
     */
    @Override
    public void stop(Server server) {
        ModClusterLogger.LOGGER.stopServer();

        if (this.established) {
            for (Engine engine : server.getEngines()) {
                for (Host host : engine.getHosts()) {
                    for (Context context : host.getContexts()) {
                        if (context.isStarted()) {
                            this.stop(context);
                        }

                        this.remove(context);
                    }
                }

                this.removeAll(engine);
            }
        }
    }

    /**
     * Configures the specified engine. Sends CONFIG request.
     * 
     * @param engine
     */
    protected void config(Engine engine) {
        ModClusterLogger.LOGGER.sendEngineCommand(MCMPRequestType.CONFIG, engine);

        try {
            MCMPRequest request = this.requestFactory.createConfigRequest(engine, this.nodeConfig, this.balancerConfig);

            this.mcmpHandler.sendRequest(request);
        } catch (Exception e) {
            this.mcmpHandler.markProxiesInError();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.modcluster.mcmp.MCMPConnectionListener#isEstablished()
     */
    @Override
    public boolean isEstablished() {
        return this.established;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.modcluster.mcmp.MCMPConnectionListener#connectionEstablished(java.net.InetAddress)
     */
    @Override
    public void connectionEstablished(InetAddress localAddress) {
        for (Engine engine : this.server.getEngines()) {
            Connector connector = engine.getProxyConnector();
            InetAddress address = connector.getAddress();

            // Set connector address
            if ((address == null) || address.isAnyLocalAddress()) {
                connector.setAddress(localAddress);

                ModClusterLogger.LOGGER.detectConnectorAddress(engine, localAddress);
            }

            this.establishJvmRoute(engine);
        }

        this.established = true;
    }

    protected void establishJvmRoute(Engine engine) {
        // Create default jvmRoute if none was specified
        if (engine.getJvmRoute() == null) {
            String jvmRoute = this.mcmpConfig.getJvmRouteFactory().createJvmRoute(engine);

            engine.setJvmRoute(jvmRoute);

            ModClusterLogger.LOGGER.detectJvmRoute(engine, jvmRoute);
        }
    }

    /**
     * {@inhericDoc}
     * 
     * @see org.jboss.modcluster.ContainerEventHandler#add(org.jboss.modcluster.Context)
     */
    @Override
    public void add(Context context) {
        ModClusterLogger.LOGGER.addContext(context.getHost(), context);

        if (this.include(context)) {
            // Send ENABLE-APP if state is started
            if (this.established && context.isStarted()) {
                this.enable(context);
            }
        }
    }

    /**
     * {@inhericDoc}
     * 
     * @see org.jboss.modcluster.ContainerEventHandler#start(org.jboss.modcluster.Context)
     */
    @Override
    public void start(Context context) {
        ModClusterLogger.LOGGER.startContext(context.getHost(), context);

        if (this.include(context)) {
            if (this.established) {
                this.enable(context);
            }

            EnablableRequestListener listener = new NotifyOnDestroyRequestListener();

            if (this.requestListeners.putIfAbsent(context, listener) == null) {
                context.addRequestListener(listener);
            }
        }
    }

    private void enable(Context context) {
        ModClusterLogger.LOGGER.sendContextCommand(this.autoEnableContexts ? MCMPRequestType.ENABLE_APP : MCMPRequestType.DISABLE_APP, context.getHost(), context);

        this.mcmpHandler.sendRequest(this.autoEnableContexts ? this.requestFactory.createEnableRequest(context) : this.requestFactory.createDisableRequest(context));
    }

    private void disable(Context context) {
        ModClusterLogger.LOGGER.sendContextCommand(MCMPRequestType.DISABLE_APP, context.getHost(), context);

        this.mcmpHandler.sendRequest(this.requestFactory.createDisableRequest(context));
    }

    /**
     * {@inhericDoc}
     * 
     * @see org.jboss.modcluster.ContainerEventHandler#stop(org.jboss.modcluster.Context)
     */
    @Override
    public void stop(Context context) {
        ModClusterLogger.LOGGER.stopContext(context.getHost(), context);

        if (this.established && this.include(context)) {
            this.disable(context);

            long start = System.currentTimeMillis();
            long end = start + this.mcmpConfig.getStopContextTimeoutUnit().toMillis(this.mcmpConfig.getStopContextTimeout());

            if (this.mcmpConfig.getSessionDrainingStrategy().isEnabled(context)) {
                // If the session manager is not distributed
                // we need to drain the active sessions
                // before draining pending requests.
                this.drainSessions(context, start, end);
            }

            // Drain pending requests via iterative STOP-APP commands
            this.drainRequests(context, start, end);
        }
    }

    /**
     * {@inhericDoc}
     * 
     * @see org.jboss.modcluster.ContainerEventHandler#remove(org.jboss.modcluster.Context)
     */
    @Override
    public void remove(Context context) {
        ModClusterLogger.LOGGER.removeContext(context.getHost(), context);

        if (this.include(context)) {
            if (this.established) {
                ModClusterLogger.LOGGER.sendContextCommand(MCMPRequestType.REMOVE_APP, context.getHost(), context);

                this.mcmpHandler.sendRequest(this.requestFactory.createRemoveRequest(context));
            }

            EnablableRequestListener listener = this.requestListeners.remove(context);

            if (listener != null) {
                context.removeRequestListener(listener);
            }
        }
    }

    /**
     * Sends REMOVE-APP *, if engine was initialized
     * 
     * @param engine
     */
    protected void removeAll(Engine engine) {
        ModClusterLogger.LOGGER.sendEngineCommand(MCMPRequestType.REMOVE_APP, engine);

        // Send REMOVE-APP * request
        this.mcmpHandler.sendRequest(this.requestFactory.createRemoveRequest(engine));
    }

    /**
     * {@inhericDoc}
     * 
     * @see org.jboss.modcluster.ContainerEventHandler#status(org.jboss.modcluster.Engine)
     */
    @Override
    public void status(Engine engine) {
        this.mcmpHandler.status();

        if (this.established) {
            // Send STATUS request
            Connector connector = engine.getProxyConnector();

            int lbf = (connector != null) && connector.isAvailable() ? this.getLoadBalanceFactor(engine) : -1;

            ModClusterLogger.LOGGER.sendEngineCommand(MCMPRequestType.STATUS, engine);

            this.mcmpHandler.sendRequest(this.requestFactory.createStatusRequest(engine.getJvmRoute(), lbf));
        }
    }

    private boolean include(Context context) {
        Set<String> excludedPaths = this.excludedContexts.get(context.getHost());

        return (excludedPaths == null) || !excludedPaths.contains(context.getPath());
    }

    /**
     * {@inhericDoc}
     * 
     * @see org.jboss.modcluster.load.LoadBalanceFactorProvider#getLoadBalanceFactor()
     */
    @Override
    public int getLoadBalanceFactor(Engine engine) {
        return this.loadBalanceFactorProvider.getLoadBalanceFactor(engine);
    }

    /**
     * @{inheritDoc
     * @see org.jboss.modcluster.ModClusterServiceMBean#addProxy(java.lang.String, int)
     */
    @Override
    public void addProxy(String host, int port) {
        this.mcmpHandler.addProxy(this.createSocketAddress(host, port));
    }

    /**
     * @{inheritDoc
     * @see org.jboss.modcluster.ModClusterServiceMBean#removeProxy(java.lang.String, int)
     */
    @Override
    public void removeProxy(String host, int port) {
        this.mcmpHandler.removeProxy(this.createSocketAddress(host, port));
    }

    private InetSocketAddress createSocketAddress(String host, int port) {
        try {
            return new InetSocketAddress(InetAddress.getByName(host), port);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * {@inhericDoc}
     * 
     * @see org.jboss.modcluster.ModClusterServiceMBean#getProxyConfiguration()
     */
    @Override
    public Map<InetSocketAddress, String> getProxyConfiguration() {
        // Send DUMP * request
        return this.getProxyResults(this.requestFactory.createDumpRequest());
    }

    /**
     * {@inhericDoc}
     * 
     * @see org.jboss.modcluster.ModClusterServiceMBean#getProxyInfo()
     */
    @Override
    public Map<InetSocketAddress, String> getProxyInfo() {
        // Send INFO * request
        return this.getProxyResults(this.requestFactory.createInfoRequest());
    }

    /**
     * {@inhericDoc}
     * 
     * @see org.jboss.modcluster.ModClusterServiceMBean#ping()
     */
    @Override
    public Map<InetSocketAddress, String> ping() {
        MCMPRequest request = this.requestFactory.createPingRequest();
        return this.getProxyResults(request);
    }

    /**
     * {@inhericDoc}
     * 
     * @see org.jboss.modcluster.ModClusterServiceMBean#ping(java.lang.String)
     */
    @Override
    public Map<InetSocketAddress, String> ping(String jvmRoute) {
        MCMPRequest request = this.requestFactory.createPingRequest(jvmRoute);
        return this.getProxyResults(request);
    }

    /**
     * {@inhericDoc}
     * 
     * @see org.jboss.modcluster.ModClusterServiceMBean#ping(java.lang.String, java.lang.String, int)
     */
    @Override
    public Map<InetSocketAddress, String> ping(String scheme, String host, int port) {
        MCMPRequest request = this.requestFactory.createPingRequest(scheme, host, port);
        return this.getProxyResults(request);
    }

    private Map<InetSocketAddress, String> getProxyResults(MCMPRequest request) {
        if (!this.established)
            return Collections.emptyMap();

        Map<MCMPServerState, String> responses = this.mcmpHandler.sendRequest(request);

        if (responses.isEmpty())
            return Collections.emptyMap();

        Map<InetSocketAddress, String> results = new HashMap<InetSocketAddress, String>();

        for (Map.Entry<MCMPServerState, String> response : responses.entrySet()) {
            MCMPServerState state = response.getKey();

            results.put(state.getSocketAddress(), response.getValue());
        }

        return results;
    }

    /**
     * {@inhericDoc}
     * 
     * @see org.jboss.modcluster.ModClusterServiceMBean#reset()
     */
    @Override
    public void reset() {
        if (this.established) {
            this.mcmpHandler.reset();
        }
    }

    /**
     * @{inheritDoc
     * @see org.jboss.modcluster.ModClusterServiceMBean#refresh()
     */
    @Override
    public void refresh() {
        if (this.established) {
            // Set as error, and the periodic event will refresh the configuration
            this.mcmpHandler.markProxiesInError();
        }
    }

    /**
     * @{inheritDoc
     * @see org.jboss.modcluster.ModClusterServiceMBean#disable()
     */
    @Override
    public boolean disable() {
        if (!this.established)
            return false;

        for (Engine engine : this.server.getEngines()) {
            // Send DISABLE-APP * request
            this.mcmpHandler.sendRequest(this.requestFactory.createDisableRequest(engine));
        }

        return this.mcmpHandler.isProxyHealthOK();
    }

    /**
     * @{inheritDoc
     * @see org.jboss.modcluster.ModClusterServiceMBean#enable()
     */
    @Override
    public boolean enable() {
        if (!this.established)
            return false;

        for (Engine engine : this.server.getEngines()) {
            // Send ENABLE-APP * request
            this.mcmpHandler.sendRequest(this.requestFactory.createEnableRequest(engine));
        }

        this.autoEnableContexts = true;

        return this.mcmpHandler.isProxyHealthOK();
    }

    /**
     * @{inheritDoc
     * @see org.jboss.modcluster.ModClusterServiceMBean#disable(java.lang.String, java.lang.String)
     */
    @Override
    public boolean disableContext(String host, String path) {
        if (!this.established)
            return false;

        Context context = this.findContext(this.findHost(host), path);

        // Send DISABLE-APP /... request
        this.mcmpHandler.sendRequest(this.requestFactory.createDisableRequest(context));

        return this.mcmpHandler.isProxyHealthOK();
    }

    /**
     * @{inheritDoc
     * @see org.jboss.modcluster.ModClusterServiceMBean#enable(java.lang.String, java.lang.String)
     */
    @Override
    public boolean enableContext(String host, String path) {
        if (!this.established)
            return false;

        Context context = this.findContext(this.findHost(host), path);

        // Send ENABLE-APP /... request
        this.mcmpHandler.sendRequest(this.requestFactory.createEnableRequest(context));

        return this.mcmpHandler.isProxyHealthOK();
    }

    /**
     * {@inhericDoc}
     * 
     * @see org.jboss.modcluster.ModClusterServiceMBean#stop(long, java.util.concurrent.TimeUnit)
     */
    @Override
    public boolean stop(long timeout, TimeUnit unit) {
        if (!this.established)
            return false;

        // Send DISABLE-APP * requests
        for (Engine engine : this.server.getEngines()) {
            this.mcmpHandler.sendRequest(this.requestFactory.createDisableRequest(engine));
        }

        long start = System.currentTimeMillis();
        long end = start + unit.toMillis(timeout);

        for (Engine engine : this.server.getEngines()) {
            for (Host host : engine.getHosts()) {
                for (Context context : host.getContexts()) {
                    if (this.mcmpConfig.getSessionDrainingStrategy().isEnabled(context) && !this.drainSessions(context, start, end)) {
                        return false;
                    }
                }
            }
        }

        // Send STOP-APP * requests
        for (Engine engine : this.server.getEngines()) {
            this.mcmpHandler.sendRequest(this.requestFactory.createStopRequest(engine));
        }

        return true;
    }

    /**
     * {@inhericDoc}
     * 
     * @see org.jboss.modcluster.ModClusterServiceMBean#stopContext(java.lang.String, java.lang.String, long,
     *      java.util.concurrent.TimeUnit)
     */
    @Override
    public boolean stopContext(String host, String path, long timeout, TimeUnit unit) {
        if (!this.established)
            return false;

        Context context = this.findContext(this.findHost(host), path);

        this.disable(context);

        long start = System.currentTimeMillis();

        boolean success = true;

        if (this.mcmpConfig.getSessionDrainingStrategy().isEnabled(context)) {
            success = this.drainSessions(context, start, start + unit.toMillis(timeout));
        }

        if (success) {
            this.mcmpHandler.sendRequest(this.requestFactory.createStopRequest(context));
        }

        return success;
    }

    /*
     * Sends STOP-APP requests for the specified context until there are no more pending requests, or until the specified
     * timeout is met. Returns true, if there are no more pending requests, false otherwise.
     */
    private <M> boolean drainRequests(Context context, long start, long end) {
        EnablableRequestListener listener = this.requestListeners.get(context);

        boolean noTimeout = (start >= end);

        MCMPRequest request = this.requestFactory.createStopRequest(context);

        if (listener == null) {
            // Just send a STOP (for example with TC6 we don't have a listener)
            int requests = this.stop(request);
            return (requests == 0);
        }

        synchronized (listener) {
            listener.setEnabled(true);

            try {
                long current = System.currentTimeMillis();
                long timeout = end - current;

                int requests = this.stop(request);

                while ((requests > 0) && (noTimeout || (timeout > 0))) {
                    ModClusterLogger.LOGGER.drainRequests(requests, context.getHost(), context);

                    // Wait to be notified of a destroyed request
                    listener.wait(noTimeout ? 0 : timeout);

                    current = System.currentTimeMillis();
                    timeout = end - current;

                    requests = this.stop(request);
                }

                boolean success = (requests == 0);
                float duration = ((success ? System.currentTimeMillis() : end) - start) / 1000f;

                if (success) {
                    ModClusterLogger.LOGGER.requestsDrained(context.getHost(), context, duration);
                } else {
                    ModClusterLogger.LOGGER.requestDrainTimeout(requests, context.getHost(), context, duration);
                }

                return success;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } finally {
                listener.setEnabled(false);
            }
        }
    }

    /*
     * Sends the specified stop request, parses and totals the responses.
     */
    private int stop(MCMPRequest request) {
        Map<MCMPServerState, String> responses = this.mcmpHandler.sendRequest(request);

        int requests = 0;

        for (String response : responses.values()) {
            requests += this.responseParser.parseStopAppResponse(response);
        }

        return requests;
    }

    /*
     * Returns true, when the active session count reaches 0; or false, after timeout.
     */
    private boolean drainSessions(Context context, long start, long end) {
        int remainingSessions = context.getActiveSessionCount();

        // Short circuit if there are already no sessions
        if (remainingSessions == 0)
            return true;

        // Notify the user that the server is draining sessions since it might appear stuck since messages while draining are on DEBUG
        ModClusterLogger.LOGGER.startSessionDraining(remainingSessions, context.getHost(), context, TimeUnit.MILLISECONDS.toSeconds(end - start));

        boolean noTimeout = (start >= end);

        HttpSessionListener listener = new NotifyOnDestroySessionListener();

        try {
            synchronized (listener) {
                context.addSessionListener(listener);

                long current = System.currentTimeMillis();
                long timeout = end - current;

                remainingSessions = context.getActiveSessionCount();

                while ((remainingSessions > 0) && (noTimeout || (timeout > 0))) {
                    ModClusterLogger.LOGGER.drainSessions(remainingSessions, context.getHost(), context);

                    // Poll active sessions every second since since right after the notify, the session manager implementation
                    // will still account for that last session.
                    listener.wait(noTimeout ? 0 : Math.min(timeout, 1000));

                    current = System.currentTimeMillis();
                    timeout = end - current;
                    remainingSessions = context.getActiveSessionCount();
                }
            }

            boolean success = (remainingSessions == 0);
            float duration = ((success ? System.currentTimeMillis() : end) - start) / 1000f;

            if (success) {
                ModClusterLogger.LOGGER.sessionsDrained(context.getHost(), context, duration);
            } else {
                ModClusterLogger.LOGGER.sessionDrainTimeout(remainingSessions, context.getHost(), context, duration);
            }

            return success;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            context.removeSessionListener(listener);
        }
    }

    private Host findHost(String name) {
        for (Engine engine : this.server.getEngines()) {
            Host host = engine.findHost(name);

            if (host != null) return host;
        }

        throw ModClusterMessages.MESSAGES.hostNotFound(name);
    }

    private Context findContext(Host host, String path) {
        Context context = host.findContext(path);

        if (context == null) {
            throw ModClusterMessages.MESSAGES.contextNotFound(path, host);
        }

        return context;
    }

    interface Enablable {
        boolean isEnabled();

        void setEnabled(boolean enabled);
    }

    interface EnablableRequestListener extends Enablable, ServletRequestListener {
    }

    static class NotifyOnDestroyRequestListener implements EnablableRequestListener {
        private volatile boolean enabled = false;

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.modcluster.ModClusterService.Controllable#start()
         */
        @Override
        public boolean isEnabled() {
            return this.enabled;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.modcluster.ModClusterService.Controllable#stop()
         */
        @Override
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.servlet.ServletRequestListener#requestInitialized(javax.servlet.ServletRequestEvent)
         */
        @Override
        public void requestInitialized(ServletRequestEvent event) {
            // Do nothing
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.servlet.ServletRequestListener#requestDestroyed(javax.servlet.ServletRequestEvent)
         */
        @Override
        public void requestDestroyed(ServletRequestEvent event) {
            if (this.enabled) {
                // Notify waiting threads, but only if enabled
                synchronized (this) {
                    this.notify();
                }
            }
        }
    };

    static class NotifyOnDestroySessionListener implements HttpSessionListener {
        /**
         * {@inheritDoc}
         * 
         * @see javax.servlet.http.HttpSessionListener#sessionCreated(javax.servlet.http.HttpSessionEvent)
         */
        @Override
        public void sessionCreated(HttpSessionEvent event) {
            // Do nothing
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.servlet.http.HttpSessionListener#sessionDestroyed(javax.servlet.http.HttpSessionEvent)
         */
        @Override
        public void sessionDestroyed(HttpSessionEvent event) {
            synchronized (this) {
                this.notify();
            }
        }
    };
}
