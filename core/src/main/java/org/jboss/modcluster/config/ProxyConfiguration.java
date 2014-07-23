/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
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

package org.jboss.modcluster.config;

import java.net.InetSocketAddress;

/**
 * Proxy configuration with destination address and optional local address to bind to.
 *
 * @author Radoslav Husar
 * @version July 2014
 * @since 1.3.1
 */
public interface ProxyConfiguration {

    /**
     * Returns the remote address of the proxy.
     *
     * @return remote address of the proxy
     */
    InetSocketAddress getRemoteAddress();

    /**
     * Returns the local address to bind to for connecting to the proxy, if {@code null} the default will be used,
     * if port is {@code 0} an ephemeral port is used.
     *
     * @return local address to bind to for connecting to the proxy
     */
    InetSocketAddress getLocalAddress();

}
