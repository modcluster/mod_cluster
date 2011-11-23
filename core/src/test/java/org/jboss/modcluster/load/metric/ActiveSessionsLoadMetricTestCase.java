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
 * 
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
