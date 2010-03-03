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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.easymock.EasyMock;
import org.jboss.modcluster.Context;
import org.jboss.modcluster.Engine;
import org.jboss.modcluster.Host;
import org.jboss.modcluster.Server;
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
   
   private ResetRequestSource source = new ResetRequestSourceImpl(this.nodeConfig, this.balancerConfig, this.requestFactory);
   
   @Test
   public void getResetRequestsNoServer()
   {
      Server server = EasyMock.createStrictMock(Server.class);
      
      EasyMock.replay(server);
      
      List<MCMPRequest> requests = this.source.getResetRequests(Collections.<String, Set<ResetRequestSource.VirtualHost>>emptyMap());

      EasyMock.verify(server);
      
      Assert.assertTrue(requests.isEmpty());
      
      EasyMock.reset(server);
   }
   
   @Test
   public void getResetRequests() throws Exception
   {
      Server server = EasyMock.createStrictMock(Server.class);
      ContextFilter contextFilter = EasyMock.createStrictMock(ContextFilter.class);
      
      EasyMock.replay(server);
      
      this.source.init(server, contextFilter);

      EasyMock.verify(server);
      EasyMock.reset(server);

      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      Context context = EasyMock.createStrictMock(Context.class);
      Context excludedContext = EasyMock.createStrictMock(Context.class);
      MCMPRequest configRequest = EasyMock.createStrictMock(MCMPRequest.class);
      MCMPRequest contextRequest = EasyMock.createStrictMock(MCMPRequest.class);
      
      EasyMock.expect(contextFilter.getExcludedContexts()).andReturn(Collections.singletonMap(host, Collections.singleton("/excluded")));
      EasyMock.expect(contextFilter.isAutoEnableContexts()).andReturn(true);
      
      EasyMock.expect(server.getEngines()).andReturn(Collections.singleton(engine));
      
      EasyMock.expect(this.requestFactory.createConfigRequest(engine, this.nodeConfig, this.balancerConfig)).andReturn(configRequest);

      EasyMock.expect(engine.getJvmRoute()).andReturn("host1");
      
      EasyMock.expect(engine.getHosts()).andReturn(Collections.singleton(host));
      EasyMock.expect(host.getName()).andReturn("host");
      EasyMock.expect(host.getAliases()).andReturn(new TreeSet<String>(Arrays.asList("alias1", "alias2")));
      EasyMock.expect(host.getContexts()).andReturn(Arrays.asList(context, excludedContext));
      EasyMock.expect(context.getPath()).andReturn("/context");
      EasyMock.expect(context.isStarted()).andReturn(true);
      
      EasyMock.expect(this.requestFactory.createEnableRequest(context)).andReturn(contextRequest);
      
      EasyMock.expect(excludedContext.getPath()).andReturn("/excluded");
      
      EasyMock.replay(server, contextFilter, this.requestFactory, engine, host, context, excludedContext, this.nodeConfig, this.balancerConfig);
      
      List<MCMPRequest> requests = source.getResetRequests(Collections.<String, Set<ResetRequestSource.VirtualHost>>emptyMap());
      
      EasyMock.verify(server, contextFilter, this.requestFactory, engine, host, context, excludedContext, this.nodeConfig, this.balancerConfig);
      
      Assert.assertEquals(2, requests.size());
      
      Assert.assertSame(configRequest, requests.get(0));
      Assert.assertSame(contextRequest, requests.get(1));
      
      EasyMock.reset(server, contextFilter, this.requestFactory, engine, host, context, excludedContext, this.nodeConfig, this.balancerConfig);
   }
   
   @Test
   public void getResetRequestsDisableContexts() throws Exception
   {
      Server server = EasyMock.createStrictMock(Server.class);
      ContextFilter contextFilter = EasyMock.createStrictMock(ContextFilter.class);
      
      EasyMock.replay(server);
      
      this.source.init(server, contextFilter);

      EasyMock.verify(server);
      EasyMock.reset(server);

      Engine engine = EasyMock.createStrictMock(Engine.class);
      Host host = EasyMock.createStrictMock(Host.class);
      Context context = EasyMock.createStrictMock(Context.class);
      Context excludedContext = EasyMock.createStrictMock(Context.class);
      MCMPRequest configRequest = EasyMock.createStrictMock(MCMPRequest.class);
      MCMPRequest contextRequest = EasyMock.createStrictMock(MCMPRequest.class);
      
      EasyMock.expect(contextFilter.getExcludedContexts()).andReturn(Collections.singletonMap(host, Collections.singleton("/excluded")));
      EasyMock.expect(contextFilter.isAutoEnableContexts()).andReturn(false);
      
      EasyMock.expect(server.getEngines()).andReturn(Collections.singleton(engine));
      
      EasyMock.expect(this.requestFactory.createConfigRequest(engine, this.nodeConfig, this.balancerConfig)).andReturn(configRequest);

      EasyMock.expect(engine.getJvmRoute()).andReturn("host1");
      
      EasyMock.expect(engine.getHosts()).andReturn(Collections.singleton(host));
      EasyMock.expect(host.getName()).andReturn("host");
      EasyMock.expect(host.getAliases()).andReturn(new TreeSet<String>(Arrays.asList("alias1", "alias2")));
      EasyMock.expect(host.getContexts()).andReturn(Arrays.asList(context, excludedContext));
      EasyMock.expect(context.getPath()).andReturn("/context");
      EasyMock.expect(context.isStarted()).andReturn(true);
      
      EasyMock.expect(this.requestFactory.createDisableRequest(context)).andReturn(contextRequest);

      EasyMock.expect(excludedContext.getPath()).andReturn("/excluded");
      
      EasyMock.replay(server, contextFilter, this.requestFactory, engine, host, context, excludedContext, this.nodeConfig, this.balancerConfig);
      
      List<MCMPRequest> requests = source.getResetRequests(Collections.<String, Set<ResetRequestSource.VirtualHost>>emptyMap());
      
      EasyMock.verify(server, contextFilter, this.requestFactory, engine, host, context, excludedContext, this.nodeConfig, this.balancerConfig);
      
      Assert.assertEquals(2, requests.size());
      
      Assert.assertSame(configRequest, requests.get(0));
      Assert.assertSame(contextRequest, requests.get(1));
      
      EasyMock.reset(server, contextFilter, this.requestFactory, engine, host, context, excludedContext, this.nodeConfig, this.balancerConfig);
   }
}
