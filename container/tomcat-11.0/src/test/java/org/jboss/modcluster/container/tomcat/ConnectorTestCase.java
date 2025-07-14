/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.container.tomcat;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.tomcat.util.IntrospectionUtils;
import org.jboss.modcluster.container.Connector;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link TomcatConnector}.
 *
 * @author Radoslav Husar
 * @author Paul Ferraro
 */
class ConnectorTestCase {
    protected final Connector ajpConnector = createConnector("AJP/1.3", Connector.Type.AJP, false);
    protected final Connector httpConnector = createConnector("HTTP/1.1", Connector.Type.HTTP, false);
    protected final Connector httpsConnector = createConnector("HTTP/1.1", Connector.Type.HTTPS, true);

    protected Connector createConnector(String protocol, Connector.Type type, boolean sslEnabled) {
        org.apache.catalina.connector.Connector connector = this.createConnector(protocol);
        connector.setPort(type.getDefaultPort());
        this.setSSLEnabled(connector, sslEnabled);
        return this.createConnector(connector);
    }

    protected org.apache.catalina.connector.Connector createConnector(String protocol) {
        try {
            return new org.apache.catalina.connector.Connector(protocol);
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Failed to create %s connector", protocol), e);
        }
    }

    protected void setSSLEnabled(org.apache.catalina.connector.Connector connector, boolean sslEnabled) {
        IntrospectionUtils.setProperty(connector.getProtocolHandler(), "SSLEnabled", Boolean.toString(sslEnabled));
    }

    protected Connector createConnector(org.apache.catalina.connector.Connector connector) {
        return new TomcatConnector(connector);
    }

    @Test
    void getPort() {
        assertEquals(Connector.Type.AJP.getDefaultPort(), this.ajpConnector.getPort());
        assertEquals(Connector.Type.HTTP.getDefaultPort(), this.httpConnector.getPort());
        assertEquals(Connector.Type.HTTPS.getDefaultPort(), this.httpsConnector.getPort());
    }

    @Test
    void getType() {
        assertSame(Connector.Type.AJP, this.ajpConnector.getType());
        assertSame(Connector.Type.HTTP, this.httpConnector.getType());
        assertSame(Connector.Type.HTTPS, this.httpsConnector.getType());
    }

    @Test
    void isReverse() {
        assertFalse(this.ajpConnector.isReverse());
        assertFalse(this.httpConnector.isReverse());
        assertFalse(this.httpsConnector.isReverse());
    }

    @Test
    void setAddress() throws UnknownHostException {
        String address = "127.0.0.1";

        // Since 7.0.100, 8.5.51, 9.0.31, and 10.0.0-M3 the default bind address for the AJP/1.3 connector is the loopback address
        assertEquals(InetAddress.getLoopbackAddress(), this.ajpConnector.getAddress());

        this.ajpConnector.setAddress(InetAddress.getByName(address));
        assertEquals(address, this.ajpConnector.getAddress().getHostAddress());

        assertNull(this.httpConnector.getAddress());
        this.httpConnector.setAddress(InetAddress.getByName(address));
        assertEquals(address, this.httpConnector.getAddress().getHostAddress());

        assertNull(this.httpsConnector.getAddress());
        this.httpsConnector.setAddress(InetAddress.getByName(address));
        assertEquals(address, this.httpsConnector.getAddress().getHostAddress());
    }

    @Test
    void getBytesSent() {
        assertEquals(0, this.httpConnector.getBytesSent());
        assertEquals(0, this.httpsConnector.getBytesSent());
        assertEquals(0, this.ajpConnector.getBytesSent());
    }

    @Test
    void getBytesReceived() {
        assertEquals(0, this.httpConnector.getBytesReceived());
        assertEquals(0, this.httpsConnector.getBytesReceived());
        assertEquals(0, this.ajpConnector.getBytesReceived());
    }

    @Test
    void getRequestCount() {
        assertEquals(0, this.httpConnector.getRequestCount());
        assertEquals(0, this.httpsConnector.getRequestCount());
        assertEquals(0, this.ajpConnector.getRequestCount());
    }

    @Test
    void getMaxThreads() {
        assertEquals(0, this.httpConnector.getMaxThreads());
        assertEquals(0, this.httpsConnector.getMaxThreads());
        assertEquals(0, this.ajpConnector.getMaxThreads());
    }

    @Test
    void getBusyThreads() {
        assertEquals(0, this.httpConnector.getBusyThreads());
        assertEquals(0, this.httpsConnector.getBusyThreads());
        assertEquals(0, this.ajpConnector.getBusyThreads());
    }
}
