/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
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
     * Indicates the type of connector.
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
         * Returns lower case name of this enum constant.
         */
        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }

    /**
     * Indicates whether the endpoint of this connector uses a reverse connection to httpd. A reverse connection uses a normal
     * socket connection, instead of the traditional server socket.
     *
     * @return true, if the endpoint uses a reverse connection, false otherwise
     */
    boolean isReverse();

    /**
     * Indicates the type of this connector.
     *
     * @return a connector type
     */
    Type getType();

    /**
     * Returns the address on which this connector operates.
     *
     * @return an address
     */
    InetAddress getAddress();

    /**
     * Sets the address on which this connector operates. Used to set an explicit connector address if undefined or defined
     * as any address.
     *
     * @param address a network interface address
     */
    void setAddress(InetAddress address);

    /**
     * Returns the port on which this connector operates.
     *
     * @return a port number
     */
    int getPort();

    /**
     * Returns whether this connector available for processing requests.
     *
     * @return whether this connector available for processing requests
     */
    boolean isAvailable();

    int getMaxThreads();

    int getBusyThreads();

    long getBytesSent();

    long getBytesReceived();

    long getRequestCount();
}
