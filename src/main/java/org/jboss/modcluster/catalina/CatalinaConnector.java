/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.modcluster.catalina;

import java.net.InetAddress;

import org.apache.coyote.ProtocolHandler;
import org.apache.tomcat.util.IntrospectionUtils;
import org.jboss.modcluster.Connector;
import org.jboss.modcluster.Utils;

/**
 * @author Paul Ferraro
 */
public class CatalinaConnector implements Connector
{
   private final org.apache.catalina.connector.Connector connector;
   
   public CatalinaConnector(org.apache.catalina.connector.Connector connector)
   {
      this.connector = connector;
   }
   
   public InetAddress getAddress()
   {
      return (InetAddress) IntrospectionUtils.getProperty(this.connector.getProtocolHandler(), "address");
   }

   public int getPort()
   {
      return this.connector.getPort();
   }

   public Type getType()
   {
      if (isAJP(this.connector)) return Type.AJP;
      
      ProtocolHandler handler = this.connector.getProtocolHandler();
      
      return Boolean.TRUE.equals(IntrospectionUtils.getProperty(handler, "SSLEnabled")) ? Type.HTTPS : Type.HTTP;
   }

   public boolean isReverse()
   {
      return Boolean.TRUE.equals(IntrospectionUtils.getProperty(this.connector.getProtocolHandler(), "reverseConnection"));
   }
   
   public String toString()
   {
      return this.getType() + "://" + Utils.identifyHost(this.getAddress()) + ":" + this.connector.getPort();
   }

   public static boolean isAJP(org.apache.catalina.connector.Connector connector)
   {
      String protocol = connector.getProtocol();
      
      return protocol.startsWith("AJP") || protocol.startsWith("org.apache.coyote.ajp");
   }
}
