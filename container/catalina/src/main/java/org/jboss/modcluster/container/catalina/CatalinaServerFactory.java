package org.jboss.modcluster.container.catalina;

import javax.management.MBeanServer;

import org.jboss.modcluster.container.Server;

public class CatalinaServerFactory implements ServerFactory {
    @Override
    public Server createServer(org.apache.catalina.Server server, MBeanServer mbeanServer) {
        return new CatalinaServer(server, mbeanServer);
    }
}
