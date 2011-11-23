package org.jboss.modcluster.container.catalina;

import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Server;

public class CatalinaEngineFactory implements EngineFactory {
    @Override
    public Engine createEngine(org.apache.catalina.Engine engine, Server server) {
        return new CatalinaEngine(engine, server);
    }
}
