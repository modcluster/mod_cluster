/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.container.tomcat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.catalina.Engine;
import org.apache.catalina.Service;
import org.jboss.modcluster.container.Connector;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link ConfigurableProxyConnectorProvider}.
 *
 * @author Radoslav Husar
 */
class ConfigurableProxyConnectorProviderTestCase {

    @Test
    void createProxyConnector() throws Exception {
        Engine engine = mock(Engine.class);
        Service service = mock(Service.class);

        org.apache.catalina.connector.Connector connector = new org.apache.catalina.connector.Connector("AJP/1.3");
        connector.setPort(8009);

        Connector expected = new TomcatConnector(connector);

        when(engine.getService()).thenReturn(service);
        when(service.findConnectors()).thenReturn(new org.apache.catalina.connector.Connector[] { connector });

        TomcatConnectorConfiguration config = mock(TomcatConnectorConfiguration.class);
        when(config.getConnectorAddress()).thenReturn(null);
        when(config.getConnectorPort()).thenReturn(8009);

        Connector result = new ConfigurableProxyConnectorProvider(config).createProxyConnector(engine);

        assertEquals(expected, result);
    }
}
