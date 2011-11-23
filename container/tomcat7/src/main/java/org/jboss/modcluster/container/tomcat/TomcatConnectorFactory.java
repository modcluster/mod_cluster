package org.jboss.modcluster.container.tomcat;

import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.catalina.ConnectorFactory;

public class TomcatConnectorFactory implements ConnectorFactory {
    @Override
    public Connector createConnector(org.apache.catalina.connector.Connector connector) {
        return new TomcatConnector(connector);
    }
}
