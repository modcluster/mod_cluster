/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.modcluster.load.metric;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedHashSet;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.QueryExp;

import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Server;
import org.jboss.modcluster.load.metric.impl.MBeanAttributeRatioLoadMetric;
import org.junit.Test;

/**
 * @author Paul Ferraro
 * 
 */
public class MBeanAttributeRatioLoadMetricTestCase {
    @Test
    public void getLoad() throws Exception {
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
