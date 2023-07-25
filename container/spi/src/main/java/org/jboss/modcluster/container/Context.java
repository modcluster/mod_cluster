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

import org.jboss.modcluster.container.listeners.HttpSessionListener;
import org.jboss.modcluster.container.listeners.ServletRequestListener;

/**
 * SPI for a web application context.
 *
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public interface Context {
    /**
     * Returns host associated with this context.
     *
     * @return host associated with this context
     */
    Host getHost();

    /**
     * Returns the context path. The root context needs to be represented as an empty string ("").
     *
     * @return context path; empty string if it's a root context
     */
    String getPath();

    /**
     * Returns whether this context is started and ready to accept requests.
     *
     * @return whether this context is started and ready to accept requests
     */
    boolean isStarted();

    /**
     * Returns whether this context is suspended. A suspended context is not available for processing requests,
     * but it could be made available at later time. This translates into the context be made known to reverse proxies
     * but in a stopped state. Implementing this method is optional defaulting to suspended mode not being supported
     * by the container.
     *
     * @return whether this context is suspended
     */
    default boolean isSuspended() {
        return false;
    }

    /**
     * Registers the specified request listener with this context. Used for request draining.
     *
     * @param requestListener request listener to register
     */
    void addRequestListener(ServletRequestListener requestListener);

    /**
     * Removes the specified previously registered request listener.
     *
     * @param requestListener request listener to remove
     */
    void removeRequestListener(ServletRequestListener requestListener);

    /**
     * Adds the specified session listener to this context.
     *
     * @param sessionListener a session listener to register
     */
    void addSessionListener(HttpSessionListener sessionListener);

    /**
     * Removes the specified session listener to this context.
     *
     * @param sessionListener a session listener to remove
     */
    void removeSessionListener(HttpSessionListener sessionListener);

    /**
     * Returns the number of active sessions for this context.
     *
     * @return the active session count
     */
    int getActiveSessionCount();

    /**
     * Indicates whether this context is distributable.
     *
     * @return true, if this context distributes sessions, false otherwise
     */
    boolean isDistributable();
}
