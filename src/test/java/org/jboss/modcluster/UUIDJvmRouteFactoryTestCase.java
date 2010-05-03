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
