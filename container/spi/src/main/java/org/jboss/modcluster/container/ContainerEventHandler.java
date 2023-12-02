/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.container;

/**
 * Defines the container events to which mod_cluster will respond. This API defines the integration point between mod_cluster
 * and the servlet container.
 *
 * @author Paul Ferraro
 */
public interface ContainerEventHandler {
    /**
     * Triggers the initialization of mod_cluster. This event should be triggered only once, after the startup of the servlet
     * container, but before triggering the {@link #start(Server)} event.
     *
     * @param server a server
     */
    void init(Server server);

    /**
     * Triggers the shutdown of mod_cluster. Closes any resources created in {@link #init(Server)}.
     */
    void shutdown();

    /**
     * Indicates the deployment of a new web application which is not intending to start immediately. This will issue a STOP-APP
     * without session draining command on the proxies. In case the application will start
     * use {@link ContainerEventHandler#start(Context)}.
     *
     * @param context the added context
     */
    void add(Context context);

    /**
     * Indicates the specified web application context was started. This event triggers an ENABLE-APP command for the specified
     * context.
     *
     * @param context the started context
     */
    void start(Context context);

    /**
     * Indicates the specified web application context was stopped. This event triggers a STOP-APP command for the context
     * started by {@link #start(Context)}.
     *
     * @param context the stopped context
     */
    void stop(Context context);

    /**
     * Indicates the undeployment of the specified context. This event triggers a REMOVE-APP command for the specified context.
     *
     * @param context the removed context
     */
    void remove(Context context);

    /**
     * This is a periodic event that triggers a STATUS command containing newly calculated load factor. This event also
     * processing of newly added/discovered proxies, and reset of any proxies in error.
     *
     * @param engine the engine to be processed
     */
    void status(Engine engine);

    /**
     * Indicates the servlet container has been started. This event triggers the configuration of each servlet engine, and the
     * addition of all web application contexts.
     *
     * @param server the started server
     */
    void start(Server server);

    /**
     * Indicates the servlet container has been stopped. This event triggers the removal of all web application contexts, and
     * REMOVE-APP * of each engine.
     *
     * @param server the stopped server
     */
    void stop(Server server);
}
