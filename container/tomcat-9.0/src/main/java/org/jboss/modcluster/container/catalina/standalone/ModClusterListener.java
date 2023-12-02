/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.container.catalina.standalone;

/**
 * Compatibility class for legacy server.xml configurations referencing this class through
 * {@code <Listener className="org.jboss.modcluster.container.catalina.standalone.ModClusterListener"/>}.
 *
 * @author Radoslav Husar
 */
@Deprecated
public class ModClusterListener extends org.jboss.modcluster.container.tomcat.ModClusterListener {
}
