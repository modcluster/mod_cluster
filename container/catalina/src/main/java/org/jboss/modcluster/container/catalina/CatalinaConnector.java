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

import java.lang.reflect.Field;
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
        Integer result = (Integer) IntrospectionUtils.getProperty(this.connector.getProtocolHandler(), "maxThreads");
        return (result != null) ? result.intValue() : 0;
    }

    @Override
    public int getBusyThreads() {
        Object endpoint = this.getEndpoint();
        return (endpoint != null) ? (Integer) IntrospectionUtils.getProperty(endpoint, "currentThreadsBusy") : 0;
    }
    
    protected Object getEndpoint() {
        return this.getProtocolHandlerProperty("endpoint");
    }
    
    protected Object getProtocolHandlerProperty(String property) {
        Field field = this.findField(this.connector.getProtocolHandler().getClass(), property);
        if (field == null) {
            return null;
        }
        field.setAccessible(true);
        try {
            return field.get(this.connector.getProtocolHandler());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } 
    }
    
    Field findField(Class<?> targetClass, String name) {
        if ((targetClass == null) || Object.class.equals(targetClass)) return null;
        for (Field field: targetClass.getDeclaredFields()) {
            if (field.getName().equals(name)) {
                return field;
            }
        }
        return this.findField(targetClass.getSuperclass(), name);
    }
    
    @Override
    public long getBytesSent() {
        RequestGroupInfo info = this.getRequestGroupInfo();
        return (info != null) ? this.getRequestGroupInfo().getBytesSent() : 0;
    }

    @Override
    public long getBytesReceived() {
        RequestGroupInfo info = this.getRequestGroupInfo();
        return (info != null) ? this.getRequestGroupInfo().getBytesReceived() : 0;
    }

    @Override
    public long getRequestCount() {
        RequestGroupInfo info = this.getRequestGroupInfo();
        return (info != null) ? this.getRequestGroupInfo().getRequestCount() : 0;
    }

    protected Object getConnectionHandler() {
        return this.getProtocolHandlerProperty("cHandler");
    }
    
    protected RequestGroupInfo getRequestGroupInfo() {
        Object connectionHandler = this.getConnectionHandler();
        if (connectionHandler == null) return null;
        return this.getRequestGroupInfo(connectionHandler);
    }
    
    protected RequestGroupInfo getRequestGroupInfo(Object connectionHandler) {
        Field field = this.findField(connectionHandler.getClass(), "global");
        if (field == null) return null;
        field.setAccessible(true);
        try {
            return (RequestGroupInfo) field.get(connectionHandler);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
