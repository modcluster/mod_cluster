package org.jboss.modcluster.container.catalina;

import org.apache.catalina.Server;

public class SimpleServerProvider implements ServerProvider {
    private final Server server;
    
    public SimpleServerProvider(Server server) {
        this.server = server;
    }

    @Override
    public Server getServer() {
        return this.server;
    }
}
