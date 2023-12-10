/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.container.tomcat;

import java.beans.PropertyChangeListener;

import org.apache.catalina.ContainerListener;
import org.apache.catalina.LifecycleListener;

/**
 * @author Paul Ferraro
 */
public interface TomcatEventHandler extends LifecycleListener, ContainerListener, PropertyChangeListener {

    void start();

    void stop();
}
