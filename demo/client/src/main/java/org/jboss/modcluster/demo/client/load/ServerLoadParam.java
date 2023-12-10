/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.demo.client.load;

/**
 * @author Brian Stansberry
 */
public class ServerLoadParam {
    private final String name;
    private final String label;
    private final String description;
    private String value;

    public ServerLoadParam(String name, String label, String description, String value) {
        this.name = name;
        this.label = label;
        this.description = description;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
