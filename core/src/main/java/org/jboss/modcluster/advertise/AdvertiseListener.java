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