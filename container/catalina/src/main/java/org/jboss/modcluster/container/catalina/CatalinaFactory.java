package org.jboss.modcluster.container.catalina;

import java.util.ServiceLoader;

import javax.management.MBeanServer;

import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Host;
import org.jboss.modcluster.container.Server;

public class CatalinaFactory {
    public static final ServerFactory SERVER_FACTORY = load(ServerFactory.class, new CatalinaServerFactory());
    public static final EngineFactory ENGINE_FACTORY = load(EngineFactory.class, new CatalinaEngineFactory());
    public static final HostFactory HOST_FACTORY = load(HostFactory.class, new CatalinaHostFactory());
    public static final ContextFactory CONTEXT_FACTORY = load(ContextFactory.class, new CatalinaContextFactory());
    public static final ConnectorFactory CONNECTOR_FACTORY = load(ConnectorFactory.class, new CatalinaConnectorFactory());

    private static <T> T load(Class<T> targetClass, T defaultValue) {
        for (T object : ServiceLoader.load(targetClass, targetClass.getClassLoader())) {
            return object;
        }
        return defaultValue;
    }
    
    private final MBeanServer server;
    
    public CatalinaFactory(MBeanServer server) {
        this.server = server;
    }
    
    public Server createServer(org.apache.catalina.Server server) {
        return CatalinaFactory.SERVER_FACTORY.createServer(server, this.server);
    }
    
    public Engine createEngine(org.apache.catalina.Engine engine) {
        return CatalinaFactory.ENGINE_FACTORY.createEngine(engine, this.createServer(engine.getService().getServer()));
    }
    
    public Host createHost(org.apache.catalina.Host host) {
        return CatalinaFactory.HOST_FACTORY.createHost(host, this.createEngine((org.apache.catalina.Engine) host.getParent()));
    }
    
    public Context createContext(org.apache.catalina.Context context) {
        return CatalinaFactory.CONTEXT_FACTORY.createContext(context, this.createHost((org.apache.catalina.Host) context.getParent()));
    }
}
