/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
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
