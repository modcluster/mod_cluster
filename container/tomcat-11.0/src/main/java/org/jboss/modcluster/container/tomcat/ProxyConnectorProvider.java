/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.container.tomcat;

import org.apache.catalina.Engine;
import org.jboss.modcluster.container.Connector;

/**
 * Strategy for determining the connector with which mod_cluster will communicate.
 *
 * @author Paul Ferraro
 */
public interface ProxyConnectorProvider {
    Connector createProxyConnector(Engine engine);
}
