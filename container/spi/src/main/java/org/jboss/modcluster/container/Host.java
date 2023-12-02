/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.container;

import java.util.Set;

/**
 * SPI for a host, defined as a set of web application contexts.
 *
 * @author Paul Ferraro
 */
public interface Host {
    /**
     * The name of this host.
     *
     * @return the host name
     */
    String getName();

    /**
     * The engine to which this host is associated.
     *
     * @return the servlet engine
     */
    Engine getEngine();

    /**
     * Returns all contexts associated with this host.
     *
     * @return this host's contexts
     */
    Iterable<Context> getContexts();

    /**
     * Returns the aliases of this host, including the actual host name.
     *
     * @return a set of aliases
     */
    Set<String> getAliases();

    /**
     * Returns the context identified by the specified context path.
     *
     * @param path a context path
     * @return a web application context
     */
    Context findContext(String path);
}
