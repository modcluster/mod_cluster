package org.jboss.modcluster.container.catalina;

import org.jboss.modcluster.container.Connector;

public interface ConnectorFactory {
    Connector createConnector(org.apache.catalina.connector.Connector connector);
}
