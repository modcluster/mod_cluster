/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.modcluster.container.tomcat8;

import org.junit.Assert;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public class ConnectorTestCase extends org.jboss.modcluster.container.tomcat.ConnectorTestCase {

    @Test
    public void setAddress() throws UnknownHostException {
        String address = "127.0.0.1";

        // Since 7.0.100, 8.5.51, 9.0.31, and 10.0.0-M3 the default bind address for the AJP/1.3 connector is the loopback address
        // -> however 8.0.x has not been updated yet
        Assert.assertNull(this.ajpConnector.getAddress());

        this.ajpConnector.setAddress(InetAddress.getByName(address));
        Assert.assertEquals(address, this.ajpConnector.getAddress().getHostAddress());

        Assert.assertNull(this.httpConnector.getAddress());
        this.httpConnector.setAddress(InetAddress.getByName(address));
        Assert.assertEquals(address, this.httpConnector.getAddress().getHostAddress());

        Assert.assertNull(this.httpsConnector.getAddress());
        this.httpsConnector.setAddress(InetAddress.getByName(address));
        Assert.assertEquals(address, this.httpsConnector.getAddress().getHostAddress());
    }
}
