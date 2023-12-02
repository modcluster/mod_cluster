/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.container.listeners;

/**
 * @author Radoslav Husar
 */
public interface HttpSessionListener {
    void sessionCreated();

    void sessionDestroyed();
}
