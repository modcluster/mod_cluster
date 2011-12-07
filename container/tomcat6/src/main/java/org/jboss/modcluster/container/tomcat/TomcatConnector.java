package org.jboss.modcluster.container.tomcat;

import java.net.InetAddress;

import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.IntrospectionUtils;
import org.jboss.modcluster.container.catalina.CatalinaConnector;

public class TomcatConnector extends CatalinaConnector {

    public TomcatConnector(Connector connector) {
        super(connector);
    }

    @Override
    public int getMaxThreads() {
        return (Integer) IntrospectionUtils.getProperty(this.connector.getProtocolHandler(), "maxThreads");
    }

    @Override
    public void setAddress(InetAddress address) {
        IntrospectionUtils.setProperty(this.connector.getProtocolHandler(), "address", address.getHostAddress());
    }
}
