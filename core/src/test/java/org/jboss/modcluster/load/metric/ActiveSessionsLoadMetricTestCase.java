/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.load.metric;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Host;
import org.jboss.modcluster.load.metric.impl.ActiveSessionsLoadMetric;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class ActiveSessionsLoadMetricTestCase {
    @Test
    public void getLoad() throws Exception {
        LoadMetric metric = new ActiveSessionsLoadMetric();
        Engine engine = mock(Engine.class);
        Host host = mock(Host.class);
        Context context = mock(Context.class);

        when(engine.getHosts()).thenReturn(Collections.singleton(host));
        when(host.getContexts()).thenReturn(Collections.singleton(context));
        when(context.getActiveSessionCount()).thenReturn(10);

        double load = metric.getLoad(engine);

        assertEquals(10.0, load, 0.0);
    }
}
