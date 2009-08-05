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
package org.jboss.modcluster.mcmp;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.easymock.EasyMock;
import org.jboss.modcluster.ServerProvider;
import org.jboss.modcluster.config.BalancerConfiguration;
import org.jboss.modcluster.config.NodeConfiguration;
import org.jboss.modcluster.mcmp.impl.ResetRequestSourceImpl;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
public class ResetRequestSourceTestCase
{
   private final NodeConfiguration nodeConfig = EasyMock.createStrictMock(NodeConfiguration.class);
   private final BalancerConfiguration balancerConfig = EasyMock.createStrictMock(BalancerConfiguration.class);
   private final MCMPRequestFactory requestFactory = EasyMock.createStrictMock(MCMPRequestFactory.class);
   @SuppressWarnings("unchecked")
   private final ServerProvider<Server> serverProvider = EasyMock.createStrictMock(ServerProvider.class);
   
   @Test
   public void testGetResetRequestsNoServer()
   {
      ResetRequestSource source = new ResetRequestSourceImpl(this.nodeConfig, this.balancerConfig, this.serverProvider, this.requestFactory);

      EasyMock.expect(this.serverProvider.getServer()).andReturn(null);
      
      EasyMock.replay(this.serverProvider);
      
      Map<String, Set<ResetRequestSource.VirtualHost>> emptyResponseMap = Collections.emptyMap();
      
      List<MCMPRequest> requests = source.getResetRequests(emptyResponseMap);

      EasyMock.verify(this.serverProvider);
      
      Assert.assertTrue(requests.isEmpty());
   }
   
   @Test
   public void testGetResetRequests() throws Exception
   {
      ResetRequestSource source = new ResetRequestSourceImpl(this.nodeConfig, this.balancerConfig, this.serverProvider, this.requestFactory);
      
      Server server = EasyMock.createStrictMock(Server.class);
      Service service = EasyMock.createStrictMock(Service.class);
      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      Context context = EasyMock.createStrictMock(Context.class);
      MCMPRequest configRequest = EasyMock.createStrictMock(MCMPRequest.class);
      MCMPRequest contextRequest = EasyMock.createStrictMock(MCMPRequest.class);
      Map<String, Set<String>> emptyContextMap = Collections.emptyMap();
      
      source.init(emptyContextMap);

      EasyMock.expect(this.serverProvider.getServer()).andReturn(server);
      
      EasyMock.expect(server.findServices()).andReturn(new Service[] { service });
      EasyMock.expect(service.getContainer()).andReturn(engine);
      
      EasyMock.expect(this.requestFactory.createConfigRequest(engine, this.nodeConfig, this.balancerConfig)).andReturn(configRequest);

      EasyMock.expect(engine.getJvmRoute()).andReturn("host1");
      
      EasyMock.expect(engine.findChildren()).andReturn(new Container[] { host });
      EasyMock.expect(host.getName()).andReturn("host").times(2);
      EasyMock.expect(host.findAliases()).andReturn(new String[] { "alias1", "alias2" });
      EasyMock.expect(host.findChildren()).andReturn(new Container[] { context });
      EasyMock.expect(context.getPath()).andReturn("/context");
      EasyMock.expect(context.isStarted()).andReturn(true);
      
      EasyMock.expect(this.requestFactory.createEnableRequest(context)).andReturn(contextRequest);
      
      EasyMock.replay(this.serverProvider, server, this.requestFactory, service, engine, host, context, this.nodeConfig, this.balancerConfig);
      
      Map<String, Set<ResetRequestSource.VirtualHost>> emptyResponseMap = Collections.emptyMap();
      
      List<MCMPRequest> requests = source.getResetRequests(emptyResponseMap);
      
      EasyMock.verify(this.serverProvider, server, this.requestFactory, service, engine, host, context, this.nodeConfig, this.balancerConfig);
      
      Assert.assertEquals(2, requests.size());
      
      Assert.assertSame(configRequest, requests.get(0));
      Assert.assertSame(contextRequest, requests.get(1));
   }
}
