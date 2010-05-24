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

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

public class UUIDJvmRouteFactoryTestCase
{
   @Test
   public void test() throws IOException
   {
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Connector connector = EasyMock.createStrictMock(Connector.class);
      InetAddress address = InetAddress.getLocalHost();
      
      JvmRouteFactory factory = new UUIDJvmRouteFactory();
      
      EasyMock.expect(engine.getProxyConnector()).andReturn(connector);
      EasyMock.expect(connector.getAddress()).andReturn(address);
      EasyMock.expect(connector.getPort()).andReturn(8000);
      EasyMock.expect(engine.getName()).andReturn("engine");
      
      EasyMock.replay(engine, connector);
      
      String result = factory.createJvmRoute(engine);
      
      EasyMock.verify(engine, connector);
      
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      DataOutput output = new DataOutputStream(bytes);
      output.write(address.getAddress());
      output.writeInt(8000);
      output.write("engine".getBytes());
      
      Assert.assertEquals(result, UUID.nameUUIDFromBytes(bytes.toByteArray()).toString());
   }
}
