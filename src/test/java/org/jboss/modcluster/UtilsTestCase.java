package org.jboss.modcluster;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.junit.Assert;
import org.junit.Test;

public class UtilsTestCase
{
   @Test
   public void parseParseSocketAddress() throws UnknownHostException
   {
      InetSocketAddress address = Utils.parseSocketAddress("127.0.0.1", 0);
      
      Assert.assertEquals("127.0.0.1", address.getAddress().getHostAddress());
      Assert.assertEquals(0, address.getPort());
      
      address = Utils.parseSocketAddress("127.0.0.1:80", 0);
      
      Assert.assertEquals("127.0.0.1", address.getAddress().getHostAddress());
      Assert.assertEquals(80, address.getPort());
      
      address = Utils.parseSocketAddress("localhost", 0);
      
      Assert.assertEquals("localhost", address.getAddress().getHostName());
      Assert.assertEquals(0, address.getPort());
      
      address = Utils.parseSocketAddress("localhost:80", 0);
      
      Assert.assertEquals("localhost", address.getAddress().getHostName());
      Assert.assertEquals(80, address.getPort());
      
      address = Utils.parseSocketAddress("0:0:0:0:0:0:0:1", 0);
      
      Assert.assertEquals("0:0:0:0:0:0:0:1", address.getAddress().getHostAddress());
      Assert.assertEquals(0, address.getPort());
      
      address = Utils.parseSocketAddress("::1", 0);
      
      Assert.assertEquals("0:0:0:0:0:0:0:1", address.getAddress().getHostAddress());
      Assert.assertEquals(0, address.getPort());
      
      address = Utils.parseSocketAddress("[0:0:0:0:0:0:0:1]:80", 0);
      
      Assert.assertEquals("0:0:0:0:0:0:0:1", address.getAddress().getHostAddress());
      Assert.assertEquals(80, address.getPort());
      
      address = Utils.parseSocketAddress("[::1]:80", 0);
      
      Assert.assertEquals("0:0:0:0:0:0:0:1", address.getAddress().getHostAddress());
      Assert.assertEquals(80, address.getPort());
   }
}
