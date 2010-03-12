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

import org.apache.catalina.Service;
import org.easymock.EasyMock;
import org.jboss.modcluster.Engine;
import org.jboss.modcluster.Server;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
public class CatalinaServerTestCase
{
   private org.apache.catalina.Server server = EasyMock.createStrictMock(org.apache.catalina.Server.class);
   private MBeanServer mbeanServer = EasyMock.createStrictMock(MBeanServer.class);
   
   private Server catalinaServer = new CatalinaServer(this.server, this.mbeanServer);
   
   @Test
   public void getEngines()
   {
      Service service = EasyMock.createStrictMock(Service.class);
      org.apache.catalina.Engine engine = EasyMock.createStrictMock(org.apache.catalina.Engine.class);
      
      EasyMock.expect(this.server.findServices()).andReturn(new Service[] { service });
      
      EasyMock.replay(this.server, service);
      
      Iterable<Engine> result = this.catalinaServer.getEngines();
      
      EasyMock.verify(this.server, service);
      
      Iterator<Engine> engines = result.iterator();
      
      Assert.assertTrue(engines.hasNext());

      EasyMock.reset(this.server, service);
      
      EasyMock.expect(service.getContainer()).andReturn(engine);

      EasyMock.replay(this.server, service);
      
      engines.next();
      
      EasyMock.verify(this.server, service);
      
      Assert.assertFalse(engines.hasNext());
      
      EasyMock.reset(this.server, service);
   }
   
   @Test
   public void getMBeanServer()
   {
      EasyMock.replay(this.server, this.mbeanServer);
      
      MBeanServer result = this.catalinaServer.getMBeanServer();
      
      EasyMock.verify(this.server, this.mbeanServer);
      
      Assert.assertSame(this.mbeanServer, result);
      
      EasyMock.reset(this.server, this.mbeanServer);
   }
   
   @Test
   public void getDomain()
   {
      EasyMock.expect(this.mbeanServer.getDefaultDomain()).andReturn("domain");
      
      EasyMock.replay(this.server, this.mbeanServer);
      
      String result = this.catalinaServer.getDomain();
      
      EasyMock.verify(this.server, this.mbeanServer);
      
      Assert.assertEquals("domain", result);
      
      EasyMock.reset(this.server, this.mbeanServer);
      
      DomainServer server = EasyMock.createStrictMock(DomainServer.class);
      Server catalinaServer = new CatalinaServer(server, this.mbeanServer);
      
      EasyMock.expect(server.getDomain()).andReturn("domain");
      
      EasyMock.replay(server, this.mbeanServer);
      
      result = catalinaServer.getDomain();
      
      EasyMock.verify(server, this.mbeanServer);
      
      Assert.assertEquals("domain", result);
      
      EasyMock.reset(server, this.mbeanServer);
   }
   
   private interface DomainServer extends org.apache.catalina.Server
   {
      String getDomain();
   }
}
