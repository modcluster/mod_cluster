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
package org.jboss.modcluster.container;

import java.net.InetAddress;

/**
 * SPI for a connector, defined as a communication end-point for a client or proxy.
 * 
 * @author Paul Ferraro
 */
public interface Connector {
    /**
     * Indicates the type of a connector.
     */
    enum Type {
        AJP(8009), HTTP(80), HTTPS(443);

        private final int defaultPort;

        Type(int defaultPort) {
            this.defaultPort = defaultPort;
        }

        /**
         * Indicates the default port for this type of connector
         * 
         * @return a valid port number
         */
        public int getDefaultPort() {
            return this.defaultPort;
        }

        /**
         * {@inhericDoc}
         * 
         * @see java.lang.Enum#toString()
         */
        public String toString() {
            return this.name().toLowerCase();
        }
    };

    /**
     * Indicates whether the endpoint of this connector uses a reverse connection to httpd. A reverse connection uses a normal
     * socket connection, instead of the traditional server socket.
     * 
     * @return true, if the endpoint uses a reverse connection, false otherwise
     */
    boolean isReverse();

    /**
     * Indicates the type of this connector
     * 
     * @return a connector type
     */
    Type getType();

    /**
     * Returns the address on which this connector operates
     * 
     * @return an address
     */
    InetAddress getAddress();

    /**
     * Sets the address on which this connector operates. Used to set an explicit connector address if it undefined or defined
     * as any address.
     * 
     * @param address a network interface address
     */
    void setAddress(InetAddress address);

    /**
     * Returns the port on which this connector operates
     * 
     * @return a port number
     */
    int getPort();

    /**
     * Is this connector available for processing requests?
     */
    boolean isAvailable();

    int getMaxThreads();

    int getBusyThreads();

    long getBytesSent();

    long getBytesReceived();

    long getRequestCount();
}
