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

import java.util.Set;

/**
 * SPI for a host, defined as a set of web application contexts.
 *
 * @author Paul Ferraro
 */
public interface Host {
    /**
     * The name of this host.
     *
     * @return the host name
     */
    String getName();

    /**
     * The engine to which this host is associated.
     *
     * @return the servlet engine
     */
    Engine getEngine();

    /**
     * Returns all contexts associated with this host.
     *
     * @return this host's contexts
     */
    Iterable<Context> getContexts();

    /**
     * Returns the aliases of this host, including the actual host name.
     *
     * @return a set of aliases
     */
    Set<String> getAliases();

    /**
     * Returns the context identified by the specified context path.
     *
     * @param path a context path
     * @return a web application context
     */
    Context findContext(String path);
}
