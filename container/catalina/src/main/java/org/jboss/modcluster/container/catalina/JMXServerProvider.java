package org.jboss.modcluster.container.catalina;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.catalina.Server;
import org.apache.catalina.Service;

public class JMXServerProvider implements ServerProvider {
    private final ObjectName name;
    private final MBeanServer mbeanServer;
    private volatile Server server;
    
    public JMXServerProvider(MBeanServer mbeanServer, ObjectName name) {
        this.mbeanServer = mbeanServer;
        this.name = name;
    }

    @Override
    public Server getServer() {
        if (this.server != null) return this.server;

        this.server = this.findServer();
        
        return this.server;
    }
    
    private Server findServer() {
        try {
            Service[] services = (Service[]) this.mbeanServer.invoke(this.name, "findServices", null, null);
            return (services.length > 0) ? services[0].getServer() : null;
        } catch (JMException e) {
            throw new IllegalStateException(e);
        }
    }
}
