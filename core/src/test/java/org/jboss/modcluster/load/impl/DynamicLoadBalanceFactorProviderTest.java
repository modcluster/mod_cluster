/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.modcluster.load.impl;

import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.load.metric.LoadMetric;
import org.jboss.modcluster.load.metric.NodeUnavailableException;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Radoslav Husar
 */
public class DynamicLoadBalanceFactorProviderTest {

    @Test
    public void getLoadBalanceFactor() throws Exception {
        Engine engine = mock(Engine.class);

        Set<LoadMetric> metrics = new HashSet<>();
        LoadMetric metric = mock(LoadMetric.class);
        when(metric.getWeight()).thenReturn(LoadMetric.DEFAULT_WEIGHT);
        when(metric.getLoad(engine)).thenThrow(new NodeUnavailableException());
        metrics.add(metric);

        DynamicLoadBalanceFactorProvider provider = new DynamicLoadBalanceFactorProvider(metrics);

        int loadBalanceFactor = provider.getLoadBalanceFactor(engine);
        assertEquals(-1, loadBalanceFactor);
    }

    @Test
    public void getLoadBalanceFactorWithFloatDecayFactor() throws Exception {
	Engine engine = mock(Engine.class);

	Set<LoadMetric> metrics = new HashSet<>();
	LoadMetric metric = mock(LoadMetric.class);
	when(metric.getWeight()).thenReturn(LoadMetric.DEFAULT_WEIGHT);
	when(metric.getCapacity()).thenReturn(LoadMetric.DEFAULT_CAPACITY);
	when(metric.getLoad(engine)).thenReturn(0.3, 0.4, 0.5);
	metrics.add(metric);

	DynamicLoadBalanceFactorProvider provider = new DynamicLoadBalanceFactorProvider(metrics);
	provider.setDecayFactor(1.5f);

	int loadBalanceFactor = provider.getLoadBalanceFactor(engine);
	loadBalanceFactor = provider.getLoadBalanceFactor(engine);
	loadBalanceFactor = provider.getLoadBalanceFactor(engine);
	assertEquals(57, loadBalanceFactor);
    }
}
