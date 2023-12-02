/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.mcmp;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.modcluster.container.Server;

/**
 * Source for a list of requests that should be sent to an httpd-side mod_cluster instance when an {@link MCMPHandler}
 * determines that the httpd-side state needs to be reset.
 *
 * @author Brian Stansberry
 */
public interface ResetRequestSource {
    enum Status {
        ENABLED, DISABLED, STOPPED
    }

    interface VirtualHost extends Serializable {
        Set<String> getAliases();

        Map<String, Status> getContexts();
    }

    void init(Server server, ContextFilter contextFilter);

    /**
     * Gets a list of requests that should be sent to an httpd-side mod_cluster instance when an {@link MCMPHandler} determines
     * that its state needs to be reset.
     *
     * @param response a parsed INFO-RSP, expressed as a map of virtual hosts per jvmRoute
     * @return a list of requests. Will not return <code>null</code>.
     */
    List<MCMPRequest> getResetRequests(Map<String, Set<VirtualHost>> response);
}
