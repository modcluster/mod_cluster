/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.mcmp;

import java.util.Set;

import org.jboss.modcluster.container.Host;

/**
 * @author Paul Ferraro
 */
public interface ContextFilter {
    /**
     * Returns the contexts that will *not* be registered in any proxy for the given host.
     *
     * @return a set of context paths excluded for the given host
     */
    Set<String> getExcludedContexts(Host host);

    /**
     * Indicates when contexts should auto-enable by default. If auto-enable is off, then contexts are disabled by default and
     * must be enabled manually.
     *
     * @return true, contexts should be auto-enabled, false otherwise.
     */
    boolean isAutoEnableContexts();
}
