/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.container.tomcat;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.junit.Test;

public class JMXServerProviderTestCase {

    @Test
    public void test() throws Exception {
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
    public void testNotFound() throws Exception {
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
