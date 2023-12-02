/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.mcmp;

import java.util.Map;
import java.util.Set;

/**
 * @author Paul Ferraro
 */
public interface MCMPResponseParser {
    /**
     * Parses the response from a INFO request.
     *
     * @param response an INFO-RSP
     * @return a map of virtual hosts per jvm route.
     */
    Map<String, Set<ResetRequestSource.VirtualHost>> parseInfoResponse(String response);

    /**
     * Parses the response from a PING request.
     *
     * @param response a PING-RSP.
     * @return true, if the ping was successful, false otherwise.
     */
    boolean parsePingResponse(String response);

    /**
     * Parses the response from a STOP-APP request.
     *
     * @param response a STOP-APP-RSP
     * @return the number of current requests
     */
    int parseStopAppResponse(String response);
}
