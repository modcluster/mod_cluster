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

package org.jboss.modcluster.mcmp;

import java.io.Serializable;
import java.net.InetAddress;

/**
 * Simple data object encapsulating an InetAddress and a port.
 * 
 * @author Brian Stansberry
 */
public class AddressPort implements Serializable
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 3835532412744565879L;
   
   private final InetAddress address;
   private final int port;
   
   public AddressPort(InetAddress address, int port)
   {
      this.address = address;
      this.port = port;
   }
   
   public InetAddress getAddress()
   {
      return this.address;
   }
   
   public int getPort()
   {
      return this.port;
   }

   @Override
   public boolean equals(Object object)
   {
      if (!(object instanceof AddressPort)) return false;
      
      AddressPort ap = (AddressPort) object;
      
      return (this.port == ap.getPort()) && (((this.address != null) && (ap.address != null)) ? this.address.equals(ap.address) : (this.address == ap.address));
   } 

   @Override
   public int hashCode()
   {
      int result = 17;
      result += 23 * (this.address == null ? 0 : this.address.hashCode());
      result += 23 * this.port;
      return result;
   }

   @Override
   public String toString()
   {
      return "AddressPort{" + this.address + ":" + this.port + "}";
   }
}