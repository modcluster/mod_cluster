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
package org.jboss.modcluster.test.mcmp;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import junit.framework.Assert;

import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPRequestType;
import org.jboss.modcluster.test.MBeanServerConnector;
import org.jboss.modcluster.test.MockProxy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
@SuppressWarnings("boxing")
public class MCMPTestCase
{
   private static final String MOD_CLUSTER_SERVICE = "jboss.web:service=ModCluster";
   private static final String ROOT_CONTEXT = "jboss.web.deployment:war=/ROOT";
   private static final String WEB_SERVER = "jboss.web:service=WebServer";
   private static final List<String> expectedContexts = Arrays.asList("/", "/invoker", "/juddi", "/jbossws", "/jmx-console", "/web-console");
   
   private MockProxy proxy = new MockProxy();
   private MBeanServerConnection server;
   private ObjectName modClusterService;
   private ObjectName rootContext;
   private ObjectName webServer;
   
   @Before
   public void init() throws Exception
   {
      this.server = new MBeanServerConnector(0).getServer();
      this.rootContext = ObjectName.getInstance(ROOT_CONTEXT);
      this.webServer = ObjectName.getInstance(WEB_SERVER);
      this.modClusterService = ObjectName.getInstance(MOD_CLUSTER_SERVICE);
      
      this.proxy.start();

      BlockingQueue<MCMPRequest> requests = this.proxy.getRequests();
      
      MCMPRequest request = requests.poll(20, TimeUnit.SECONDS);
      
      Assert.assertNotNull(request);
      Assert.assertSame(MCMPRequestType.INFO, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertNull(request.getJvmRoute());
      Assert.assertTrue(request.getParameters().isEmpty());

      request = requests.poll(1, TimeUnit.SECONDS);
      
      Assert.assertNotNull(request);
      Assert.assertSame(MCMPRequestType.CONFIG, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertEquals("localhost", request.getJvmRoute());
      Assert.assertFalse(request.getParameters().isEmpty());

      Set<String> contexts = new HashSet<String>(expectedContexts);
      
      while (!contexts.isEmpty())
      {
         // Skip any accumulated STATUS requests
         do
         {
            request = requests.poll(1, TimeUnit.SECONDS);
            Assert.assertNotNull(request);
         }
         while (request.getRequestType() == MCMPRequestType.STATUS);
         
         Assert.assertSame(MCMPRequestType.ENABLE_APP, request.getRequestType());
         Assert.assertFalse(request.isWildcard());
         Assert.assertEquals("localhost", request.getJvmRoute());
         Map<String, String> parameters = request.getParameters();
         Assert.assertEquals(2, parameters.size());
         Assert.assertEquals("localhost", parameters.get("Alias"));
         String context = parameters.get("Context");
         Assert.assertTrue(context, contexts.remove(context));
      }

      // Verify STATUS message was sent
      request = requests.poll(1, TimeUnit.SECONDS);
      
      Assert.assertNotNull(request);
      Assert.assertSame(MCMPRequestType.STATUS, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertEquals("localhost", request.getJvmRoute());
      Map<String, String> parameters = request.getParameters();
      Assert.assertEquals(1, parameters.size());
      String load = parameters.get("Load");
      Assert.assertNotNull(load);
      Assert.assertTrue(load, Integer.parseInt(load) > 0);
      
      // Verify that a periodic STATUS message was sent
      request = requests.poll(11, TimeUnit.SECONDS);
      
      Assert.assertNotNull(request);
      Assert.assertSame(MCMPRequestType.STATUS, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertEquals("localhost", request.getJvmRoute());
      parameters = request.getParameters();
      Assert.assertEquals(1, parameters.size());
      load = parameters.get("Load");
      Assert.assertNotNull(load);
      Assert.assertTrue(load, Integer.parseInt(load) > 0);
   }
   
   @Test
   public void deployment() throws Exception
   {
      BlockingQueue<MCMPRequest> requests = this.proxy.getRequests();
      MCMPRequest request = requests.poll();
      
      // Test undeploy of webapp
      this.server.invoke(this.rootContext, "stop", new Object[0], new String[0]);
      
      // Skip any accumulated STATUS requests
      do
      {
         request = requests.poll();
         Assert.assertNotNull(request);
      }
      while (request.getRequestType() == MCMPRequestType.STATUS);
      
      Assert.assertSame(MCMPRequestType.STOP_APP, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertEquals("localhost", request.getJvmRoute());
      Map<String, String> parameters = request.getParameters();
      Assert.assertEquals(2, parameters.size());
      Assert.assertEquals("localhost", parameters.get("Alias"));
      Assert.assertEquals("/", parameters.get("Context"));
      
      request = requests.poll();
      Assert.assertNotNull(request);
      Assert.assertSame(MCMPRequestType.REMOVE_APP, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertEquals("localhost", request.getJvmRoute());
      parameters = request.getParameters();
      Assert.assertEquals(2, parameters.size());
      Assert.assertEquals("localhost", parameters.get("Alias"));
      Assert.assertEquals("/", parameters.get("Context"));
      
      // Test deploy of webapp
      this.server.invoke(this.rootContext, "start", new Object[0], new String[0]);
      
      // Skip any accumulated STATUS requests
      do
      {
         request = requests.poll();      
         Assert.assertNotNull(request);
      }
      while (request.getRequestType() == MCMPRequestType.STATUS);
      
      Assert.assertSame(MCMPRequestType.ENABLE_APP, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertEquals("localhost", request.getJvmRoute());
      parameters = request.getParameters();
      Assert.assertEquals(2, parameters.size());
      Assert.assertEquals("localhost", parameters.get("Alias"));
      Assert.assertEquals("/", parameters.get("Context"));
   }
   
   @Test
   public void reenable() throws Exception
   {
      BlockingQueue<MCMPRequest> requests = this.proxy.getRequests();
      MCMPRequest request = requests.poll();
      
      // Test ModClusterService.disable()
      boolean ok = (Boolean) this.server.invoke(this.modClusterService, "disable", new Object[0], new String[0]);
      
      Assert.assertTrue(ok);
      
      // Skip any accumulated STATUS requests
      do
      {
         request = requests.poll();      
         Assert.assertNotNull(request);
      }
      while (request.getRequestType() == MCMPRequestType.STATUS);

      Assert.assertNotNull(request);
      Assert.assertSame(MCMPRequestType.DISABLE_APP, request.getRequestType());
      Assert.assertTrue(request.isWildcard());
      Assert.assertEquals("localhost", request.getJvmRoute());
      Assert.assertTrue(request.getParameters().toString(), request.getParameters().isEmpty());
      
      // Test ModClusterService.enable()
      ok = (Boolean) this.server.invoke(this.modClusterService, "enable", new Object[0], new String[0]);
      
      Assert.assertTrue(ok);
      
      // Skip any accumulated STATUS requests
      do
      {
         request = requests.poll();      
         Assert.assertNotNull(request);
      }
      while (request.getRequestType() == MCMPRequestType.STATUS);

      Assert.assertSame(MCMPRequestType.ENABLE_APP, request.getRequestType());
      Assert.assertTrue(request.isWildcard());
      Assert.assertEquals("localhost", request.getJvmRoute());
      Assert.assertTrue(request.getParameters().toString(), request.getParameters().isEmpty());
   }
   
   @Test
   public void restart() throws Exception
   {
      BlockingQueue<MCMPRequest> requests = this.proxy.getRequests();
      MCMPRequest request = requests.poll();
      
      // Test web server stop
      this.server.invoke(this.webServer, "stop", new Object[0], new String[0]);

      // Skip any accumulated STATUS requests
      do
      {
         request = requests.poll(1, TimeUnit.SECONDS);      
         Assert.assertNotNull(request);
      }
      while (request.getRequestType() == MCMPRequestType.STATUS);

      Set<String> stopContexts = new HashSet<String>(expectedContexts);
      Set<String> removeContexts = new HashSet<String>(expectedContexts);
    
      // Expect 6 contexts (from "all" profile)
      while (!stopContexts.isEmpty() && !removeContexts.isEmpty())
      {
         System.out.println(request);
         Assert.assertNotNull(request);
         Assert.assertSame(MCMPRequestType.STOP_APP, request.getRequestType());
         Assert.assertFalse(request.isWildcard());
         Assert.assertEquals("localhost", request.getJvmRoute());
         Map<String, String> parameters = request.getParameters();
         Assert.assertEquals(2, parameters.size());
         Assert.assertEquals("localhost", parameters.get("Alias"));
         String context = parameters.get("Context");
         Assert.assertTrue(context, stopContexts.remove(context));
         
         request = requests.poll(1, TimeUnit.SECONDS);
         
         System.out.println(request);
         Assert.assertNotNull(request);
         Assert.assertSame(MCMPRequestType.REMOVE_APP, request.getRequestType());
         Assert.assertFalse(request.isWildcard());
         Assert.assertEquals("localhost", request.getJvmRoute());
         parameters = request.getParameters();
         Assert.assertEquals(2, parameters.size());
         Assert.assertEquals("localhost", parameters.get("Alias"));
         context = parameters.get("Context");
         Assert.assertTrue(context, removeContexts.remove(context));
         
         request = requests.poll(1, TimeUnit.SECONDS);
      }
          
      System.out.println(request);
      Assert.assertNotNull(request);
      Assert.assertSame(MCMPRequestType.REMOVE_APP, request.getRequestType());
      Assert.assertTrue(request.isWildcard());
      Assert.assertEquals("localhost", request.getJvmRoute());
      Assert.assertTrue(request.getParameters().toString(), request.getParameters().isEmpty());
      
      // Test web server start
      this.server.invoke(this.webServer, "start", new Object[0], new String[0]);

      // Skip any accumulated STATUS requests
      do
      {
         request = requests.poll();      
         Assert.assertNotNull(request);
      }
      while (request.getRequestType() == MCMPRequestType.STATUS);
      
      Assert.assertSame(MCMPRequestType.CONFIG, request.getRequestType());
      Assert.assertFalse(request.isWildcard());
      Assert.assertEquals("localhost", request.getJvmRoute());
      Assert.assertFalse(request.getParameters().isEmpty());
      
      request = requests.poll(20, TimeUnit.SECONDS);
      
      while (request != null)
      {
         System.out.println(request);
         
         request = requests.poll();         
      }
/*
      Set<String> contexts = new HashSet<String>(expectedContexts);
      
      // Expect 6 contexts (from "all" profile)
      for (int i = 0; i < 6; ++i)
      {
         request = requests.poll(1, TimeUnit.SECONDS);
         
         Assert.assertNotNull(request);
         Assert.assertSame(MCMPRequestType.ENABLE_APP, request.getRequestType());
         Assert.assertFalse(request.isWildcard());
         Assert.assertEquals("localhost", request.getJvmRoute());
         Map<String, String> parameters = request.getParameters();
         Assert.assertEquals(2, parameters.size());
         Assert.assertEquals("localhost", parameters.get("Alias"));
         String context = parameters.get("Context");
         Assert.assertTrue(context, contexts.remove(context));
      }
*/
   }
   
   @After
   public void close() throws IOException
   {
      this.proxy.close();
   }
}
