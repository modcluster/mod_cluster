/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.advertise;

import java.io.IOException;

/**
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public interface AdvertiseListener extends AutoCloseable {

    /**
     * Returns {@code true} if listener is accepting the advertise messages; false if the listener was stopped or is
     * experiencing network problems.
     */
    boolean isListening();

    /**
     * Replaces previous pause/stop/destroy lifecycle methods, terminating the advertise worker and closing the datagram
     * channel; may throw {@link IOException}.
     *
     * @throws IOException If an I/O error occurs while closing the underlying channel.
     */
    @Override
    void close() throws IOException;
}