package org.jboss.modcluster.mcmp;

import java.net.InetAddress;

public interface MCMPConnectionListener {
    void connectionEstablished(InetAddress localAddress);

    boolean isEstablished();
}
