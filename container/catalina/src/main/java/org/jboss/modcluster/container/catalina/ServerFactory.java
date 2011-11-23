package org.jboss.modcluster.container.catalina;

import javax.management.MBeanServer;

import org.jboss.modcluster.container.Server;

public interface ServerFactory {
   Server createServer(org.apache.catalina.Server server, MBeanServer mbeanServer);
}
