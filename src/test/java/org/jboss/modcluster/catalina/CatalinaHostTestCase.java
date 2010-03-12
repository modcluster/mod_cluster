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
import java.util.Set;

import javax.management.MBeanServer;

import junit.framework.Assert;

import org.apache.catalina.Container;
import org.apache.catalina.Service;
import org.easymock.EasyMock;
import org.jboss.modcluster.Context;
import org.jboss.modcluster.Engine;
import org.jboss.modcluster.Host;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
public class CatalinaHostTestCase
{
   private org.apache.catalina.Host host = EasyMock.createStrictMock(org.apache.catalina.Host.class);
   private Engine engine = EasyMock.createStrictMock(Engine.class);
   
   private Host catalinaHost = new CatalinaHost(this.host, this.engine);
   
   @Test
   public void findContext()
   {
      org.apache.catalina.Context context = EasyMock.createMock(org.apache.catalina.Context.class);
      
      EasyMock.expect(this.host.findChild("path")).andReturn(context);
      
      EasyMock.replay(this.host, context);
      
      Context result = this.catalinaHost.findContext("path");
      
      EasyMock.verify(this.host, context);
      
      Assert.assertSame(this.catalinaHost, result.getHost());
      
      EasyMock.reset(this.host, context);
   }

   @Test
   public void getAliases()
   {
      EasyMock.expect(this.host.getName()).andReturn("host");
      EasyMock.expect(this.host.findAliases()).andReturn(new String[] { "alias" });
      
      EasyMock.replay(this.host);
      
      Set<String> result = this.catalinaHost.getAliases();
      
      EasyMock.verify(this.host);
      
      Assert.assertEquals(2, result.size());
      
      Iterator<String> aliases = result.iterator();
      Assert.assertEquals("host", aliases.next());
      Assert.assertEquals("alias", aliases.next());
      
      EasyMock.reset(this.host);
   }

   @Test
   public void getContexts()
   {
      org.apache.catalina.Context context = EasyMock.createMock(org.apache.catalina.Context.class);
      
      EasyMock.expect(this.host.findChildren()).andReturn(new Container[] { context });
      
      EasyMock.replay(this.host);
      
      Iterable<Context> result = this.catalinaHost.getContexts();
      
      EasyMock.verify(this.host);
      
      Iterator<Context> contexts = result.iterator();
      Assert.assertTrue(contexts.hasNext());
      Assert.assertSame(this.catalinaHost, contexts.next().getHost());
      Assert.assertFalse(contexts.hasNext());
      
      EasyMock.reset(this.host);
   }

   @Test
   public void getEngine()
   {
      EasyMock.replay(this.host, this.engine);
      
      Engine result = this.catalinaHost.getEngine();
      
      EasyMock.verify(this.host, this.engine);
      
      Assert.assertSame(this.engine, result);
      
      EasyMock.reset(this.host, this.engine);
      
      MBeanServer mbeanServer = EasyMock.createStrictMock(MBeanServer.class);
      org.apache.catalina.Engine engine = EasyMock.createStrictMock(org.apache.catalina.Engine.class);
      Service service = EasyMock.createStrictMock(Service.class);
      org.apache.catalina.Server server = EasyMock.createStrictMock(org.apache.catalina.Server.class);
      
      EasyMock.expect(this.host.getParent()).andReturn(engine);
      EasyMock.expect(engine.getService()).andReturn(service);
      EasyMock.expect(service.getServer()).andReturn(server);
      
      EasyMock.replay(this.host, this.engine, engine, service, server, mbeanServer);
      
      Host catalinaHost = new CatalinaHost(this.host, mbeanServer);
      result = catalinaHost.getEngine();
      
      EasyMock.verify(this.host, this.engine, engine, service, server, mbeanServer);
      
      Assert.assertSame(mbeanServer, result.getServer().getMBeanServer());
      
      EasyMock.reset(this.host, this.engine, engine, service, server, mbeanServer);
   }

   @Test
   public void getName()
   {
      String expected = "name";
      
      EasyMock.expect(this.host.getName()).andReturn(expected);
      
      EasyMock.replay(this.host);
      
      String result = this.catalinaHost.getName();
      
      EasyMock.verify(this.host);
      
      Assert.assertSame(expected, result);
      
      EasyMock.reset(this.host);
   }
}
