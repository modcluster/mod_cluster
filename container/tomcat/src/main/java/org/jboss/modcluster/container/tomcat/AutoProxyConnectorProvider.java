/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.modcluster.container.tomcat;

import org.apache.catalina.Engine;
import org.jboss.modcluster.container.Connector;

/**
 * Proxy connector provider that prefers an AJP/reverse connector;
 * or, if none are present, chooses the connector with the highest thread capacity.
 * @author Paul Ferraro
 */
public class AutoProxyConnectorProvider implements ProxyConnectorProvider {
    @Override
    public Connector createProxyConnector(ConnectorFactory factory, Engine engine) {
        int highestMaxThreads = 0;
        Connector bestConnector = null;

        for (org.apache.catalina.connector.Connector connector : engine.getService().findConnectors()) {
            Connector catalinaConnector = factory.createConnector(connector);

            if (TomcatConnector.isAJP(connector) || catalinaConnector.isReverse()) {
                return catalinaConnector;
            }

            int maxThreads = catalinaConnector.getMaxThreads();

            if (maxThreads > highestMaxThreads) {
                highestMaxThreads = maxThreads;
                bestConnector = catalinaConnector;
            }
        }

        if (bestConnector == null) {
            throw new IllegalStateException();
        }

        return bestConnector;
    }
}
