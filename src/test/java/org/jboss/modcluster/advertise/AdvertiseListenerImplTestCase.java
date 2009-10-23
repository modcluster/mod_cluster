/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.modcluster.advertise;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

import junit.framework.Assert;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.jboss.modcluster.advertise.impl.AdvertiseListenerImpl;
import org.jboss.modcluster.advertise.impl.MulticastSocketFactoryImpl;
import org.jboss.modcluster.config.AdvertiseConfiguration;
import org.jboss.modcluster.mcmp.MCMPHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests of {@link AdvertiseListener}.
 * 
 * @author Brian Stansberry
 */
@SuppressWarnings("boxing")
public class AdvertiseListenerImplTestCase
{
   private static final String ADVERTISE_GROUP = "224.0.1.106";
   private static final int ADVERTISE_PORT = 23364;
   private static final String RFC_822_FMT = "EEE, d MMM yyyy HH:mm:ss Z";
   private static final DateFormat df = new SimpleDateFormat(RFC_822_FMT, Locale.US);
   private static final String SERVER1 = "127.0.0.1";
   private static final String SERVER2 = "127.0.1.1";
   private static final int SERVER_PORT = 8888;
   private static final String SERVER1_ADDRESS = String.format("%s:%d", SERVER1, SERVER_PORT);
   private static final String SERVER2_ADDRESS = String.format("%s:%d", SERVER2, SERVER_PORT);
   
   private MCMPHandler mcmpHandler = EasyMock.createStrictMock(MCMPHandler.class);
   private AdvertiseConfiguration config = EasyMock.createMock(AdvertiseConfiguration.class);
   private MulticastSocketFactory socketFactory = EasyMock.createMock(MulticastSocketFactory.class);
   
   private MulticastSocket socket;
   private InetAddress groupAddress;
   private AdvertiseListener listener;

   @Before
   public void setup() throws Exception
   {
      EasyMock.expect(this.config.getAdvertiseThreadFactory()).andReturn(Executors.defaultThreadFactory());
      EasyMock.expect(this.config.getAdvertiseGroupAddress()).andReturn(ADVERTISE_GROUP);
      EasyMock.expect(this.config.getAdvertisePort()).andReturn(ADVERTISE_PORT);
      EasyMock.expect(this.config.getAdvertiseSecurityKey()).andReturn(null);
      EasyMock.expect(this.config.getAdvertiseInterface()).andReturn(null);
      
      EasyMock.replay(this.config);
      
      this.listener = new AdvertiseListenerImpl(this.mcmpHandler, this.config, this.socketFactory);
      
      EasyMock.verify(this.config);
      EasyMock.reset(this.config);

      this.groupAddress = InetAddress.getByName(ADVERTISE_GROUP);
      this.socket = new MulticastSocketFactoryImpl().createMulticastSocket(this.groupAddress, ADVERTISE_PORT);
   }
   
   @After
   public void tearDown()
   {
      if ((this.socket != null) && !this.socket.isClosed())
      {
         this.socket.close();
      }
   }
   
   @Test
   public void testBasicOperation() throws IOException
   {
      Capture<InetAddress> capturedAddress = new Capture<InetAddress>();
      
      EasyMock.expect(this.socketFactory.createMulticastSocket(EasyMock.capture(capturedAddress), EasyMock.eq(ADVERTISE_PORT))).andReturn(this.socket);
      
      EasyMock.replay(this.socketFactory, this.mcmpHandler);
      
      this.listener.start();

      EasyMock.verify(this.socketFactory, this.mcmpHandler);
      
      Assert.assertEquals(ADVERTISE_GROUP, capturedAddress.getValue().getHostAddress());
      Assert.assertFalse(this.socket.isClosed());
      
      EasyMock.reset(this.socketFactory, this.mcmpHandler);
      
      Capture<InetSocketAddress> capturedSocketAddress = new Capture<InetSocketAddress>();
      String date = df.format(new Date());
      
      StringBuilder data = new StringBuilder("HTTP/1.1 200 OK\r\n");
      data.append("Date: ");
      data.append(date);
      data.append("\r\n");
      data.append("Server: ");
      data.append(SERVER1);
      data.append("\r\n");
      data.append("X-Manager-Address: ");
      data.append(SERVER1_ADDRESS);
      data.append("\r\n");
      
      byte[] buf = data.toString().getBytes();
      DatagramPacket packet = new DatagramPacket(buf, buf.length, this.groupAddress, ADVERTISE_PORT);
      
      this.mcmpHandler.addProxy(EasyMock.capture(capturedSocketAddress));    
      
      EasyMock.replay(this.socketFactory, this.mcmpHandler);
      
      this.socket.send(packet);
      
      try
      {
         // Give time for advertise worker to process message
         Thread.sleep(1000);
      }
      catch (InterruptedException e)
      {
         Thread.currentThread().interrupt();
      }
      
      EasyMock.verify(this.socketFactory, this.mcmpHandler);
      
      InetSocketAddress socketAddress = capturedSocketAddress.getValue();
      Assert.assertEquals(SERVER1, socketAddress.getAddress().getHostAddress());
      Assert.assertEquals(SERVER_PORT, socketAddress.getPort());
      
      EasyMock.reset(this.socketFactory, this.mcmpHandler);
      
      EasyMock.replay(this.socketFactory, this.mcmpHandler);
      
      this.socket.send(packet);
      
      try
      {
         // Give time for advertise worker to process message
         Thread.sleep(1000);
      }
      catch (InterruptedException e)
      {
         Thread.currentThread().interrupt();
      }
      
      EasyMock.verify(this.socketFactory, this.mcmpHandler);
      EasyMock.reset(this.socketFactory, this.mcmpHandler);
      
      EasyMock.replay(this.socketFactory, this.mcmpHandler);
      
      this.listener.pause();
      
      try
      {
         Thread.sleep(1000);
      }
      catch (InterruptedException e)
      {
         Thread.currentThread().interrupt();
      }
      
      EasyMock.verify(this.socketFactory, this.mcmpHandler);
      EasyMock.reset(this.socketFactory, this.mcmpHandler);
      
      capturedSocketAddress.reset();
      
      data = new StringBuilder("HTTP/1.1 200 OK\r\n");
      data.append("Date: ");
      data.append(date);
      data.append("\r\n");
      data.append("Server: ");
      data.append(SERVER2);
      data.append("\r\n");
      data.append("X-Manager-Address: ");
      data.append(SERVER2_ADDRESS);
      data.append("\r\n");
      
      buf = data.toString().getBytes();
      packet = new DatagramPacket(buf, buf.length, this.groupAddress, ADVERTISE_PORT);
      
      EasyMock.replay(this.socketFactory, this.mcmpHandler);
      
      this.socket.send(packet);
      
      try
      {
         // Give time for advertise worker to process message
         Thread.sleep(1000);
      }
      catch (InterruptedException e)
      {
         Thread.currentThread().interrupt();
      }
      
      EasyMock.verify(this.socketFactory, this.mcmpHandler);
      EasyMock.reset(this.socketFactory, this.mcmpHandler);
      
      this.mcmpHandler.addProxy(EasyMock.capture(capturedSocketAddress));    
      
      EasyMock.replay(this.socketFactory, this.mcmpHandler);
      
      this.listener.resume();

      this.socket.send(packet);
      
      try
      {
         // Give time for advertise worker to process message
         Thread.sleep(1000);
      }
      catch (InterruptedException e)
      {
         Thread.currentThread().interrupt();
      }
      
      EasyMock.verify(this.socketFactory, this.mcmpHandler);
      
      socketAddress = capturedSocketAddress.getValue();
      Assert.assertEquals(SERVER2, socketAddress.getAddress().getHostAddress());
      Assert.assertEquals(SERVER_PORT, socketAddress.getPort());
      
      EasyMock.reset(this.socketFactory, this.mcmpHandler);
      
      EasyMock.replay(this.socketFactory, this.mcmpHandler);
      
      this.listener.stop();
      
      EasyMock.verify(this.socketFactory, this.mcmpHandler);
      
      Assert.assertFalse(this.socket.isConnected());
      
      EasyMock.reset(this.socketFactory, this.mcmpHandler);
      
      EasyMock.replay(this.socketFactory, this.mcmpHandler);
      
      this.socket.send(packet);
      
      EasyMock.verify(this.socketFactory, this.mcmpHandler);
      EasyMock.reset(this.socketFactory, this.mcmpHandler);

      EasyMock.replay(this.socketFactory, this.mcmpHandler);
      
      this.listener.destroy();
      
      EasyMock.verify(this.socketFactory, this.mcmpHandler);
      
      Assert.assertTrue(this.socket.isClosed());
      
      EasyMock.reset(this.socketFactory, this.mcmpHandler);
   }
}
