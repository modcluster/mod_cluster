package org.jboss.modcluster.container.catalina;

import java.util.ServiceLoader;

import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Host;
import org.jboss.modcluster.container.Server;

public class ServiceLoaderCatalinaFactory implements CatalinaFactory, CatalinaFactoryRegistry {
    private final ServerFactory serverFactory;
    private final EngineFactory engineFactory;
    private final HostFactory hostFactory;
    private final ContextFactory contextFactory;
    private final ConnectorFactory connectorFactory;
    
    private static <T> T load(Class<T> targetClass, Class<? extends T> defaultClass) {
        for (T value : ServiceLoader.load(targetClass, targetClass.getClassLoader())) {
            return value;
        }
        try {
            return defaultClass.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
    
    public ServiceLoaderCatalinaFactory() {
        this.serverFactory = load(ServerFactory.class, CatalinaServerFactory.class);
        this.engineFactory = load(EngineFactory.class, CatalinaEngineFactory.class);
        this.hostFactory = load(HostFactory.class, CatalinaHostFactory.class);
        this.contextFactory = load(ContextFactory.class, CatalinaContextFactory.class);
        this.connectorFactory = load(ConnectorFactory.class, CatalinaConnectorFactory.class);
    }
    
    public ServiceLoaderCatalinaFactory(ServerFactory serverFactory, EngineFactory engineFactory, HostFactory hostFactory, ContextFactory contextFactory, ConnectorFactory connectorFactory) {
        this.serverFactory = serverFactory;
        this.engineFactory = engineFactory;
        this.hostFactory = hostFactory;
        this.contextFactory = contextFactory;
        this.connectorFactory = connectorFactory;
    }
    
    @Override
    public Server createServer(org.apache.catalina.Server server) {
        return this.serverFactory.createServer(this, server);
    }
    
    @Override
    public Engine createEngine(org.apache.catalina.Engine engine) {
        return this.engineFactory.createEngine(this, engine, this.createServer(engine.getService().getServer()));
    }
    
    @Override
    public Host createHost(org.apache.catalina.Host host) {
        return this.hostFactory.createHost(this, host, this.createEngine((org.apache.catalina.Engine) host.getParent()));
    }
    
    @Override
    public Context createContext(org.apache.catalina.Context context) {
        return this.contextFactory.createContext(context, this.createHost((org.apache.catalina.Host) context.getParent()));
    }

    @Override
    public ServerFactory getServerFactory() {
        return this.serverFactory;
    }

    @Override
    public EngineFactory getEngineFactory() {
        return this.engineFactory;
    }

    @Override
    public HostFactory getHostFactory() {
        return this.hostFactory;
    }

    @Override
    public ContextFactory getContextFactory() {
        return this.contextFactory;
    }

    @Override
    public ConnectorFactory getConnectorFactory() {
        return this.connectorFactory;
    }
}
