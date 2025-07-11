/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.container.tomcat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.junit.jupiter.api.Test;

class JMXServerProviderTestCase {

    @Test
    void test() throws Exception {
        MBeanServer mbeanServer = mock(MBeanServer.class);
        ObjectName name = ObjectName.getInstance("Catalina:type=Server");
        Server expected = mock(Server.class);
        Service service = mock(Service.class);

        when(mbeanServer.invoke(same(name), eq("findServices"), isNull(), isNull())).thenReturn(new Service[] { service });
        when(service.getServer()).thenReturn(expected);

        ServerProvider provider = new JMXServerProvider(mbeanServer, name);
        Server result = provider.getServer();

        assertSame(expected, result);
    }

    @Test
    void testNotFound() throws Exception {
        MBeanServer mbeanServer = mock(MBeanServer.class);
        ObjectName name = ObjectName.getInstance("Catalina:type=Server");

        when(mbeanServer.invoke(same(name), eq("findServices"), isNull(), isNull())).thenThrow(new InstanceNotFoundException());

        ServerProvider provider = new JMXServerProvider(mbeanServer, name);
        RuntimeException exception = null;
        try {
            provider.getServer();
        } catch (IllegalStateException e) {
            exception = e;
        }
        assertNotNull(exception);
    }
}
