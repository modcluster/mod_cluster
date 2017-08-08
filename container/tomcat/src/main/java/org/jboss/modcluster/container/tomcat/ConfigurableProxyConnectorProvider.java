/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
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

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.catalina.Engine;
import org.jboss.modcluster.ModClusterLogger;
import org.jboss.modcluster.container.Connector;

/**
 * Connector provider implementation that picks a connector based on configured {@code connectorPort=".."} and/or {@code connectorAddress=".."}.
 * Should multiple or no connectors match the defined values a {@link RuntimeException} is thrown.
 *
 * @author Radoslav Husar
 */
public class ConfigurableProxyConnectorProvider implements ProxyConnectorProvider {

    private final TomcatConnectorConfiguration connectorConfiguration;

    public ConfigurableProxyConnectorProvider(TomcatConnectorConfiguration connectorConfiguration) {
        this.connectorConfiguration = connectorConfiguration;
    }

    @Override
    public Connector createProxyConnector(ConnectorFactory factory, Engine engine) {
        // Resolve configuration parameters *after* it was set by Tomcat modeler
        String connectorAddress = connectorConfiguration.getConnectorAddress();
        Integer connectorPort = connectorConfiguration.getConnectorPort();

        // Iterate entire collection of connectors once, ensuring that exactly only *one* matches the given host:port configuration
        org.apache.catalina.connector.Connector candidate = null;

        for (org.apache.catalina.connector.Connector connector : engine.getService().findConnectors()) {
            if (connectorPort == null || connectorPort.equals(connector.getPort())) {

                String resolvedConfiguredAddress = null;
                String addressInTomcat = null;

                if (connector != null) {
                    // Resolve the connector's address in use
                    Object addressObj = connector.getProperty("address");
                    if (addressObj instanceof InetAddress) {
                        addressInTomcat = ((InetAddress) addressObj).getHostAddress();
                    } else if (addressObj != null) {
                        addressInTomcat = addressObj.toString();
                    }

                    // Resolve configured address
                    try {
                        resolvedConfiguredAddress = InetAddress.getByName(connectorAddress).getHostAddress();
                    } catch (UnknownHostException e) {
                        throw ModClusterLogger.LOGGER.connectorAddressUnknownHost(connectorAddress);
                    }
                }

                if (connectorAddress == null || resolvedConfiguredAddress.equalsIgnoreCase(addressInTomcat)) {
                    if (candidate == null) {
                        candidate = connector;
                    } else {
                        throw ModClusterLogger.LOGGER.connectorMatchesMultiple(format(connectorAddress, connectorPort));
                    }
                }

            }
        }

        if (candidate == null) {
            throw ModClusterLogger.LOGGER.connectorNoMatch(format(connectorAddress, connectorPort));
        }

        return factory.createConnector(candidate);
    }

    private static String format(String connectorAddress, Integer connectorPort) {
        return (connectorAddress == null ? "*" : connectorAddress) + ":" + (connectorPort == null ? "*" : connectorPort);
    }
}
