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

import org.apache.tomcat.util.IntrospectionUtils;
import org.jboss.modcluster.Connector;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CatalinaConnectorTestCase
{
   private Connector ajpConnector;
   private Connector httpConnector;
   private Connector httpsConnector;
   
   @Before
   public void init() throws Exception
   {
      org.apache.catalina.connector.Connector connector = new org.apache.catalina.connector.Connector("AJP/1.3");
      connector.setPort(Connector.Type.AJP.getDefaultPort());
      
      this.ajpConnector = new CatalinaConnector(connector);
      
      connector = new org.apache.catalina.connector.Connector("HTTP/1.1");
      connector.setPort(Connector.Type.HTTP.getDefaultPort());
      
      this.httpConnector = new CatalinaConnector(connector);
      
      connector = new org.apache.catalina.connector.Connector("HTTP/1.1");
      connector.setPort(Connector.Type.HTTPS.getDefaultPort());
      IntrospectionUtils.setProperty(connector.getProtocolHandler(), "SSLEnabled", "true");
      
      this.httpsConnector = new CatalinaConnector(connector);
   }
   
   @Test
   public void getAddress() throws UnknownHostException
   {
      Assert.assertNull(this.ajpConnector.getAddress());
      Assert.assertNull(this.httpConnector.getAddress());
      Assert.assertNull(this.httpsConnector.getAddress());
   }

   @Test
   public void getPort()
   {
      Assert.assertEquals(Connector.Type.AJP.getDefaultPort(), this.ajpConnector.getPort());
      Assert.assertEquals(Connector.Type.HTTP.getDefaultPort(), this.httpConnector.getPort());
      Assert.assertEquals(Connector.Type.HTTPS.getDefaultPort(), this.httpsConnector.getPort());
   }

   @Test
   public void getType()
   {
      Assert.assertSame(Connector.Type.AJP, this.ajpConnector.getType());
      Assert.assertSame(Connector.Type.HTTP, this.httpConnector.getType());
      Assert.assertSame(Connector.Type.HTTPS, this.httpsConnector.getType());
   }

   @Test
   public void isReverse()
   {
      Assert.assertFalse(this.ajpConnector.isReverse());
      Assert.assertFalse(this.httpConnector.isReverse());
      Assert.assertFalse(this.httpsConnector.isReverse());
   }
   
   @Test
   public void setAddress() throws UnknownHostException
   {
      Assert.assertNull(this.ajpConnector.getAddress());
      this.ajpConnector.setAddress(InetAddress.getByName("127.0.0.1"));
      Assert.assertEquals("127.0.0.1", this.ajpConnector.getAddress().getHostAddress());
      
      Assert.assertNull(this.httpConnector.getAddress());
      this.httpConnector.setAddress(InetAddress.getByName("127.0.0.1"));
      Assert.assertEquals("127.0.0.1", this.httpConnector.getAddress().getHostAddress());
      
      Assert.assertNull(this.httpsConnector.getAddress());
      this.httpsConnector.setAddress(InetAddress.getByName("127.0.0.1"));
      Assert.assertEquals("127.0.0.1", this.httpsConnector.getAddress().getHostAddress());
   }
}
