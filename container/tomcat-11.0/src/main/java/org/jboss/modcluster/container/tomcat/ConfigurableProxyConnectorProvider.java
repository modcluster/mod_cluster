/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.container.tomcat;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.catalina.Engine;
import org.jboss.modcluster.ModClusterLogger;
import org.jboss.modcluster.container.Connector;

/**
 * Connector provider implementation that picks a connector based on configured {@code connectorPort=".."} and/or {@code connectorAddress=".."}.
 * Should multiple connectors match a {@link RuntimeException} is thrown.
 * If no connectors match for the given engine, {@code null} is returned.
 *
 * @author Radoslav Husar
 */
public class ConfigurableProxyConnectorProvider implements ProxyConnectorProvider {

    private final TomcatConnectorConfiguration connectorConfiguration;

    public ConfigurableProxyConnectorProvider(TomcatConnectorConfiguration connectorConfiguration) {
        this.connectorConfiguration = connectorConfiguration;
    }

    @Override
    public Connector createProxyConnector(Engine engine) {
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
            return null;
        }

        String externalConnectorAddress = connectorConfiguration.getExternalConnectorAddress();
        Integer externalConnectorPort = connectorConfiguration.getExternalConnectorPort();

        if (externalConnectorAddress == null && externalConnectorPort == null) {
            return new TomcatConnector(candidate);
        } else {
            return new TomcatConnector(candidate, externalConnectorAddress, externalConnectorPort);
        }
    }

    private static String format(String connectorAddress, Integer connectorPort) {
        return (connectorAddress == null ? "*" : connectorAddress) + ":" + (connectorPort == null ? "*" : connectorPort);
    }
}
