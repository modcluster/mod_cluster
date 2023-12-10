/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
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

    @Override
    public String createJvmRoute(Engine engine) {
        Connector connector = engine.getProxyConnector();

        return MessageFormat.format(this.pattern, connector.getAddress().getHostAddress(), Integer.toString(connector.getPort()), engine.getName());
    }
}
