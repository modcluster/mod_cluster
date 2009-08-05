package org.jboss.modcluster;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.util.StringManager;
import org.apache.coyote.ProtocolHandler;
import org.apache.tomcat.util.IntrospectionUtils;
import org.jboss.logging.Logger;
import org.jboss.modcluster.mcmp.MCMPHandler;
import org.jboss.modcluster.mcmp.MCMPURLEncoder;
import org.jboss.modcluster.mcmp.impl.MCMPJBURLEncoder;
import org.jboss.modcluster.mcmp.impl.MCMPTCURLEncoder;

public class Utils
{
   private static final String ROOT_CONTEXT = "ROOT";
   private static final String CONTEXT_DELIMITER = ",";
   private static final String HOST_CONTEXT_DELIMITER = ":";
   private static final String DEFAULT_HOST = "localhost";
   private static final int DEFAULT_PORT = 8000;
   
   private static final Logger log = Logger.getLogger(Utils.class);

   private static final StringManager sm = StringManager.getManager(Constants.Package);

   private enum ServerType
   {
      JBOSSWEB, TOMCAT6
   }
   
   private static final ServerType serverType = getServerType();
   
   private static ServerType getServerType()
   {
      for (Method method: Context.class.getMethods())
      {
         if (method.getName().equals("isStarted") && (method.getParameterTypes().length == 0))
         {
            return ServerType.JBOSSWEB;
         }
      }
      
      return ServerType.TOMCAT6;
   }

   /**
    * Find the most likely connector the proxy server should connect to, or
    * accept connections from.
    * 
    * @param connectors
    * @return
    */
   public static Connector findProxyConnector(Connector[] connectors)
   {
      int highestMaxThreads = 0;
      Connector bestConnector = connectors[0];
      
      for (Connector connector: connectors)
      {
         /* Possible AJP protocol for the AJP connectors:
          * protocol="org.apache.coyote.ajp.AjpProtocol"
          * protocol="AJP/1.3"
          */
         if (isAJP(connector))
         {
            return connector;
         }
         
         ProtocolHandler handler = connector.getProtocolHandler();
         
         if (Boolean.TRUE.equals(IntrospectionUtils.getProperty(handler, "reverseConnection")))
         {
            return connector;
         }
         
         int maxThreads = ((Integer) IntrospectionUtils.getProperty(handler, "maxThreads")).intValue();
         
         if (maxThreads > highestMaxThreads)
         {
            highestMaxThreads = maxThreads;
            bestConnector = connector;
         }
      }
      
      // If no AJP connector and no reverse, return the connector with the most threads
      return bestConnector;
   }

   public static boolean isAJP(Connector connector)
   {
      String protocol = connector.getProtocol();
      
      return protocol.startsWith("AJP") || protocol.startsWith("org.apache.coyote.ajp");
   }
   
   /**
    * Return the address on which the connector is bound.
    * 
    * @param connector
    * @return
    */
   public static String getAddress(Connector connector)
   {
      Object address = IntrospectionUtils.getProperty(connector.getProtocolHandler(), "address");
      
      if (address == null) return "127.0.0.1";
      
      return (address instanceof InetAddress) ? ((InetAddress) address).getHostAddress() : (String) address;
   }

   /**
    * Return the JvmRoute for the specified context.
    * 
    * @param context
    * @return
    */
   public static String getJvmRoute(Context context)
   {
      return ((Engine) context.getParent().getParent()).getJvmRoute();
   }
   
   /**
    * Returns the aliases of the host of the specified context, including its host name
    * 
    * @param context
    * @return a set of aliases, including the host name
    */
   public static Set<String> getAliases(Context context)
   {
      return getAliases((Host) context.getParent());
   }

   /**
    * Returns the aliases of the specified host, including its name
    * 
    * @param host
    * @return a set of aliases, including the host name
    */
   public static Set<String> getAliases(Host host)
   {
      String name = host.getName();
      String[] aliases = host.findAliases();

      if (aliases.length == 0) 
      {
         return Collections.singleton(name);
      }
      
      Set<String> hosts = new LinkedHashSet<String>();
      hosts.add(name);
      hosts.addAll(Arrays.asList(aliases));
      return hosts;
   }

   /**
    * Check if the context is started.
    * 
    * @param context
    * @return true if the context is started (or if we don't know) false otherwise.
    */
   public static boolean isContextStarted(Context context)
   {
      return (serverType == ServerType.TOMCAT6) ? true : context.isStarted();
   }

   /**
    * MCMPURLEncoder factory
    * @return the appropriate MCMPURLEncoder for this server
    */
   public static MCMPURLEncoder createMCMPURLEncoder()
   {
      return (serverType == ServerType.JBOSSWEB) ? new MCMPJBURLEncoder() : new MCMPTCURLEncoder();
   }

   public static String defaultObjectNameDomain()
   {
      return (serverType == ServerType.JBOSSWEB) ? "jboss.web" : "Catalina";
   }
   
   public static void establishJvmRouteAndConnectorAddress(Engine engine, MCMPHandler mcmpHandler) throws IOException
   {
      Connector connector = findProxyConnector(engine.getService().findConnectors());
      InetAddress localAddress = (InetAddress) IntrospectionUtils.getProperty(connector.getProtocolHandler(), "address");
      if ((engine.getJvmRoute() == null || localAddress == null) && !mcmpHandler.getProxyStates().isEmpty())
      {
         // Automagical JVM route (address + port + engineName)          
         if (localAddress == null)
         {
            localAddress = mcmpHandler.getLocalAddress();
            String hostAddress = (localAddress != null) ? localAddress.getHostAddress() : "127.0.0.1";
            IntrospectionUtils.setProperty(connector.getProtocolHandler(), "address", hostAddress);
            log.info(sm.getString("modcluster.util.address", hostAddress));
         }
         if (engine.getJvmRoute() == null)
         {
            String hostName = (localAddress != null) ? localAddress.getHostName() : "127.0.0.1";
            String jvmRoute = hostName + ":" + connector.getPort() + ":" + engine.getName();
            engine.setJvmRoute(jvmRoute);
            log.info(sm.getString("modcluster.util.jvmRoute", engine.getName(), jvmRoute));
         }
      }
   }

   /**
    * Analyzes the type of the given Throwable, handing it back if it is a
    * RuntimeException, wrapping it in a RuntimeException if it is a checked
    * exception, or throwing it if it is an Error
    * 
    * @param t the throwable
    * @return a RuntimeException based on t
    * @throws Error if t is an Error
    */
   public static RuntimeException convertToUnchecked(Throwable t)
   {
      if (t instanceof Error)
      {
         throw (Error) t;
      }
      else if (t instanceof RuntimeException)
      {
         return (RuntimeException) t;
      }
      else
      {
         return new RuntimeException(t.getMessage(), t);
      }
   }

   public static InetSocketAddress parseSocketAddress(String addressPort)
   {
      try
      {
         return parseSocketAddress(addressPort, 0);
      }
      catch (UnknownHostException e)
      {
         throw new IllegalArgumentException(e);
      }
   }
   
   public static List<InetSocketAddress> parseProxies(String proxyList)
   {
      if ((proxyList == null) || (proxyList.length() == 0)) return Collections.emptyList();
      
      String[] tokens = proxyList.split(",");
      
      List<InetSocketAddress> proxies = new ArrayList<InetSocketAddress>(tokens.length);
      
      for (String token: tokens)
      {
         try
         {
            InetSocketAddress addressPort = parseSocketAddress(token.trim(), DEFAULT_PORT);
            
            proxies.add(addressPort);
         }
         catch (UnknownHostException e)
         {
            log.error(sm.getString("modcluster.error.invalidHost", token), e);
         }
      }

      return proxies;
   }

   private static InetSocketAddress parseSocketAddress(String addressPort, int defaultPort) throws UnknownHostException
   {
      int pos = addressPort.indexOf(':');
      boolean colonExists = (pos >= 0);
      
      String address = colonExists ? addressPort.substring(0, pos) : addressPort;
      int port = colonExists ? Integer.parseInt(addressPort.substring(pos + 1)) : defaultPort;
      
      InetAddress inetAddress = (address != null) && (address.length() > 0) ? InetAddress.getByName(address) : null;
      
      return new InetSocketAddress(inetAddress, port);
   }

   public static Map<String, Set<String>> parseContexts(String contexts)
   {
      if (contexts == null) return Collections.emptyMap();
      
      String trimmedContexts = contexts.trim();
      
      if (trimmedContexts.length() == 0) return Collections.emptyMap();
      
      Map<String, Set<String>> map = new HashMap<String, Set<String>>();
      
      for (String context: trimmedContexts.split(CONTEXT_DELIMITER))
      {
         String[] parts = context.trim().split(HOST_CONTEXT_DELIMITER);
         
         if (parts.length > 2)
         {
            throw new IllegalArgumentException(trimmedContexts + " is not a valid value for excludedContexts");
         }
         
         String host = DEFAULT_HOST;
         String trimmedContext = parts[0].trim();
         
         if (parts.length == 2)
         {
            host = trimmedContext;
            trimmedContext = parts[1].trim();
         }
         
         String path = trimmedContext.equals(ROOT_CONTEXT) ? "" : "/" + trimmedContext;
         
         Set<String> paths = map.get(host);
         
         if (paths == null)
         {
            paths = new HashSet<String>();
            
            map.put(host, paths);
         }
         
         paths.add(path);
      }
      
      return map;
   }
   
   private Utils()
   {
   }
}
