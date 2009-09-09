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
package org.jboss.modcluster.mcmp.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.ProtocolHandler;
import org.apache.tomcat.util.IntrospectionUtils;
import org.jboss.modcluster.Utils;
import org.jboss.modcluster.config.BalancerConfiguration;
import org.jboss.modcluster.config.NodeConfiguration;
import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPRequestFactory;
import org.jboss.modcluster.mcmp.MCMPRequestType;

/**
 * @author Paul Ferraro
 */
public class DefaultMCMPRequestFactory implements MCMPRequestFactory
{
   private static final Map<String, String> EMPTY_MAP = Collections.emptyMap();
   
   private final MCMPRequest infoRequest = new DefaultMCMPRequest(MCMPRequestType.INFO, false, null, EMPTY_MAP);
   private final MCMPRequest dumpRequest = new DefaultMCMPRequest(MCMPRequestType.DUMP, true, null, EMPTY_MAP);
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.mcmp.MCMPRequestFactory#createConfigRequest(org.apache.catalina.Engine, org.jboss.modcluster.config.NodeConfiguration, org.jboss.modcluster.config.BalancerConfiguration)
    */
   public MCMPRequest createConfigRequest(Engine engine, NodeConfiguration nodeConfig, BalancerConfiguration balancerConfig)
   {
      Connector connector = Utils.findProxyConnector(engine.getService().findConnectors());
      Map<String, String> parameters = new HashMap<String, String>();

      ProtocolHandler handler = connector.getProtocolHandler();

      boolean reverseConnection = Boolean.TRUE.equals(IntrospectionUtils.getProperty(handler, "reverseConnection"));
      boolean ssl = Boolean.TRUE.equals(IntrospectionUtils.getProperty(handler, "SSLEnabled"));
      boolean ajp = Utils.isAJP(connector);

      if (reverseConnection)
      {
         parameters.put("Reversed", "true");
      }
      parameters.put("Host", Utils.getAddress(connector));
      parameters.put("Port", "" + connector.getPort());
      parameters.put("Type", ajp ? "ajp" : (ssl ? "https" : "http"));

      // Other configuration parameters
      String domain = nodeConfig.getDomain();
      if (domain != null)
      {
         parameters.put("Domain", domain);
      }
      if (nodeConfig.getFlushPackets())
      {
         parameters.put("flushpackets", "On");
      }
      int flushWait = nodeConfig.getFlushWait();
      if (flushWait != -1)
      {
         parameters.put("flushwait", String.valueOf(flushWait));
      }
      int ping = nodeConfig.getPing();
      if (ping != -1)
      {
         parameters.put("ping", String.valueOf(ping));
      }
      int smax = nodeConfig.getSmax();
      if (smax != -1)
      {
         parameters.put("smax", String.valueOf(smax));
      }
      int ttl = nodeConfig.getTtl();
      if (ttl != -1)
      {
         parameters.put("ttl", String.valueOf(ttl));
      }
      int nodeTimeout = nodeConfig.getNodeTimeout();
      if (nodeTimeout != -1)
      {
         parameters.put("Timeout", String.valueOf(nodeTimeout));
      }
      String balancer = nodeConfig.getBalancer();
      if (balancer != null)
      {
         parameters.put("Balancer", balancer);
      }
      if (!balancerConfig.getStickySession())
      {
         parameters.put("StickySession", "No");
      }
      if (!org.apache.catalina.Globals.SESSION_COOKIE_NAME.equals("JSESSIONID"))
      {
         parameters.put("StickySessionCookie", org.apache.catalina.Globals.SESSION_COOKIE_NAME);
      }
      if (!org.apache.catalina.Globals.SESSION_PARAMETER_NAME.equals("jsessionid"))
      {
         parameters.put("StickySessionPath", org.apache.catalina.Globals.SESSION_PARAMETER_NAME);
      }
      if (balancerConfig.getStickySessionRemove())
      {
         parameters.put("StickySessionRemove", "Yes");
      }
      if (!balancerConfig.getStickySessionForce())
      {
         parameters.put("StickySessionForce", "No");
      }
      int workerTimeout = balancerConfig.getWorkerTimeout();
      if (workerTimeout != -1)
      {
         parameters.put("WaitWorker", "" + workerTimeout);
      }
      int maxAttempts = balancerConfig.getMaxAttempts();
      if (maxAttempts != -1)
      {
         parameters.put("Maxattempts", "" + maxAttempts);
      }

      return new DefaultMCMPRequest(MCMPRequestType.CONFIG, false, engine.getJvmRoute(), parameters);
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.mcmp.MCMPRequestFactory#createDisableRequest(org.apache.catalina.Context)
    */
   public MCMPRequest createDisableRequest(Context context)
   {
      return this.createRequest(MCMPRequestType.DISABLE_APP, context);
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.mcmp.MCMPRequestFactory#createDisableRequest(org.apache.catalina.Engine)
    */
   public MCMPRequest createDisableRequest(Engine engine)
   {
      return this.createRequest(MCMPRequestType.DISABLE_APP, engine);
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.mcmp.MCMPRequestFactory#createEnableRequest(org.apache.catalina.Context)
    */
   public MCMPRequest createEnableRequest(Context context)
   {
      return this.createRequest(MCMPRequestType.ENABLE_APP, context);
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.mcmp.MCMPRequestFactory#createEnableRequest(org.apache.catalina.Engine)
    */
   public MCMPRequest createEnableRequest(Engine engine)
   {
      return this.createRequest(MCMPRequestType.ENABLE_APP, engine);
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.mcmp.MCMPRequestFactory#createRemoveRequest(org.apache.catalina.Engine)
    */
   public MCMPRequest createRemoveRequest(Engine engine)
   {
      return this.createRequest(MCMPRequestType.REMOVE_APP, engine);
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.mcmp.MCMPRequestFactory#createRemoveRequest(org.apache.catalina.Context)
    */
   public MCMPRequest createRemoveRequest(Context context)
   {
      return this.createRequest(MCMPRequestType.REMOVE_APP, context);
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.mcmp.MCMPRequestFactory#createStatusRequest(java.lang.String, int)
    */
   public MCMPRequest createStatusRequest(String jvmRoute, int lbf)
   {
      return new DefaultMCMPRequest(MCMPRequestType.STATUS, false, jvmRoute, Collections.singletonMap("Load", String.valueOf(lbf)));
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.mcmp.MCMPRequestFactory#createStopRequest(org.apache.catalina.Context)
    */
   public MCMPRequest createStopRequest(Context context)
   {
      return this.createRequest(MCMPRequestType.STOP_APP, context);
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.mcmp.MCMPRequestFactory#createDumpRequest()
    */
   public MCMPRequest createDumpRequest()
   {
      return this.dumpRequest;
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.mcmp.MCMPRequestFactory#createInfoRequest()
    */
   public MCMPRequest createInfoRequest()
   {
      return this.infoRequest;
   }
   public MCMPRequest createPingRequest(String JvmRoute)
   {
      return new DefaultMCMPRequest(MCMPRequestType.PING, false, JvmRoute, EMPTY_MAP);
   }

   private MCMPRequest createRequest(MCMPRequestType type, Context context)
   {
      return this.createRequest(type, Utils.getJvmRoute(context), Utils.getAliases(context), context.getPath());
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.mcmp.MCMPRequestFactory#createRequest(org.jboss.modcluster.mcmp.MCMPRequestType, java.lang.String, java.util.Set, java.lang.String)
    */
   public MCMPRequest createRequest(MCMPRequestType type, String jvmRoute, Set<String> aliases, String path)
   {
      Map<String, String> parameters = new HashMap<String, String>();

      parameters.put("Context", "".equals(path) ? "/" : path);
      
      StringBuilder builder = new StringBuilder();
      Iterator<String> hosts = aliases.iterator();
      while (hosts.hasNext())
      {
         builder.append(hosts.next());
         if (hosts.hasNext())
         {
            builder.append(',');
         }
      }
      parameters.put("Alias", builder.toString());
      
      return new DefaultMCMPRequest(type, false, jvmRoute, parameters);
   }

   private MCMPRequest createRequest(MCMPRequestType type, Engine engine)
   {
      return this.createRequest(type, engine.getJvmRoute());
   }

   private MCMPRequest createRequest(MCMPRequestType type, String jvmRoute)
   {
      return new DefaultMCMPRequest(type, true, jvmRoute, EMPTY_MAP);
   }
}
