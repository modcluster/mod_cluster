/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.load.metric;

import org.jboss.modcluster.load.metric.impl.DeterministicLoadStateImpl;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class DeterministicLoadMetricTestCase {
    private DeterministicLoadStateImpl state = new DeterministicLoadStateImpl();

    @Test
    public void testDelta() throws InterruptedException {
        long lastTime = this.state.getPreviousTime();

        Thread.sleep(500);

        double result = this.state.delta(20);

        long nextTime = this.state.getPreviousTime();

        double elapsed = (nextTime - lastTime) / 1000d;

        Assert.assertEquals(20 / elapsed, result, 0);

        lastTime = this.state.getPreviousTime();

        Thread.sleep(1000);

        result = this.state.delta(50);

        nextTime = this.state.getPreviousTime();

        elapsed = (nextTime - lastTime) / 1000d;

        Assert.assertEquals(30 / elapsed, result, 0);
    }
}
