/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.config.impl;

import org.jboss.modcluster.config.SessionDrainingStrategy;
import org.jboss.modcluster.container.Context;

/**
 * @author Paul Ferraro
 */
public enum SessionDrainingStrategyEnum implements SessionDrainingStrategy {
    /** Drain sessions only if the target context is non-distributable, i.e. its sessions are not replicated */
    DEFAULT(null),
    /** Always drain sessions */
    ALWAYS(Boolean.TRUE),
    /** Never drain sessions */
    NEVER(Boolean.FALSE);

    private final Boolean drainSessions;

    SessionDrainingStrategyEnum(Boolean drainSessions) {
        this.drainSessions = drainSessions;
    }

    /**
     * @see SessionDrainingStrategy#isEnabled(org.jboss.modcluster.container.Context)
     */
    @Override
    public boolean isEnabled(Context context) {
        return (this.drainSessions != null) ? this.drainSessions : !context.isDistributable();
    }
}
