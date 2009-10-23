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
package org.jboss.modcluster;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;

public class Utils
{
   private static final String ROOT_CONTEXT = "ROOT";
   private static final String CONTEXT_DELIMITER = ",";
   private static final String HOST_CONTEXT_DELIMITER = ":";
   private static final String DEFAULT_HOST = "localhost";
   private static final int DEFAULT_PORT = 8000;

   private static final Logger log = Logger.getLogger(Utils.class);
   
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
            log.error(Strings.ERROR_HOST_INVALID.getString(token), e);
         }
      }

      return proxies;
   }

   public static String identifyHost(InetAddress address)
   {
      if ((address != null) && !address.isAnyLocalAddress())
      {
         return address.getHostAddress();
      }
      
      try
      {
         return InetAddress.getLocalHost().getHostName();
      }
      catch (UnknownHostException e)
      {
         return "127.0.0.1";
      }
   }
   
   private static InetSocketAddress parseSocketAddress(String addressPort, int defaultPort) throws UnknownHostException
   {
      int colonPosition = addressPort.indexOf(':');
      boolean colonExists = (colonPosition >= 0);
      
      String address = colonExists ? addressPort.substring(0, colonPosition) : addressPort;
      int port = colonExists ? Integer.parseInt(addressPort.substring(colonPosition + 1)) : defaultPort;
      
      InetAddress inetAddress = (address != null) && (address.length() > 0) ? InetAddress.getByName(address) : InetAddress.getLocalHost();
      
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
