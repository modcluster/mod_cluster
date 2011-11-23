package org.jboss.modcluster.container.catalina;

import org.jboss.modcluster.container.Connector;

public class CatalinaConnectorFactory implements ConnectorFactory {
    @Override
    public Connector createConnector(org.apache.catalina.connector.Connector connector) {
        return new CatalinaConnector(connector);
    }
}
