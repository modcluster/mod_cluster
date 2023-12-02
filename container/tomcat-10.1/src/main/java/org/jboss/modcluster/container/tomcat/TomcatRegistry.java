/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.container.tomcat;

/**
 * Registry for sharing configuration with the container SPI objects.
 *
 * @author Radoslav Husar
 */
public interface TomcatRegistry {
    ProxyConnectorProvider getProxyConnectorProvider();
}
