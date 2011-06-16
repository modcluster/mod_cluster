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
import java.net.UnknownHostException;
import java.lang.reflect.Method;

import org.apache.coyote.ProtocolHandler;
import org.apache.tomcat.util.IntrospectionUtils;
import org.jboss.modcluster.Connector;

/**
 * {@link Connector} implementation that wraps a {@link org.apache.catalina.connector.Connector}.
 * @author Paul Ferraro
 */
public class CatalinaConnector implements Connector
{
   private final org.apache.catalina.connector.Connector connector;
   
   /**
    * Constructs a new CatalinaConnector wrapping the specified catalina connector.
    * @param connector the catalina connector
    */
   public CatalinaConnector(org.apache.catalina.connector.Connector connector)
   {
      this.connector = connector;
   }
   
   /**
    * {@inhericDoc}
    * @see org.jboss.modcluster.Connector#getAddress()
    */
   public InetAddress getAddress()
   {
      Object value = IntrospectionUtils.getProperty(this.connector.getProtocolHandler(), "address");
      
      if (value instanceof InetAddress) return (InetAddress) value;
      
      if (value instanceof String)
      {
         try
         {
            return InetAddress.getByName((String) value);
         }
         catch (UnknownHostException e)
         {
            // Ignore
         }
      }
      
      return null;
   }

   /**
    * {@inhericDoc}
    * @see org.jboss.modcluster.Connector#setAddress(java.net.InetAddress)
    */
   public void setAddress(InetAddress address)
   {
      IntrospectionUtils.setProperty(this.connector.getProtocolHandler(), "address", address.getHostAddress());
   }

   /**
    * {@inhericDoc}
    * @see org.jboss.modcluster.Connector#getPort()
    */
   public int getPort()
   {
      return this.connector.getPort();
   }

   /**
    * {@inhericDoc}
    * @see org.jboss.modcluster.Connector#getType()
    */
   public Type getType()
   {
      if (isAJP(this.connector)) return Type.AJP;
      
      ProtocolHandler handler = this.connector.getProtocolHandler();
      
      return Boolean.TRUE.equals(IntrospectionUtils.getProperty(handler, "SSLEnabled")) ? Type.HTTPS : Type.HTTP;
   }

   /**
    * {@inhericDoc}
    * @see org.jboss.modcluster.Connector#isReverse()
    */
   public boolean isReverse()
   {
      return Boolean.TRUE.equals(IntrospectionUtils.getProperty(this.connector.getProtocolHandler(), "reverseConnection"));
   }
   
   @Override
   public boolean equals(Object object)
   {
      if ((object == null) || !(object instanceof CatalinaConnector)) return false;
      
      CatalinaConnector connector = (CatalinaConnector) object;
      
      return this.connector == connector.connector;
   }

   @Override
   public int hashCode()
   {
      return this.connector.hashCode();
   }

   /**
    * {@inhericDoc}
    * @see java.lang.Object#toString()
    */
   public String toString()
   {
      InetAddress address = this.getAddress();
      return this.getType() + "://" + ((address != null) ? address.getHostAddress() : "<undefined>") + ":" + this.connector.getPort();
   }

   /**
    * Indicates whether or not the specified connector use the AJP protocol.
    * @param connector a connector
    * @return true, if the specified connector is AJP, false otherwise
    */
   public static boolean isAJP(org.apache.catalina.connector.Connector connector)
   {
      String protocol = connector.getProtocol();
      
      return protocol.startsWith("AJP") || protocol.startsWith("org.apache.coyote.ajp");
   }
    /**
     * Is this connector available for processing requests?
     */
    public boolean isAvailable() {
        try
        {
           return connector.isAvailable();
        }
        catch (NoSuchMethodError e)
        {
           try
           {
              // Tomcat 7 doesn't have isAvailable but the Lifecycle has it.
              String methodName = "getState";
              Class clazz = connector.getClass();
              Method method = clazz.getMethod("getState" , (Class [])null);
              Object obj = method.invoke(connector, (Object []) null);

              clazz = Class.forName("org.apache.catalina.LifecycleState");
              method = clazz.getMethod("isAvailable", (Class [])null);
              boolean ret = (Boolean) method.invoke(obj, (Object []) null);
              return ret;
           } catch (Exception ex) {
              return true; // Assume OK.
           } 
        } 
    }
}
