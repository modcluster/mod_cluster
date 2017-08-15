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

import java.text.MessageFormat;

import org.jboss.modcluster.config.JvmRouteFactory;
import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.Engine;

/**
 * Generate a jvm route of the form:<br/>
 * <em>connector-bind-address</em>:<em>connector-port</em>:<em>engine-name</em>
 *
 * @author Paul Ferraro
 */
public class SimpleJvmRouteFactory implements JvmRouteFactory {
    private final String pattern;

    public SimpleJvmRouteFactory() {
        this("{0}:{1}:{2}");
    }

    public SimpleJvmRouteFactory(String pattern) {
        this.pattern = pattern;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.modcluster.JvmRouteFactory#createJvmRoute(java.net.InetAddress, org.jboss.modcluster.Engine)
     */
    @Override
    public String createJvmRoute(Engine engine) {
        Connector connector = engine.getProxyConnector();

        return MessageFormat.format(this.pattern, connector.getAddress().getHostAddress(), Integer.toString(connector.getPort()), engine.getName());
    }
}
