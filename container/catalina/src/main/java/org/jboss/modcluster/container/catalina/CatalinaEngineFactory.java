package org.jboss.modcluster.container.catalina;

import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Server;

public class CatalinaEngineFactory implements EngineFactory {
    @Override
    public Engine createEngine(CatalinaFactoryRegistry registry, org.apache.catalina.Engine engine, Server server) {
        return new CatalinaEngine(registry, engine, server);
    }
}
