/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.load.metric.impl;

import java.util.List;

import javax.management.JMException;

import org.jboss.modcluster.load.metric.LoadMetric;

/**
 * Generic {@link LoadMetric} whose load is the aggregated value of an mbean attribute.
 *
 * @author Paul Ferraro
 */
public class MBeanAttributeLoadMetric extends AbstractMBeanLoadMetric {
    private String attribute;

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    @Override
    protected double getLoad() throws JMException {
        double load = 0;
        List<Number> results = this.getAttributes(this.attribute, Number.class);
        for (Number result : results) {
            load += result.doubleValue();
        }
        return load;
    }
}
