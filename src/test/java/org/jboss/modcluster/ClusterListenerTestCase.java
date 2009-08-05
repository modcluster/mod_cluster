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
package org.jboss.modcluster;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.catalina.Container;
import org.apache.catalina.ContainerEvent;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Engine;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Server;
import org.apache.catalina.ServerFactory;
import org.apache.catalina.Service;
import org.apache.tomcat.util.modeler.Registry;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.jboss.modcluster.mcmp.MCMPHandler;
import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPRequestType;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
@SuppressWarnings("boxing")
public class ClusterListenerTestCase
{
   private static final LifecycleServer server = EasyMock.createStrictMock(LifecycleServer.class);
   {
      ServerFactory.setServer(server);
   }
   
   private final MCMPHandler mcmpHandler = EasyMock.createStrictMock(MCMPHandler.class);
   private final LifecycleListener lifecycleListener = EasyMock.createStrictMock(LifecycleListener.class);
   private final ContainerListener containerListener = EasyMock.createStrictMock(ContainerListener.class);
   
   private final ClusterListener listener = new ClusterListener(this.mcmpHandler, this.lifecycleListener, this.containerListener);
   
   @Test
   public void getProxyConfiguration()
   {
      EasyMock.expect(this.mcmpHandler.getProxyConfiguration()).andReturn("config");

      EasyMock.replay(this.mcmpHandler);
      
      String result = this.listener.getProxyConfiguration();
      
      EasyMock.verify(this.mcmpHandler);
      
      Assert.assertEquals("config", result);
      
      EasyMock.reset(this.mcmpHandler);
   }
   
   @Test
   public void reset()
   {
      this.mcmpHandler.reset();

      EasyMock.replay(this.mcmpHandler);
      
      this.listener.reset();
      
      EasyMock.verify(this.mcmpHandler);
      EasyMock.reset(this.mcmpHandler);
   }
   
   @Test
   public void refresh()
   {
      this.mcmpHandler.markProxiesInError();

      EasyMock.replay(this.mcmpHandler);
      
      this.listener.refresh();
      
      EasyMock.verify(this.mcmpHandler);
      EasyMock.reset(this.mcmpHandler);
   }
   
   @Test
   public void enable()
   {
      Service service = EasyMock.createStrictMock(Service.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Capture<MCMPRequest> capturedRequest = new Capture<MCMPRequest>();
      
      EasyMock.expect(server.findServices()).andReturn(new Service[] { service });
      EasyMock.expect(service.getContainer()).andReturn(engine);
      EasyMock.expect(engine.getJvmRoute()).andReturn("host1");
      
      this.mcmpHandler.sendRequest(EasyMock.capture(capturedRequest));
      EasyMock.expect(this.mcmpHandler.isProxyHealthOK()).andReturn(true);
      
      EasyMock.replay(this.mcmpHandler, server, service, engine);
      
      boolean result = this.listener.enable();
      
      EasyMock.verify(this.mcmpHandler, server, service, engine);
      
      Assert.assertTrue(result);
      
      MCMPRequest request = capturedRequest.getValue();
      
      Assert.assertSame(MCMPRequestType.ENABLE_APP, request.getRequestType());
      Assert.assertTrue(request.isWildcard());
      Assert.assertEquals("host1", request.getJvmRoute());
      Assert.assertTrue(request.getParameters().isEmpty());
      
      EasyMock.reset(this.mcmpHandler, server, service, engine);
   }
   
   @Test
   public void disable()
   {
      Service service = EasyMock.createStrictMock(Service.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Capture<MCMPRequest> capturedRequest = new Capture<MCMPRequest>();
      
      EasyMock.expect(server.findServices()).andReturn(new Service[] { service });
      EasyMock.expect(service.getContainer()).andReturn(engine);
      EasyMock.expect(engine.getJvmRoute()).andReturn("host1");
      
      this.mcmpHandler.sendRequest(EasyMock.capture(capturedRequest));
      EasyMock.expect(this.mcmpHandler.isProxyHealthOK()).andReturn(true);
      
      EasyMock.replay(this.mcmpHandler, server, service, engine);
      
      boolean result = this.listener.disable();
      
      EasyMock.verify(this.mcmpHandler, server, service, engine);
      
      Assert.assertTrue(result);
      
      MCMPRequest request = capturedRequest.getValue();
      
      Assert.assertSame(MCMPRequestType.DISABLE_APP, request.getRequestType());
      Assert.assertTrue(request.isWildcard());
      Assert.assertEquals("host1", request.getJvmRoute());
      Assert.assertTrue(request.getParameters().isEmpty());
      
      EasyMock.reset(this.mcmpHandler, server, service, engine);
   }
   
   @Test
   public void containerEvent()
   {
      ContainerEvent event = new ContainerEvent(EasyMock.createMock(Container.class), Container.ADD_CHILD_EVENT, null);
      
      this.containerListener.containerEvent(event);
      
      EasyMock.replay(this.containerListener);

      this.listener.containerEvent(event);
      
      EasyMock.verify(this.containerListener);
      EasyMock.reset(this.containerListener);
   }
   
   @Test
   public void startServerLifecycleEvent() throws MalformedObjectNameException
   {
      LifecycleEvent event = new LifecycleEvent(server, Lifecycle.AFTER_START_EVENT);
      
      this.lifecycleListener.lifecycleEvent(event);
      
      EasyMock.expect(server.getDomain()).andReturn("domain");
      
      EasyMock.replay(this.lifecycleListener, server);
      
      this.listener.lifecycleEvent(event);
      
      EasyMock.verify(this.lifecycleListener, server);
      
      Registry registry = Registry.getRegistry(null, null);
      ObjectName name = ObjectName.getInstance("domain:type=ClusterListener");
      
      Assert.assertTrue(registry.getMBeanServer().isRegistered(name));
      
      EasyMock.reset(this.lifecycleListener, server);
   }
   
   @Test
   public void stopServerLifecycleEvent() throws MalformedObjectNameException
   {
      LifecycleEvent event = new LifecycleEvent(server, Lifecycle.STOP_EVENT);
      
      this.lifecycleListener.lifecycleEvent(event);
      
      EasyMock.expect(server.getDomain()).andReturn("domain");
      
      EasyMock.replay(this.lifecycleListener, server);
      
      this.listener.lifecycleEvent(event);
      
      EasyMock.verify(this.lifecycleListener, server);
      
      Registry registry = Registry.getRegistry(null, null);
      ObjectName name = ObjectName.getInstance("domain:type=ClusterListener");
      
      Assert.assertFalse(registry.getMBeanServer().isRegistered(name));
      
      EasyMock.reset(this.lifecycleListener, server);
   }
   
   @Test
   public void otherLifecycleEvent()
   {
      LifecycleEvent event = new LifecycleEvent(EasyMock.createMock(Lifecycle.class), Lifecycle.START_EVENT);
      
      this.lifecycleListener.lifecycleEvent(event);
      
      EasyMock.replay(this.lifecycleListener);
      
      this.listener.lifecycleEvent(event);
      
      EasyMock.verify(this.lifecycleListener);
      EasyMock.reset(this.lifecycleListener);
   }
   
   interface LifecycleServer extends Server, Lifecycle
   {
      String getDomain();
   }
}
