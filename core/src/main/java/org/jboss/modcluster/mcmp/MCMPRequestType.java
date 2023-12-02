/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.mcmp;

/**
 * Valid types of MCMP requests.
 *
 * @author Brian Stansberry
 */
public enum MCMPRequestType {
    CONFIG("CONFIG", true),
    ENABLE_APP("ENABLE-APP", false),
    DISABLE_APP("DISABLE-APP", false),
    STOP_APP("STOP-APP", false),
    REMOVE_APP("REMOVE-APP", false),
    STATUS("STATUS", false),
    INFO("INFO", false),
    DUMP("DUMP", false),
    PING("PING", false);

    private final String command;
    private final boolean establishesServer;

    MCMPRequestType(String command, boolean establishesServer) {
        this.command = command;
        this.establishesServer = establishesServer;
    }

    public boolean getEstablishesServer() {
        return this.establishesServer;
    }

    @Override
    public String toString() {
        return this.command;
    }
}
