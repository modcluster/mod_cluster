/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.config;

/**
 * @author Brian Stansberry
 */
public interface BalancerConfiguration {
    /**
     * Enables sticky sessions.
     */
    boolean getStickySession();

    /**
     * Remove session when the request cannot be routed to the right node.
     */
    boolean getStickySessionRemove();

    /**
     * Return an error when the request cannot be routed to the right node.
     */
    boolean getStickySessionForce();

    /**
     * Timeout to wait for an available worker (default is no wait).
     */
    int getWorkerTimeout();

    /**
     * Maximum number of attempts to send the request to the backend server.
     */
    int getMaxAttempts();
}
