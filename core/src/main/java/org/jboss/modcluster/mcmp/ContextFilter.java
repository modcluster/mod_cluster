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
package org.jboss.modcluster.mcmp;

import java.util.Map;
import java.util.Set;

import org.jboss.modcluster.container.Host;

/**
 * @author Paul Ferraro
 */
public interface ContextFilter {
    /**
     * Returns the contexts that will *not* be registered in any proxy.
     * 
     * @return a map of context paths per host
     */
    Map<Host, Set<String>> getExcludedContexts();

    /**
     * Returns the contexts that will be registered as disabled in every proxy.
     *
     * @return a map of disabled context paths per host
     */
    Map<Host, Set<String>> getDisabledContexts();

    /**
     * Indicates when contexts should auto-enable by default. If auto-enable is off, then contexts are disabled by default and
     * must be enabled manually.
     * 
     * @return true, contexts should be auto-enabled, false otherwise.
     */
    boolean isAutoEnableContexts();
}
