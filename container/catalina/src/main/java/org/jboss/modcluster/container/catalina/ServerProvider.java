package org.jboss.modcluster.container.catalina;

import org.apache.catalina.Server;

public interface ServerProvider {
    Server getServer();
}
