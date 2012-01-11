package org.jboss.modcluster.container.tomcat;

import org.apache.catalina.connector.Connector;
import org.jboss.modcluster.container.catalina.CatalinaConnector;

public class TomcatConnector extends CatalinaConnector {

    public TomcatConnector(Connector connector) {
        super(connector);
    }

    @Override
    public boolean isAvailable() {
        return this.connector.isAvailable();
    }
}
