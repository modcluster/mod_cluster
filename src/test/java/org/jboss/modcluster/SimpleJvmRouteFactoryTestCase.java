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

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

public class SimpleJvmRouteFactoryTestCase
{
   @Test
   public void defaultPattern() throws UnknownHostException
   {
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Connector connector = EasyMock.createStrictMock(Connector.class);
      InetAddress address = InetAddress.getLocalHost();
      
      JvmRouteFactory factory = new SimpleJvmRouteFactory();
      
      EasyMock.expect(engine.getProxyConnector()).andReturn(connector);
      EasyMock.expect(connector.getAddress()).andReturn(address);
      EasyMock.expect(connector.getPort()).andReturn(8000);
      EasyMock.expect(engine.getName()).andReturn("engine");
      
      EasyMock.replay(engine, connector);
      
      String result = factory.createJvmRoute(engine);
      
      EasyMock.verify(engine, connector);
      
      Assert.assertEquals(result, address.getHostAddress() + ":8000:engine");
   }
   
   @Test
   public void customPattern() throws UnknownHostException
   {
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Connector connector = EasyMock.createStrictMock(Connector.class);
      InetAddress address = InetAddress.getLocalHost();
      
      JvmRouteFactory factory = new SimpleJvmRouteFactory("{2}-{1}-{0}");
      
      EasyMock.expect(engine.getProxyConnector()).andReturn(connector);
      EasyMock.expect(connector.getAddress()).andReturn(address);
      EasyMock.expect(connector.getPort()).andReturn(8000);
      EasyMock.expect(engine.getName()).andReturn("engine");
      
      EasyMock.replay(engine, connector);
      
      String result = factory.createJvmRoute(engine);
      
      EasyMock.verify(engine, connector);
      
      Assert.assertEquals(result, "engine-8000-" + address.getHostAddress());
   }
}
