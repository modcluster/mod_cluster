package org.jboss.modcluster.container.jbossweb;

import java.net.InetAddress;

import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.IntrospectionUtils;
import org.jboss.modcluster.container.catalina.CatalinaConnector;

public class JBossWebConnector extends CatalinaConnector {

    public JBossWebConnector(Connector connector) {
        super(connector);
    }

    @Override
    public int getMaxThreads() {
        return (Integer) IntrospectionUtils.getProperty(this.connector.getProtocolHandler(), "maxThreads");
    }

    @Override
    public int getBusyThreads() {
        Object endpoint = this.getEndpoint();
        return (Integer) IntrospectionUtils.getProperty(endpoint, "currentThreadsBusy");
    }

    @Override
    protected Object getEndpoint() {
        return this.getProtocolHandlerProperty("endpoint");
    }

    @Override
    public void setAddress(InetAddress address) {
        IntrospectionUtils.setProperty(this.connector.getProtocolHandler(), "address", address.getHostAddress());
    }
}
