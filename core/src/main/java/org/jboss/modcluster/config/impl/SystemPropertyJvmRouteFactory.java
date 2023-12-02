/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.config.impl;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.modcluster.config.JvmRouteFactory;
import org.jboss.modcluster.container.Engine;

/**
 * Decorates an existing jvm route factory, allowing system property override if defined.
 *
 * @author Paul Ferraro
 */
public class SystemPropertyJvmRouteFactory implements JvmRouteFactory {
    public static final String PROPERTY = "jboss.mod_cluster.jvmRoute";

    private final String property;
    private final JvmRouteFactory factory;

    /**
     * Creates a new SystemPropertyJvmRouteFactory using the default system {@link #PROPERTY}.
     *
     * @param factory the factory from which to generate a jvm route if the default system property is not defined.
     */
    public SystemPropertyJvmRouteFactory(JvmRouteFactory factory) {
        this(factory, PROPERTY);
    }

    /**
     * Creates a new SystemPropertyJvmRouteFactory
     *
     * @param property the system property that defines this node's jvm route
     * @param factory the factory from which to generate a jvm route if the specified system property is not defined.
     */
    public SystemPropertyJvmRouteFactory(JvmRouteFactory factory, String property) {
        this.factory = factory;
        this.property = property;
    }

    @Override
    public String createJvmRoute(Engine engine) {
        final String defaultJvmRoute = this.factory.createJvmRoute(engine);
        final String property = this.property;

        PrivilegedAction<String> action = new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(property, defaultJvmRoute);
            }
        };

        return AccessController.doPrivileged(action);
    }
}
