/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster;

import java.util.ResourceBundle;

/**
 * Holds current version of mod_cluster.
 *
 * @author Radoslav Husar
 * @author Paul Ferraro
 */
public enum Version {
    INSTANCE("version");

    private static final ResourceBundle resource = ResourceBundle.getBundle(Version.class.getName());
    private final String key;

    Version(String key) {
        this.key = key;
    }

    public String toString() {
        return resource.getString(this.key);
    }
}