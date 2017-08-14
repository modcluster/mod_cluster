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

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.tomcat.util.IntrospectionUtils;
import org.jboss.modcluster.container.Connector;
import org.junit.Assert;
import org.junit.Test;

public class ConnectorTestCase {
    protected final Connector ajpConnector = createConnector("AJP/1.3", Connector.Type.AJP, false);
    protected final Connector httpConnector = createConnector("HTTP/1.1", Connector.Type.HTTP, false);
    protected final Connector httpsConnector = createConnector("HTTP/1.1", Connector.Type.HTTPS, true);

    protected Connector createConnector(String protocol, Connector.Type type, boolean secure) {
        org.apache.catalina.connector.Connector connector = this.createConnector(protocol);
        connector.setPort(type.getDefaultPort());
        this.setSecure(connector, secure);
        return this.createConnector(connector);
    }

    protected org.apache.catalina.connector.Connector createConnector(String protocol) {
        try {
            return new org.apache.catalina.connector.Connector(protocol);
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Failed to create %s connector", protocol), e);
        }
    }

    protected void setSecure(org.apache.catalina.connector.Connector connector, boolean secure) {
        IntrospectionUtils.setProperty(connector.getProtocolHandler(), "secure", Boolean.toString(secure));
    }

    protected Connector createConnector(org.apache.catalina.connector.Connector connector) {
        return new TomcatConnector(connector);
    }

    @Test
    public void getPort() {
        Assert.assertEquals(Connector.Type.AJP.getDefaultPort(), this.ajpConnector.getPort());
        Assert.assertEquals(Connector.Type.HTTP.getDefaultPort(), this.httpConnector.getPort());
        Assert.assertEquals(Connector.Type.HTTPS.getDefaultPort(), this.httpsConnector.getPort());
    }

    @Test
    public void getType() {
        Assert.assertSame(Connector.Type.AJP, this.ajpConnector.getType());
        Assert.assertSame(Connector.Type.HTTP, this.httpConnector.getType());
        Assert.assertSame(Connector.Type.HTTPS, this.httpsConnector.getType());
    }

    @Test
    public void isReverse() {
        Assert.assertFalse(this.ajpConnector.isReverse());
        Assert.assertFalse(this.httpConnector.isReverse());
        Assert.assertFalse(this.httpsConnector.isReverse());
    }

    @Test
    public void setAddress() throws UnknownHostException {
        String address = "127.0.0.1";
        Assert.assertNull(this.ajpConnector.getAddress());
        this.ajpConnector.setAddress(InetAddress.getByName(address));
        Assert.assertEquals(address, this.ajpConnector.getAddress().getHostAddress());

        Assert.assertNull(this.httpConnector.getAddress());
        this.httpConnector.setAddress(InetAddress.getByName(address));
        Assert.assertEquals(address, this.httpConnector.getAddress().getHostAddress());

        Assert.assertNull(this.httpsConnector.getAddress());
        this.httpsConnector.setAddress(InetAddress.getByName(address));
        Assert.assertEquals(address, this.httpsConnector.getAddress().getHostAddress());
    }

    @Test
    public void getBytesSent() {
        Assert.assertEquals(0, this.httpConnector.getBytesSent());
        Assert.assertEquals(0, this.httpsConnector.getBytesSent());
        Assert.assertEquals(0, this.ajpConnector.getBytesSent());
    }

    @Test
    public void getBytesReceived() {
        Assert.assertEquals(0, this.httpConnector.getBytesReceived());
        Assert.assertEquals(0, this.httpsConnector.getBytesReceived());
        Assert.assertEquals(0, this.ajpConnector.getBytesReceived());
    }

    @Test
    public void getRequestCount() {
        Assert.assertEquals(0, this.httpConnector.getRequestCount());
        Assert.assertEquals(0, this.httpsConnector.getRequestCount());
        Assert.assertEquals(0, this.ajpConnector.getRequestCount());
    }

    @Test
    public void getMaxThreads() {
        Assert.assertEquals(0, this.httpConnector.getMaxThreads());
        Assert.assertEquals(0, this.httpsConnector.getMaxThreads());
        Assert.assertEquals(0, this.ajpConnector.getMaxThreads());
    }

    @Test
    public void getBusyThreads() {
        Assert.assertEquals(0, this.httpConnector.getBusyThreads());
        Assert.assertEquals(0, this.httpsConnector.getBusyThreads());
        Assert.assertEquals(0, this.ajpConnector.getBusyThreads());
    }
}
