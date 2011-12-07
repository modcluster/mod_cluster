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
package org.jboss.modcluster.container.catalina;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.RequestGroupInfo;
import org.apache.tomcat.util.IntrospectionUtils;
import org.jboss.modcluster.container.Connector;

/**
 * {@link Connector} implementation that wraps a {@link org.apache.catalina.connector.Connector}.
 * 
 * @author Paul Ferraro
 */
public class CatalinaConnector implements Connector {
    protected final org.apache.catalina.connector.Connector connector;

    /**
     * Constructs a new CatalinaConnector wrapping the specified catalina connector.
     * 
     * @param connector the catalina connector
     */
    public CatalinaConnector(org.apache.catalina.connector.Connector connector) {
        this.connector = connector;
    }

    @Override
    public InetAddress getAddress() {
        Object value = IntrospectionUtils.getProperty(this.connector.getProtocolHandler(), "address");

        if (value instanceof InetAddress) return (InetAddress) value;

        if (value instanceof String) {
            try {
                return InetAddress.getByName((String) value);
            } catch (UnknownHostException e) {
                // Ignore
            }
        }

        return null;
    }

    @Override
    public void setAddress(InetAddress address) {
        IntrospectionUtils.setProperty(this.connector.getProtocolHandler(), "address", address.getHostAddress());
    }

    @Override
    public int getPort() {
        return this.connector.getPort();
    }

    @Override
    public Type getType() {
        if (isAJP(this.connector)) return Type.AJP;

        ProtocolHandler handler = this.connector.getProtocolHandler();

        return Boolean.TRUE.equals(IntrospectionUtils.getProperty(handler, "secure")) ? Type.HTTPS : Type.HTTP;
    }

    @Override
    public boolean isReverse() {
        return Boolean.TRUE.equals(IntrospectionUtils.getProperty(this.connector.getProtocolHandler(), "reverseConnection"));
    }

    @Override
    public boolean equals(Object object) {
        if ((object == null) || !(object instanceof CatalinaConnector)) return false;

        CatalinaConnector connector = (CatalinaConnector) object;

        return this.connector == connector.connector;
    }

    @Override
    public int hashCode() {
        return this.connector.hashCode();
    }

    @Override
    public String toString() {
        InetAddress address = this.getAddress();
        return String.format("%s://%s:%d", this.getType(), (address != null) ? address.getHostAddress() : "<undefined>", this.connector.getPort());
    }

    /**
     * Indicates whether or not the specified connector use the AJP protocol.
     * 
     * @param connector a connector
     * @return true, if the specified connector is AJP, false otherwise
     */
    public static boolean isAJP(org.apache.catalina.connector.Connector connector) {
        String protocol = connector.getProtocol();
        return protocol.startsWith("AJP") || protocol.startsWith("org.apache.coyote.ajp");
    }

    @Override
    public boolean isAvailable() {
        return this.connector.isAvailable();
    }

    @Override
    public int getMaxThreads() {
        // max thread is in Protocol since 6.0.x get it directly. (tc5.5.x is EOL).
        return (Integer) IntrospectionUtils.getProperty(this.connector.getProtocolHandler(), "maxThreads");
    }

    @Override
    public int getBusyThreads() {
    	// TODO this won't work.
        return this.getEndpointProperty("curThreadsBusy", Number.class).intValue();
    }
    
    private <T> T getEndpointProperty(String property, Class<T> targetClass) {
        Object endpoint = IntrospectionUtils.getProperty(this.connector.getProtocolHandler(), "endpoint");
        return targetClass.cast(IntrospectionUtils.getProperty(endpoint, property));
    }
    
    @Override
    public long getBytesSent() {
        return this.getRequestGroupInfo().getBytesSent();
    }

    @Override
    public long getBytesReceived() {
        return this.getRequestGroupInfo().getBytesReceived();
    }

    @Override
    public long getRequestCount() {
        return this.getRequestGroupInfo().getRequestCount();
    }

    private RequestGroupInfo getRequestGroupInfo() {
        Object handler = IntrospectionUtils.getProperty(this.connector.getProtocolHandler(), "cHandler");
        return (RequestGroupInfo) IntrospectionUtils.getProperty(handler, "global");
    }
}
