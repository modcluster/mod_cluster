package org.jboss.modcluster.container.jbossweb;

import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.catalina.ConnectorFactory;

public class JBossWebConnectorFactory implements ConnectorFactory {

    @Override
    public Connector createConnector(org.apache.catalina.connector.Connector connector) {
        return new JBossWebConnector(connector);
    }
}
