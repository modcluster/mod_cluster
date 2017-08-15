/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
