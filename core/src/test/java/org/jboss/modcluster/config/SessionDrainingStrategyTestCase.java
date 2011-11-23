package org.jboss.modcluster.config;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.jboss.modcluster.config.impl.SessionDrainingStrategyEnum;
import org.jboss.modcluster.container.Context;
import org.junit.Test;

public class SessionDrainingStrategyTestCase {
    private Context context = mock(Context.class);

    @Test
    public void defaultStrategy() {
        when(this.context.isDistributable()).thenReturn(false);

        boolean result = SessionDrainingStrategyEnum.DEFAULT.isEnabled(this.context);

        assertTrue(result);

        when(this.context.isDistributable()).thenReturn(true);

        result = SessionDrainingStrategyEnum.DEFAULT.isEnabled(this.context);

        assertFalse(result);
    }

    @Test
    public void alwaysStrategy() {
        boolean result = SessionDrainingStrategyEnum.ALWAYS.isEnabled(this.context);

        assertTrue(result);
    }

    @Test
    public void neverStrategy() {
        boolean result = SessionDrainingStrategyEnum.NEVER.isEnabled(this.context);

        assertFalse(result);
    }
}
