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
package org.jboss.modcluster.advertise.impl;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.logging.Logger;
import org.jboss.modcluster.advertise.MulticastSocketFactory;

/**
 * On Linux, we attempt to avoid cross-talk problem by binding the MulticastSocket
 * to the multicast address, if possible.
 * See {@linkplain https://jira.jboss.org/jira/browse/JGRP-777}
 * 
 * @author Paul Ferraro
 */
public class MulticastSocketFactoryImpl implements MulticastSocketFactory
{
   final Logger log = Logger.getLogger(this.getClass());
   
   private final boolean linux;
   
   public MulticastSocketFactoryImpl()
   {
      String value = this.getSystemProperty("os.name");

      this.linux = (value != null) && value.toLowerCase().startsWith("linux");
   }
   
   private String getSystemProperty(final String key)
   {
      if (System.getSecurityManager() == null)
      {
         return System.getProperty(key);
      }
      
      PrivilegedAction<String> action = new PrivilegedAction<String>()
      {
         public String run()
         {
            try
            {
               return System.getProperty(key);
            }
            catch (SecurityException e)
            {
               MulticastSocketFactoryImpl.this.log.warn(e.getMessage(), e);
               
               return null;
            }
         }
      };
      
      return AccessController.doPrivileged(action);
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.advertise.MulticastSocketFactory#createMulticastSocket(java.net.InetAddress, int)
    */
   public MulticastSocket createMulticastSocket(InetAddress address, int port) throws IOException
   {
      if ((address == null) || !this.linux) return new MulticastSocket(port);

      if (!address.isMulticastAddress())
      {
         this.log.warn(address + " is not a multicast address, will be ignored");
         
         return new MulticastSocket(port);
      }

      try
      {
         return new MulticastSocket(new InetSocketAddress(address, port));
      }
      catch (IOException e)
      {
         StringBuilder builder = new StringBuilder();
         builder.append("could not bind to ").append(address.getHostAddress()).append(" (");
         builder.append((address instanceof Inet4Address) ? "IPv4" : "IPv6").append(" address)");
         builder.append("; make sure your mcast_addr is of the same type as the IP stack (IPv4 or IPv6).");
         builder.append("\nWill ignore mcast_addr, but this may lead to cross talking ");
         builder.append("(see http://www.jboss.org/community/docs/DOC-9469 for details). ");
         
         this.log.warn(builder.toString(), e);
         
         return new MulticastSocket(port);
      }
   }
}
