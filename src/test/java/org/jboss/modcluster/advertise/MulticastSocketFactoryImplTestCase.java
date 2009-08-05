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
import java.net.MulticastSocket;
import java.util.Arrays;

import org.jboss.modcluster.advertise.impl.AdvertiseListenerImpl;
import org.jboss.modcluster.advertise.impl.MulticastSocketFactoryImpl;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests of {@link AdvertiseListenerImpl}.
 * 
 * @author Brian Stansberry
 */
public class MulticastSocketFactoryImplTestCase
{
   private static final String ADVERTISE_GROUP = "224.0.1.106";
   private static final int ADVERTISE_PORT = 23365;
   
   @Test
   public void createMulticastSocketNotLinux() throws IOException
   {
      String os = System.getProperty("os.name");
      
      boolean linux = os.trim().toLowerCase().startsWith("linux");
      
      try
      {
         if (linux)
         {
            System.setProperty("os.name", "OtherOS");
         }
         
         this.createMulticastSocketAsLinux();
      }
      finally
      {
         if (linux)
         {
            System.setProperty("os.name", os);
         }
      }
   }
   
   @Test
   public void createMulticastSocketAsLinux() throws IOException
   {
      InetAddress groupAddress = InetAddress.getByName(ADVERTISE_GROUP);
      
      MulticastSocketFactory socketFactory = new MulticastSocketFactoryImpl();
      MulticastSocket factorySocket = socketFactory.createMulticastSocket(groupAddress, ADVERTISE_PORT);
      
      try
      {
         factorySocket.joinGroup(groupAddress);
         
         testMulticastSocket(factorySocket, groupAddress);
         
         factorySocket.leaveGroup(groupAddress);
      }
      finally
      {
         factorySocket.close();
      }
   }
   
   public void testMulticastSocket(MulticastSocket factorySocket, InetAddress groupAddress) throws IOException
   {
      MulticastSocket socket = new MulticastSocket(ADVERTISE_PORT);
      
      try
      {
         socket.joinGroup(groupAddress);

         String data = "1234567890";
         
         byte[] buffer = data.getBytes();
         DatagramPacket packet = new DatagramPacket(buffer, buffer.length, groupAddress, ADVERTISE_PORT);
         
         socket.send(packet);
         
         try
         {
            Thread.sleep(20);
         }
         catch (InterruptedException e)
         {
            Thread.interrupted();
         }

         Arrays.fill(buffer, (byte) 0);
         
         packet = new DatagramPacket(buffer, buffer.length);
         
         factorySocket.receive(packet);
         
         Assert.assertArrayEquals(buffer, data.getBytes());
         
         socket.leaveGroup(groupAddress);
      }
      finally
      {
         socket.close();
      }
   }
}
