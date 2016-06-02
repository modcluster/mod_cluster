package org.jboss.modcluster.container.catalina;

import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.catalina.Engine;
import org.apache.catalina.Service;
import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.tomcat.AutoProxyConnectorProvider;
import org.jboss.modcluster.container.tomcat.ConnectorFactory;
import org.junit.Test;

public class AutoProxyConnectorProviderTestCase {
    @Test
    public void createProxyConnector() throws Exception {
        ConnectorFactory factory = mock(ConnectorFactory.class);
        Engine engine = mock(Engine.class);
        Service service = mock(Service.class);
        Connector expected = mock(Connector.class);
        
        org.apache.catalina.connector.Connector connector = new org.apache.catalina.connector.Connector("AJP/1.3");
        
        when(engine.getService()).thenReturn(service);
        when(service.findConnectors()).thenReturn(new org.apache.catalina.connector.Connector[] { connector });
        when(factory.createConnector(same(connector))).thenReturn(expected);
        
        Connector result = new AutoProxyConnectorProvider().createProxyConnector(factory, engine);

        assertSame(expected, result);
    }
}
