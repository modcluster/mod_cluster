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

import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.jboss.modcluster.Connector;
import org.jboss.modcluster.Context;
import org.jboss.modcluster.Engine;
import org.jboss.modcluster.Host;
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
   private final MCMPRequest infoRequest = new DefaultMCMPRequest(MCMPRequestType.INFO, false, null, Collections.<String, String>emptyMap());
   private final MCMPRequest dumpRequest = new DefaultMCMPRequest(MCMPRequestType.DUMP, true, null, Collections.<String, String>emptyMap());
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.mcmp.MCMPRequestFactory#createConfigRequest(org.apache.catalina.Engine, org.jboss.modcluster.config.NodeConfiguration, org.jboss.modcluster.config.BalancerConfiguration)
    */
   public MCMPRequest createConfigRequest(Engine engine, NodeConfiguration nodeConfig, BalancerConfiguration balancerConfig)
   {
      Connector connector = engine.getProxyConnector();
      Map<String, String> parameters = new TreeMap<String, String>();

      if (connector.isReverse())
      {
         parameters.put("Reversed", "true");
      }

      parameters.put("Host", Utils.identifyHost(connector.getAddress()));
      parameters.put("Port", String.valueOf(connector.getPort()));
      parameters.put("Type", connector.getType().toString());

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

   public MCMPRequest createStopRequest(Engine engine)
   {
      return this.createRequest(MCMPRequestType.STOP_APP, engine);
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
   
   /**
    * Returns a PING MCMPRequest.
    *
    * @param JvmRoute a <code>String</code> containing
    * the name of node (JVMRoute) or an url to PING
    */
   public MCMPRequest createPingRequest(String jvmRoute)
   {
      return new DefaultMCMPRequest(MCMPRequestType.PING, false, jvmRoute, Collections.<String, String>emptyMap());
   }

   public MCMPRequest createPingRequest(URI uri)
   {
      String scheme = uri.getScheme();
      String host = uri.getHost();
      int port = uri.getPort();
      if (port < 0)
      {
         port = Connector.Type.valueOf(scheme.toUpperCase()).getDefaultPort();
      }
      
      Map<String, String> parameters = new TreeMap<String, String>();
      parameters.put("Scheme", scheme);
      parameters.put("Host", host);
      parameters.put("Port", String.valueOf(port));
      
      return new DefaultMCMPRequest(MCMPRequestType.PING, false, null, parameters);
   }

   private MCMPRequest createRequest(MCMPRequestType type, Context context)
   {
      Host host = context.getHost();
      
      return this.createContextRequest(type, host.getEngine().getJvmRoute(), host.getAliases(), context.getPath());
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.mcmp.MCMPRequestFactory#createRemoveContextRequest(java.lang.String, java.util.Set, java.lang.String)
    */
   public MCMPRequest createRemoveContextRequest(String jvmRoute, Set<String> aliases, String path)
   {
      return this.createContextRequest(MCMPRequestType.REMOVE_APP, jvmRoute, aliases, path);
   }

   private MCMPRequest createContextRequest(MCMPRequestType type, String jvmRoute, Set<String> aliases, String path)
   {
      Map<String, String> parameters = new TreeMap<String, String>();

      parameters.put("Context", (path.length() == 0) ? "/" : path);
      parameters.put("Alias", join(aliases, ','));
      
      return new DefaultMCMPRequest(type, false, jvmRoute, parameters);
   }
   
   private MCMPRequest createRequest(MCMPRequestType type, Engine engine)
   {
      return this.createEngineRequest(type, engine.getJvmRoute());
   }

   private MCMPRequest createEngineRequest(MCMPRequestType type, String jvmRoute)
   {
      return new DefaultMCMPRequest(type, true, jvmRoute, Collections.<String, String>emptyMap());
   }
   
   public MCMPRequest createRemoveEngineRequest(String jvmRoute)
   {
      return this.createEngineRequest(MCMPRequestType.REMOVE_APP, jvmRoute);
   }

   private static String join(Iterable<String> collection, char delimiter)
   {
      StringBuilder builder = new StringBuilder();
      Iterator<String> values = collection.iterator();
      if (values.hasNext())
      {
         builder.append(values.next());
      }
      while (values.hasNext())
      {
         builder.append(delimiter).append(values.next());
      }
      return builder.toString();
   }
}
