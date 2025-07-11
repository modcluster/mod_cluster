/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.load.metric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedHashSet;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.QueryExp;

import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.load.metric.impl.MBeanAttributeRatioLoadMetric;
import org.junit.jupiter.api.Test;

/**
 * @author Paul Ferraro
 */
class MBeanAttributeRatioLoadMetricTestCase {
    @Test
    void getLoad() throws Exception {
        MBeanServer server = mock(MBeanServer.class);
        ObjectName pattern = ObjectName.getInstance("domain:*");
        String dividend = "dividend";
        String divisor = "divisor";

        MBeanAttributeRatioLoadMetric metric = new MBeanAttributeRatioLoadMetric();
        metric.setMBeanServer(server);
        metric.setPattern(pattern);
        metric.setDividendAttribute(dividend);
        metric.setDivisorAttribute(divisor);

        ObjectName name1 = ObjectName.getInstance("domain:name=test1");
        ObjectName name2 = ObjectName.getInstance("domain:name=test2");

        Engine engine = mock(Engine.class);

        when(server.queryNames(same(pattern), (QueryExp) isNull())).thenReturn(
                new LinkedHashSet<ObjectName>(Arrays.asList(name1, name2)));

        when(server.getAttribute(same(name1), same(dividend))).thenReturn(1);
        when(server.getAttribute(same(name2), same(dividend))).thenReturn(2);

        when(server.getAttribute(same(name1), same(divisor))).thenReturn(2);
        when(server.getAttribute(same(name2), same(divisor))).thenReturn(2);

        double load = metric.getLoad(engine);

        assertEquals(0.75, load, 0.0);
    }
}
