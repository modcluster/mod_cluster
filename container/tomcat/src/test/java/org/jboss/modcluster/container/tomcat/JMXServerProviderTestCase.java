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
        
        when(mbeanServer.invoke(same(name), eq("findServices"), (Object[]) isNull(), (String[]) isNull())).thenReturn(new Service[] { service });
        when(service.getServer()).thenReturn(expected);
        
        ServerProvider provider = new JMXServerProvider(mbeanServer, name);
        Server result = provider.getServer();
        
        assertSame(expected, result);
    }
    
    @Test
    public void testNotFound() throws Exception {
        MBeanServer mbeanServer = mock(MBeanServer.class);
        ObjectName name = ObjectName.getInstance("Catalina:type=Server");
        
        when(mbeanServer.invoke(same(name), eq("findServices"), (Object[]) isNull(), (String[]) isNull())).thenThrow(new InstanceNotFoundException());
        
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
