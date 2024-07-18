/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.container.tomcat;

import org.apache.catalina.Valve;
import org.jboss.modcluster.container.listeners.ServletRequestListener;

/**
 * @author Paul Ferraro
 */
public interface RequestListenerValveFactory {
    Valve createValve(ServletRequestListener listener);
}
