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
package org.jboss.modcluster.advertise.impl;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.channels.DatagramChannel;

import org.jboss.modcluster.ModClusterLogger;
import org.jboss.modcluster.advertise.DatagramChannelFactory;

/**
 * @author Paul Ferraro
 * @author Radoslav Husar
 * @version May 2016
 */
public class DatagramChannelFactoryImpl implements DatagramChannelFactory {

    @Override
    public DatagramChannel createDatagramChannel(InetAddress address, int port) throws IOException {
        if (!address.isMulticastAddress()) {
            ModClusterLogger.LOGGER.createMulticastSocketWithUnicastAddress(address);
        }

        boolean isJdk7 = System.getProperty("java.version").startsWith("1.7.0");

        return DatagramChannel
                .open(address instanceof Inet4Address ? StandardProtocolFamily.INET : StandardProtocolFamily.INET6)
                .setOption(StandardSocketOptions.SO_REUSEADDR, true)
                .bind(isJdk7 ? new InetSocketAddress(address, port) : new InetSocketAddress(port))
                ;
    }
}
