/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.modcluster.container.tomcat;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.catalina.Server;
import org.apache.catalina.Service;

/**
 * {@link ServerProvider} that uses JMX to locate the {@link Server}.
 * @author Paul Ferraro
 */
public class JMXServerProvider implements ServerProvider {
    private final ObjectName name;
    private final MBeanServer mbeanServer;
    private volatile Server server;
    
    public JMXServerProvider(MBeanServer mbeanServer, ObjectName name) {
        this.mbeanServer = mbeanServer;
        this.name = name;
    }

    @Override
    public Server getServer() {
        if (this.server != null) return this.server;

        this.server = this.findServer();
        
        return this.server;
    }
    
    private Server findServer() {
        try {
            Service[] services = (Service[]) this.mbeanServer.invoke(this.name, "findServices", null, null);
            return (services.length > 0) ? services[0].getServer() : null;
        } catch (JMException e) {
            throw new IllegalStateException(e);
        }
    }
}
