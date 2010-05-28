package org.jboss.modcluster;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;

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
      
      address = Utils.parseSocketAddress("", 0);
      
      Assert.assertEquals(InetAddress.getLocalHost().getHostName(), address.getHostName());
      Assert.assertEquals(0, address.getPort());
      
      address = Utils.parseSocketAddress(null, 0);
      
      Assert.assertEquals(InetAddress.getLocalHost().getHostName(), address.getHostName());
      Assert.assertEquals(0, address.getPort());
   }

   @Test
   public void parseSocketAddresses()
   {
      List<InetSocketAddress> addresses = Utils.parseSocketAddresses("192.168.1.11:8000,192.168.1.13:8000", 8001);
      
      Assert.assertEquals(2, addresses.size());
      Assert.assertEquals("192.168.1.11", addresses.get(0).getAddress().getHostAddress());
      Assert.assertEquals(8000, addresses.get(0).getPort());
      Assert.assertEquals("192.168.1.13", addresses.get(1).getAddress().getHostAddress());
      Assert.assertEquals(8000, addresses.get(1).getPort());
      
      addresses = Utils.parseSocketAddresses(" 192.168.1.11:8000 , 192.168.1.13:8000 ", 8000);
      
      Assert.assertEquals(2, addresses.size());
      Assert.assertEquals("192.168.1.11", addresses.get(0).getAddress().getHostAddress());
      Assert.assertEquals(8000, addresses.get(0).getPort());
      Assert.assertEquals("192.168.1.13", addresses.get(1).getAddress().getHostAddress());
      Assert.assertEquals(8000, addresses.get(1).getPort());
      
      addresses = Utils.parseSocketAddresses("", 8000);
      
      Assert.assertEquals(0, addresses.size());
      
      addresses = Utils.parseSocketAddresses(null, 8000);
      
      Assert.assertEquals(0, addresses.size());
   }
}
