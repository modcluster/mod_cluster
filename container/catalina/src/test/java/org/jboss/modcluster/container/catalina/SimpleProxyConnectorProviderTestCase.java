package org.jboss.modcluster.container.catalina;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.catalina.Engine;
import org.jboss.modcluster.container.Connector;
import org.junit.Test;

public class SimpleProxyConnectorProviderTestCase {
    @Test
    public void createProxyConnector() throws Exception {
        ConnectorFactory factory = mock(ConnectorFactory.class);
        Engine engine = mock(Engine.class);
        Connector expected = mock(Connector.class);
        
        org.apache.catalina.connector.Connector connector = new org.apache.catalina.connector.Connector("AJP/1.3");
        
        when(factory.createConnector(same(connector))).thenReturn(expected);
        
        Connector result = new SimpleProxyConnectorProvider(connector).createProxyConnector(factory, engine);

        assertSame(expected, result);
    }
}
