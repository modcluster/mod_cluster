/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.demo.client.load;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Brian Stansberry
 */
public enum ServerLoadServlets {
    ACTIVE_SESSIONS("Active Sessions", "Generates server load by causing session creation on the target server.", "sessions",
            new ServerLoadParam("count", "Number of Sessions", "Number of sessions to create", "20")),

    DATASOURCE_USAGE(
            "Datasource Use",
            "Generates server load by taking connections from the java:DefaultDS datasource for a period",
            "database",
            new ServerLoadParam("count", "Number of Connections", "Number of connections to request from the datasource", "20"),
            new ServerLoadParam("duration", "Duration",
                    "Number of seconds to hold the connections before returning to datasource", "15")),

    CONNECTOR_THREAD_USAGE("Connector Thread Use",
            "Generates server load by tieing up threads in the webserver connections pool for a period", "connectors",
            new ServerLoadParam("count", "Number of Connections", "Number of connection pool threads to tie up", "50"),
            new ServerLoadParam("duration", "Duration", "Number of seconds to tie up the connections", "15")),

    HEAP_MEMORY_USAGE("Heap Memory Use", "Generates server load by filling a percentage of free heap memory for a period",
            "heap", new ServerLoadParam("duration", "Duration", "Number of seconds to maintain memory usage", "15"),
            new ServerLoadParam("ratio", "Ratio", "Percentage of heap memory to reserve", "0.9")),

    CPU_USAGE("CPU Use", "Generates server CPU load by initiating a tight loop in a thread", "cpu", new ServerLoadParam(
            "duration", "Duration", "Number of seconds to maintain CPU usage", "15")),

    RECEIVE_TRAFFIC_USAGE("Server Receive Traffic",
            "Generates server traffic receipt load by POSTing a large byte array to the server once per second for a period",
            "receive", new ServerLoadParam("size", "POST size", "Number of bytes to POST, divided by 1000", "100"),
            new ServerLoadParam("duration", "Duration", "Number of seconds to continue POSTing", "15")),

    SEND_TRAFFIC_USAGE(
            "Server Send Traffic",
            "Generates server traffic send load by making a request once per second to which the server responds with a large byte array",
            "send",
            new ServerLoadParam("size", "Response size", "Size of the server response in bytes, divided by 1000", "100"),
            new ServerLoadParam("duration", "Duration", "Number of seconds to continue POSTing", "15")),

    REQUEST_COUNT_USAGE("Request Count",
            "Generates server load by making numerous requests, increasing the request count on the target server.",
            "requests", new ServerLoadParam("count", "Number of Requests", "Number of requestss to make", "50"));

    private final String label;
    private final String description;
    private final String servletPath;
    private final List<ServerLoadParam> params;

    ServerLoadServlets(String label, String description, String servletPath, ServerLoadParam... params) {
        this.label = label;
        this.description = description;
        this.servletPath = servletPath;
        if (params != null) {
            this.params = Collections.unmodifiableList(Arrays.asList(params));
        } else {
            this.params = Collections.emptyList();
        }
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public String getServletPath() {
        return servletPath;
    }

    public List<ServerLoadParam> getParams() {
        return params;
    }

    @Override
    public String toString() {
        return label;
    }
}
