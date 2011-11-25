package org.jboss.modcluster.container.catalina;

import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Host;
import org.jboss.modcluster.container.Server;

public interface CatalinaFactory {
    Server createServer(org.apache.catalina.Server server);
    
    Engine createEngine(org.apache.catalina.Engine engine);
    
    Host createHost(org.apache.catalina.Host host);
    
    Context createContext(org.apache.catalina.Context context);
}
