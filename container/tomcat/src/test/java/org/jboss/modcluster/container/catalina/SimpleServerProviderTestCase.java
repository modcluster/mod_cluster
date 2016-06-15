package org.jboss.modcluster.container.catalina;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.apache.catalina.Server;
import org.jboss.modcluster.container.tomcat.ServerProvider;
import org.jboss.modcluster.container.tomcat.SimpleServerProvider;
import org.junit.Test;

public class SimpleServerProviderTestCase {
    @Test
    public void test() {
        Server server = mock(Server.class);
        ServerProvider provider = new SimpleServerProvider(server);
        assertSame(server, provider.getServer());
    }
}
