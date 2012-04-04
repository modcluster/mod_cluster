package org.jboss.modcluster.container.catalina;


public interface CatalinaFactoryRegistry {
    ServerFactory getServerFactory();

    EngineFactory getEngineFactory();

    HostFactory getHostFactory();

    ContextFactory getContextFactory();

    ConnectorFactory getConnectorFactory();

    ProxyConnectorProvider getProxyConnectorProvider();
}
