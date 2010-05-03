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
package org.jboss.modcluster;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Generates a jvm route using a UUID constructed from the connector address/port and engine name.
 * 
 * @author Paul Ferraro
 */
public class UUIDJvmRouteFactory implements JvmRouteFactory
{
   private static final int INT_SIZE = Integer.SIZE / Byte.SIZE;
   
   /**
    * {@inheritDoc}
    * @see org.jboss.modcluster.JvmRouteFactory#createJvmRoute(java.net.InetAddress, org.jboss.modcluster.Engine)
    */
   public String createJvmRoute(Engine engine)
   {
      Connector connector = engine.getProxyConnector();
      byte[] address = connector.getAddress().getAddress();
      byte[] name = engine.getName().getBytes();
      
      ByteBuffer buffer = ByteBuffer.allocate(address.length + INT_SIZE + name.length);

      buffer.put(address).putInt(connector.getPort()).put(name);
      
      return UUID.nameUUIDFromBytes(buffer.array()).toString();
   }
}
