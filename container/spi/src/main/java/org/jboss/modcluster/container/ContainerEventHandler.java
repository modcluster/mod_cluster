/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
     * Indicates the deployment of a new web application. This event triggers a ENABLE-APP command for this context, if it is
     * already started.
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
