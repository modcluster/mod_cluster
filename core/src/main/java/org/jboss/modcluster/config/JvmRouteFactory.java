/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.config;

import org.jboss.modcluster.container.Engine;

/**
 * Factory for creating a unique jvm route for an engine.
 *
 * @author Paul Ferraro
 */
public interface JvmRouteFactory {
    /**
     * Creates a unique jvm route for the specified engine.
     *
     * @param engine an engine
     * @return a jvm route
     */
    String createJvmRoute(Engine engine);
}
