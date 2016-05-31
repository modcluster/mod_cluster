package org.jboss.modcluster.container.tomcat;

import org.jboss.modcluster.container.Server;
import org.jboss.modcluster.container.catalina.CatalinaFactoryRegistry;
import org.jboss.modcluster.container.catalina.ServerFactory;

/**
 * @author Paul Ferraro
 * @author Radoslav Husar
 * @version May 2016
 */
public class TomcatServerFactory implements ServerFactory {
    @Override
    public Server createServer(CatalinaFactoryRegistry registry, org.apache.catalina.Server server) {
        return new TomcatServer(registry, server);
    }
}
