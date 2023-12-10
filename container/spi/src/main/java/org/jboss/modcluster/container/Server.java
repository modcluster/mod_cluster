/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.container;


/**
 * SPI for a web application server, defined as a collection of one or more Engines.
 *
 * @author Paul Ferraro
 */
public interface Server {
    /**
     * Returns the servlet engines associated with this server.
     *
     * @return the server's engines
     */
    Iterable<Engine> getEngines();
}
