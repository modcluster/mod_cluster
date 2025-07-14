/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.load.metric.impl;

import java.util.List;
import javax.management.JMException;

import org.jboss.modcluster.load.metric.LoadMetric;

/**
 * Generic {@link LoadMetric} whose load is the ratio of 2 aggregated mbean attributes.
 *
 * @author Paul Ferraro
 */
public class MBeanAttributeRatioLoadMetric extends AbstractMBeanLoadMetric {
    private String dividendAttribute;
    private String divisorAttribute;

    public void setDividendAttribute(String attribute) {
        this.dividendAttribute = attribute;
    }

    public void setDivisorAttribute(String attribute) {
        this.divisorAttribute = attribute;
    }

    @Override
    public double getLoad() throws JMException {
        double dividend = 0;
        List<Number> results = this.getAttributes(this.dividendAttribute, Number.class);
        for (Number result : results) {
            dividend += result.doubleValue();
        }

        double divisor = 0;
        results = this.getAttributes(this.divisorAttribute, Number.class);
        for (Number result : results) {
            divisor += result.doubleValue();
        }

        return dividend / divisor;
    }
}
