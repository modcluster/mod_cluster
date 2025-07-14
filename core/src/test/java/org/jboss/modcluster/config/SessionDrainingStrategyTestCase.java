/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.jboss.modcluster.config.impl.SessionDrainingStrategyEnum;
import org.jboss.modcluster.container.Context;
import org.junit.jupiter.api.Test;

class SessionDrainingStrategyTestCase {
    private Context context = mock(Context.class);

    @Test
    void defaultStrategy() {
        when(this.context.isDistributable()).thenReturn(false);

        boolean result = SessionDrainingStrategyEnum.DEFAULT.isEnabled(this.context);

        assertTrue(result);

        when(this.context.isDistributable()).thenReturn(true);

        result = SessionDrainingStrategyEnum.DEFAULT.isEnabled(this.context);

        assertFalse(result);
    }

    @Test
    void alwaysStrategy() {
        boolean result = SessionDrainingStrategyEnum.ALWAYS.isEnabled(this.context);

        assertTrue(result);
    }

    @Test
    void neverStrategy() {
        boolean result = SessionDrainingStrategyEnum.NEVER.isEnabled(this.context);

        assertFalse(result);
    }
}
