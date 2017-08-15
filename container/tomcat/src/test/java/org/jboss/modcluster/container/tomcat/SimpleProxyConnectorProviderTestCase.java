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
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

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
