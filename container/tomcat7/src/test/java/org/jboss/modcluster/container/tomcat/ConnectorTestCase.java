/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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

import org.apache.tomcat.util.IntrospectionUtils;
import org.jboss.modcluster.container.Connector;
import org.junit.Assert;

public class ConnectorTestCase extends org.jboss.modcluster.container.catalina.ConnectorTestCase {
    
    @Override
    protected Connector createConnector(org.apache.catalina.connector.Connector connector) {
        return new TomcatConnector(connector);
    }

    @Override
    protected void setSecure(org.apache.catalina.connector.Connector connector, boolean secure) {
        IntrospectionUtils.setProperty(connector.getProtocolHandler(), "secure", Boolean.toString(secure));
    }
    
    @Override
    public void getMaxThreads() {
        Assert.assertEquals(0, this.httpConnector.getMaxThreads());
        Assert.assertEquals(0, this.httpsConnector.getMaxThreads());
        Assert.assertEquals(0, this.ajpConnector.getMaxThreads());
    }
}
