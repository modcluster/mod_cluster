package org.jboss.modcluster.container.catalina;

import org.jboss.modcluster.container.Server;

public class CatalinaServerFactory implements ServerFactory {
    @Override
    public Server createServer(CatalinaFactoryRegistry registry, org.apache.catalina.Server server) {
        return new CatalinaServer(registry, server);
    }
}
