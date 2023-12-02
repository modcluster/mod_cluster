/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.mcmp;

/**
 * Extends {@link MCMPServer} to provide information about the current state of communications with that server.
 *
 * @author Brian Stansberry
 */
public interface MCMPServerState extends MCMPServer {
    /** Possible communication states vis a vis the server */
    enum State {
        OK, ERROR, DOWN
    }

    State getState();
}
