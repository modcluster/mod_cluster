/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.load.metric;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.LinkedHashSet;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.QueryExp;

import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.load.metric.impl.MBeanAttributeLoadMetric;
import org.junit.jupiter.api.Test;

/**
 * @author Paul Ferraro
 */
class MBeanAttributeLoadMetricTestCase {
    @Test
    void getLoad() throws Exception {
        MBeanServer server = mock(MBeanServer.class);
        ObjectName pattern = ObjectName.getInstance("domain:*");
        String attribute = "attribute";

        MBeanAttributeLoadMetric metric = new MBeanAttributeLoadMetric();
        metric.setMBeanServer(server);
        metric.setPattern(pattern);
        metric.setAttribute(attribute);

        ObjectName name1 = ObjectName.getInstance("domain:name=test1");
        ObjectName name2 = ObjectName.getInstance("domain:name=test2");

        Engine engine = mock(Engine.class);

        when(server.queryNames(same(pattern), (QueryExp) isNull())).thenReturn(new LinkedHashSet<ObjectName>(Arrays.asList(name1, name2)));
        when(server.getAttribute(same(name1), same(attribute))).thenReturn(1);
        when(server.getAttribute(same(name2), same(attribute))).thenReturn(2);

        double load = metric.getLoad(engine);

        assertEquals(3.0, load, 0.0);
    }
}
