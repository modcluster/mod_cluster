/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.util.StringManager;
import org.apache.coyote.ProtocolHandler;
import org.apache.tomcat.util.IntrospectionUtils;
import org.jboss.logging.Logger;
import org.jboss.modcluster.Constants;
import org.jboss.modcluster.Utils;
import org.jboss.modcluster.config.BalancerConfiguration;
import org.jboss.modcluster.config.NodeConfiguration;
import org.jboss.modcluster.mcmp.ResetRequestSource.VirtualHost;

/**
 * Utility methods related to the Mod-Cluster Management Protocol.
 * 
 * @author Brian Stansberry
 * @version $Revision$
 */
public class MCMPUtils
{
   public static final int DEFAULT_PORT = 8000;

   private static final Map<String, String> EMPTY_MAP = Collections.emptyMap();
   public static final MCMPRequest INFO_REQUEST = new MCMPRequest(MCMPRequestType.INFO, false, null, EMPTY_MAP);
   public static final MCMPRequest DUMP_REQUEST = new MCMPRequest(MCMPRequestType.DUMP, true, null, EMPTY_MAP);

   private static final Logger log = Logger.getLogger(MCMPUtils.class);

   /**
    * The string manager for this package.
    */
   private static final StringManager sm = StringManager.getManager(Constants.Package);

   public static MCMPRequest createConfigRequest(Engine engine, NodeConfiguration nodeConfig, BalancerConfiguration balancerConfig)
   {
      return createConfigRequest(engine.getJvmRoute(), engine.getService().findConnectors(), nodeConfig, balancerConfig);
   }
   
   public static MCMPRequest createConfigRequest(String jvmRoute, Connector[] connectors, NodeConfiguration nodeConfig, BalancerConfiguration balancerConfig)
   {
      Connector connector = Utils.findProxyConnector(connectors);
      Map<String, String> parameters = new HashMap<String, String>();

      ProtocolHandler handler = connector.getProtocolHandler();

      boolean reverseConnection = Boolean.TRUE.equals(IntrospectionUtils.getProperty(handler, "reverseConnection"));
      boolean ssl = Boolean.TRUE.equals(IntrospectionUtils.getProperty(handler, "SSLEnabled"));
      boolean ajp = ((String) IntrospectionUtils.getProperty(handler, "name")).startsWith("ajp-");

      if (reverseConnection)
      {
         parameters.put("Reversed", "true");
      }
      parameters.put("Host", Utils.getAddress(connector));
      parameters.put("Port", "" + connector.getPort());
      if (ajp)
      {
         parameters.put("Type", "ajp");
      }
      else if (ssl)
      {
         parameters.put("Type", "https");
      }
      else
      {
         parameters.put("Type", "http");
      }

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

      return new MCMPRequest(MCMPRequestType.CONFIG, false, jvmRoute, parameters);
   }

   public static MCMPRequest createEnableAppRequest(Context context)
   {
      return createRequest(MCMPRequestType.ENABLE_APP, context);
   }

   public static MCMPRequest createDisableAppRequest(Context context)
   {
      return createRequest(MCMPRequestType.DISABLE_APP, context);
   }

   public static MCMPRequest createStopAppRequest(Context context)
   {
      return createRequest(MCMPRequestType.STOP_APP, context);
   }

   public static MCMPRequest createRemoveAppRequest(Context context)
   {
      return createRequest(MCMPRequestType.REMOVE_APP, context);
   }

   private static MCMPRequest createRequest(MCMPRequestType type, Context context)
   {
      return createContextRequest(type, Utils.getJvmRoute(context), Utils.getAliases(context), context.getPath());
   }
   
   private static MCMPRequest createContextRequest(MCMPRequestType type, String jvmRoute, Set<String> aliases, String path)
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
      
      return new MCMPRequest(type, false, jvmRoute, parameters);
   }
   
   public static MCMPRequest createStatusRequest(Engine engine, int lbf)
   {
      return createStatusRequest(engine.getJvmRoute(), lbf);
   }

   public static MCMPRequest createStatusRequest(String jvmRoute, int lbf)
   {
      return new MCMPRequest(MCMPRequestType.STATUS, false, jvmRoute, Collections.singletonMap("Load", String.valueOf(lbf)));
   }

   public static MCMPRequest createEnableEngineRequest(Engine engine)
   {
      return createRequest(MCMPRequestType.ENABLE_APP, engine);
   }

   public static MCMPRequest createDisableEngineRequest(Engine engine)
   {
      return createRequest(MCMPRequestType.DISABLE_APP, engine);
   }

   public static MCMPRequest createRemoveAllRequest(Engine engine)
   {
      return createRequest(MCMPRequestType.REMOVE_APP, engine);
   }

   private static MCMPRequest createRequest(MCMPRequestType type, Engine engine)
   {
      return createEngineRequest(type, engine.getJvmRoute());
   }

   private static MCMPRequest createEngineRequest(MCMPRequestType type, String jvmRoute)
   {
      return new MCMPRequest(type, true, jvmRoute, EMPTY_MAP);
   }
   
   /**
    * Reset configuration for a particular proxy following an error.
    */
   public static List<MCMPRequest> getResetRequests(Map<String, Set<ResetRequestSource.VirtualHost>> response, Server server, NodeConfiguration nodeConfig, BalancerConfiguration balancerConfig)
   {
      List<MCMPRequest> requests = new ArrayList<MCMPRequest>();
      List<MCMPRequest> engineRequests = new LinkedList<MCMPRequest>();      
      
      for (Service service: server.findServices())
      {
         Engine engine = (Engine) service.getContainer();
         String jvmRoute = engine.getJvmRoute();
         
         engineRequests.add(createConfigRequest(jvmRoute, service.findConnectors(), nodeConfig, balancerConfig));

         Set<ResetRequestSource.VirtualHost> responseHosts = Collections.emptySet();
         if (response.containsKey(jvmRoute))
         {
            responseHosts = response.get(jvmRoute);
         }
         
         for (Container child: engine.findChildren())
         {
            Host host = (Host) child;
            String hostName = host.getName();
            Set<String> aliases = Utils.getAliases(host);
            
            VirtualHost responseHost = null;
            
            for (VirtualHost virtualHost: responseHosts)
            {
               if (virtualHost.getAliases().contains(hostName))
               {
                  responseHost = virtualHost;
                  break;
               }
            }
            
            Set<String> responseAliases = Collections.emptySet();
            Map<String, ResetRequestSource.Status> responseContexts = Collections.emptyMap();
            
            if (responseHost != null)
            {
               responseAliases = responseHost.getAliases();
               
               // If the host(or aliases) is missing - force full reset
               if (!aliases.equals(responseAliases))
               {
                  engineRequests.add(0, createEngineRequest(MCMPRequestType.REMOVE_APP, jvmRoute));
               }
               else
               {
                  responseContexts = responseHost.getContexts();
               }
            }
            
            Set<String> obsoleteContexts = new HashSet<String>(responseContexts.keySet());
            
            for (Container container: host.findChildren())
            {
               Context context = (Context) container;
               String path = context.getPath();
               
               obsoleteContexts.remove(path);
               
               ResetRequestSource.Status status = responseContexts.get(path);
               
               if (Utils.isContextStarted(context))
               {
                  if (status != ResetRequestSource.Status.ENABLED)
                  {
                     engineRequests.add(createContextRequest(MCMPRequestType.ENABLE_APP, jvmRoute, aliases, path));
                  }
               }
               else
               {
                  if (status == ResetRequestSource.Status.ENABLED)
                  {
                     engineRequests.add(createContextRequest(MCMPRequestType.STOP_APP, jvmRoute, aliases, path));
                  }
               }
            }
            
            if (!obsoleteContexts.isEmpty())
            {
               // If all contexts from response no longer exist - remove all
               if (obsoleteContexts.size() == responseContexts.size())
               {
                  // Send REMOVE-APP * request first
                  engineRequests.add(0, createEngineRequest(MCMPRequestType.REMOVE_APP, jvmRoute));
               }
               // otherwise only remove those that no longer exist
               else
               {
                  for (String context: obsoleteContexts)
                  {
                     engineRequests.add(createContextRequest(MCMPRequestType.REMOVE_APP, jvmRoute, responseAliases, context));
                  }
               }
            }
         }
         
         requests.addAll(engineRequests);
         
         engineRequests.clear();
      }
      
      return requests;
   }

   public static AddressPort parseAddressPort(String addressPort)
   {
      try
      {
         return parseAddressPort(addressPort, 0);
      }
      catch (UnknownHostException e)
      {
         throw new IllegalArgumentException(e);
      }
   }
   
   public static List<AddressPort> parseProxies(String proxyList)
   {
      if (proxyList == null) return Collections.emptyList();
      
      String[] tokens = proxyList.split(",");
      
      List<AddressPort> proxies = new ArrayList<AddressPort>(tokens.length);
      
      for (String token: tokens)
      {
         try
         {
            AddressPort addressPort = parseAddressPort(token.trim(), DEFAULT_PORT);
            
            proxies.add(addressPort);
         }
         catch (UnknownHostException e)
         {
            log.error(sm.getString("modcluster.error.invalidHost", token), e);
         }
      }

      return proxies;
   }

   private static AddressPort parseAddressPort(String addressPort, int defaultPort) throws UnknownHostException
   {
      int pos = addressPort.indexOf(':');
      boolean colonExists = (pos >= 0);
      
      String address = colonExists ? addressPort.substring(0, pos) : addressPort;
      int port = colonExists ? Integer.parseInt(addressPort.substring(pos + 1)) : defaultPort;
      
      InetAddress inetAddress = (address != null) && (address.length() > 0) ? InetAddress.getByName(address) : null;
      
      return new AddressPort(inetAddress, port);
   }

   /**
    * Disable external instantiation. 
    */
   private MCMPUtils()
   {
   }
}
