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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.jboss.modcluster.Utils;

/**
 * Abstract {@link MCMPHandler} that implements the trivial convenience methods.
 * 
 * @author Paul Ferraro
 */
public abstract class AbstractMCMPHandler implements MCMPHandler
{
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.mcmp.MCMPHandler#addProxy(java.lang.String)
    */
   public void addProxy(String address)
   {
      InetSocketAddress socketAddress = Utils.parseSocketAddress(address);
      
      this.addProxy(socketAddress.getAddress(), socketAddress.getPort());
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.mcmp.MCMPHandler#addProxy(java.lang.String, int)
    */
   public void addProxy(String host, int port)
   {
      try
      {
         InetAddress address = InetAddress.getByName(host);

         this.addProxy(address, port);
      }
      catch (UnknownHostException e)
      {
         throw new IllegalArgumentException(e);
      }
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.mcmp.MCMPHandler#removeProxy(java.lang.String, int)
    */
   public void removeProxy(String host, int port)
   {
      try
      {
         InetAddress address = InetAddress.getByName(host);

         this.removeProxy(address, port);
      }
      catch (UnknownHostException e)
      {
         throw new IllegalArgumentException(e);
      }
   }
}
