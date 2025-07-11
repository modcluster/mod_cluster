/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.load.impl;

import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.load.metric.LoadMetric;
import org.jboss.modcluster.load.metric.NodeUnavailableException;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link DynamicLoadBalanceFactorProvider}.
 *
 * @author Radoslav Husar
 */
class DynamicLoadBalanceFactorProviderTest {

    @Test
    void getLoadBalanceFactor() throws Exception {
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
    void getLoadBalanceFactor_FloatDecayFactor() throws Exception {
        Engine engine = mock(Engine.class);

        Set<LoadMetric> metrics = new HashSet<>();
        LoadMetric metric = mock(LoadMetric.class);
        when(metric.getWeight()).thenReturn(LoadMetric.DEFAULT_WEIGHT);
        when(metric.getCapacity()).thenReturn(LoadMetric.DEFAULT_CAPACITY);
        when(metric.getLoad(engine)).thenReturn(0.3, 0.4, 0.5);
        metrics.add(metric);

        DynamicLoadBalanceFactorProvider provider = new DynamicLoadBalanceFactorProvider(metrics, -1);
        provider.setDecayFactor(1.5f);

        assertEquals(70, provider.getLoadBalanceFactor(engine)); // 100-(0.3*100)/(100)*100 = 70
        assertEquals(64, provider.getLoadBalanceFactor(engine)); // 100-(0.3*100/1.5+0.4*100)/(100/1.5+100)*100 = 64
        assertEquals(57, provider.getLoadBalanceFactor(engine)); // 100-(0.3*100/(1.5^2)+0.4*100/1.5+0.5*100)/(100/(1.5^2)+100/1.5+100)*100 = 57.3684
    }

    @Test
    void getLoadBalanceFactor_InitialLoad() throws Exception {
        Engine engine = mock(Engine.class);

        Set<LoadMetric> metrics = new HashSet<>();
        LoadMetric metric = mock(LoadMetric.class);
        when(metric.getWeight()).thenReturn(LoadMetric.DEFAULT_WEIGHT);
        when(metric.getCapacity()).thenReturn(LoadMetric.DEFAULT_CAPACITY);
        when(metric.getLoad(engine)).thenReturn(0d);
        metrics.add(metric);

        // try full initial load
        DynamicLoadBalanceFactorProvider provider = new DynamicLoadBalanceFactorProvider(metrics, 0);
        provider.setHistory(4);
        provider.setDecayFactor(1f);

        assertEquals(20, provider.getLoadBalanceFactor(engine));
        assertEquals(40, provider.getLoadBalanceFactor(engine));
        assertEquals(60, provider.getLoadBalanceFactor(engine));
        assertEquals(80, provider.getLoadBalanceFactor(engine));
        assertEquals(100, provider.getLoadBalanceFactor(engine));
        assertEquals(100, provider.getLoadBalanceFactor(engine));

        // try 50% initial load
        provider = new DynamicLoadBalanceFactorProvider(metrics, 50);
        provider.setHistory(4);
        provider.setDecayFactor(1f);

        assertEquals(60, provider.getLoadBalanceFactor(engine));
        assertEquals(70, provider.getLoadBalanceFactor(engine));
        assertEquals(80, provider.getLoadBalanceFactor(engine));
        assertEquals(90, provider.getLoadBalanceFactor(engine));
        assertEquals(100, provider.getLoadBalanceFactor(engine));
        assertEquals(100, provider.getLoadBalanceFactor(engine));
    }

}
