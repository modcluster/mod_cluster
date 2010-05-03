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
