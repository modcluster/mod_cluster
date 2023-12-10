/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.config.builder;

/**
 * Creator for common configuration objects.
 *
 * @author Radoslav Husar
 * @since 1.3.6.Final
 */
public interface Creator<T> {

    /**
     * Creates the configuration object.
     */
    T create();
}
