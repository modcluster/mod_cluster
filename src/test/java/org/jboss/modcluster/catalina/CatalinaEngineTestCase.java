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

import java.util.Iterator;

import javax.management.MBeanServer;

import junit.framework.Assert;

import org.apache.catalina.Container;
import org.apache.catalina.Service;
import org.easymock.EasyMock;
import org.jboss.modcluster.Connector;
import org.jboss.modcluster.Engine;
import org.jboss.modcluster.Host;
import org.jboss.modcluster.Server;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
public class CatalinaEngineTestCase
{
   private org.apache.catalina.Engine engine = EasyMock.createStrictMock(org.apache.catalina.Engine.class);
   private Server server = EasyMock.createStrictMock(Server.class);
   
   private Engine catalinaEngine = new CatalinaEngine(this.engine, this.server);
   
   @Test
   public void findHost()
   {
      org.apache.catalina.Host host = EasyMock.createMock(org.apache.catalina.Host.class);
      
      EasyMock.expect(this.engine.findChild("host")).andReturn(host);
      
      EasyMock.replay(this.engine);
      
      Host result = this.catalinaEngine.findHost("host");
      
      EasyMock.verify(this.engine);
      
      Assert.assertSame(this.catalinaEngine, result.getEngine());
      
      EasyMock.reset(this.engine);
   }

   @Test
   public void getHosts()
   {
      org.apache.catalina.Host host = EasyMock.createMock(org.apache.catalina.Host.class);
      
      EasyMock.expect(this.engine.findChildren()).andReturn(new Container[] { host });
      
      EasyMock.replay(this.engine);
      
      Iterable<Host> result = this.catalinaEngine.getHosts();
      
      EasyMock.verify(this.engine);
      
      Iterator<Host> hosts = result.iterator();
      Assert.assertTrue(hosts.hasNext());
      Assert.assertSame(this.catalinaEngine, hosts.next().getEngine());
      Assert.assertFalse(hosts.hasNext());
      
      EasyMock.reset(this.engine);
   }

   @Test
   public void getJvmRoute()
   {
      String expected = "route";
      
      EasyMock.expect(this.engine.getJvmRoute()).andReturn(expected);
      
      EasyMock.replay(this.engine);
      
      String result = this.catalinaEngine.getJvmRoute();
      
      EasyMock.verify(this.engine);
      
      Assert.assertSame(expected, result);
      
      EasyMock.reset(this.engine);
   }

   @Test
   public void getName()
   {
      String expected = "name";
      
      EasyMock.expect(this.engine.getName()).andReturn(expected);
      
      EasyMock.replay(this.engine);
      
      String result = this.catalinaEngine.getName();
      
      EasyMock.verify(this.engine);
      
      Assert.assertSame(expected, result);
      
      EasyMock.reset(this.engine);
   }

   @Test
   public void getProxyConnector() throws Exception
   {
      org.apache.catalina.connector.Connector connector = new org.apache.catalina.connector.Connector("AJP/1.3");
      Service service = EasyMock.createStrictMock(Service.class);
      
      EasyMock.expect(this.engine.getService()).andReturn(service);
      EasyMock.expect(service.findConnectors()).andReturn(new org.apache.catalina.connector.Connector[] { connector });
      
      EasyMock.replay(this.engine, service);
      
      Connector result = this.catalinaEngine.getProxyConnector();
      
      EasyMock.verify(this.engine, service);
      
      Assert.assertSame(Connector.Type.AJP, result.getType());
      
      EasyMock.reset(this.engine, service);
   }

   @Test
   public void setJvmRoute()
   {
      this.engine.setJvmRoute("route");
      
      EasyMock.replay(this.engine);
      
      this.catalinaEngine.setJvmRoute("route");
      
      EasyMock.verify(this.engine);
      EasyMock.reset(this.engine);
   }
   
   @Test
   public void getServer()
   {
      EasyMock.replay(this.engine, this.server);
      
      Server result = this.catalinaEngine.getServer();
      
      EasyMock.verify(this.engine, this.server);
      
      Assert.assertSame(this.server, result);
      
      EasyMock.reset(this.engine, this.server);
      
      MBeanServer mbeanServer = EasyMock.createStrictMock(MBeanServer.class);
      Service service = EasyMock.createStrictMock(Service.class);
      org.apache.catalina.Server server = EasyMock.createStrictMock(org.apache.catalina.Server.class);
      
      EasyMock.expect(this.engine.getService()).andReturn(service);
      EasyMock.expect(service.getServer()).andReturn(server);
      
      EasyMock.replay(this.engine, this.server, service, server, mbeanServer);
      
      Engine catalinaEngine = new CatalinaEngine(this.engine, mbeanServer);
      result = catalinaEngine.getServer();
      
      EasyMock.verify(this.engine, this.server, service, server, mbeanServer);
      
      Assert.assertSame(mbeanServer, result.getMBeanServer());
      
      EasyMock.reset(this.engine, this.server, service, server, mbeanServer);
   }
}
