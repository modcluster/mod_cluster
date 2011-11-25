package org.jboss.modcluster.container.catalina;

import org.jboss.modcluster.container.Server;

public interface ServerFactory {
   Server createServer(CatalinaFactoryRegistry registry, org.apache.catalina.Server server);
}
