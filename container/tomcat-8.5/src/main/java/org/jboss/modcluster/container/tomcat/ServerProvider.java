/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.container.tomcat;

import org.apache.catalina.Server;

/**
 * @author Paul Ferraro
 */
public interface ServerProvider {
    Server getServer();
}
