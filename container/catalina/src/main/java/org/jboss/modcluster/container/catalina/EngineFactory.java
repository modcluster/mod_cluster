package org.jboss.modcluster.container.catalina;

import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Server;

public interface EngineFactory {
    Engine createEngine(org.apache.catalina.Engine engine, Server server);
}
