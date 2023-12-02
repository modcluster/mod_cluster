/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.config;

import org.jboss.modcluster.container.Context;

/**
 * Defines the strategy for draining sessions from a context
 *
 * @author Paul Ferraro
 */
public interface SessionDrainingStrategy {
    /**
     * Indicates whether or not to drain sessions prior to stopping the specified context.
     *
     * @param context a web application context
     * @return true, if sessions draining is enabled, false otherwise
     */
    boolean isEnabled(Context context);
}