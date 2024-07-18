/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.container.tomcat;

/**
 * Tomcat-specific configuration for selecting a connector to register with the proxy.
 *
 * @author Radoslav Husar
 */
public interface TomcatConnectorConfiguration {

    /**
     * Returns optional connector address.
     *
     * @return connector address
     */
    String getConnectorAddress();

    /**
     * Returns optional connector port.
     *
     * @return connector port
     */
    Integer getConnectorPort();

    /**
     * Returns optional external connector address.
     *
     * @return connector address
     */
    String getExternalConnectorAddress();

    /**
     * Returns optional external connector port.
     *
     * @return connector port
     */
    Integer getExternalConnectorPort();

}
