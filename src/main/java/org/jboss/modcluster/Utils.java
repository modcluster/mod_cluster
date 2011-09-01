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

   public static InetSocketAddress parseSocketAddress(String addressPort, int defaultPort) throws UnknownHostException
   {
      String address = addressPort;
      int port = defaultPort;
      
      int lastColon = (addressPort != null) ? addressPort.lastIndexOf(":") : -1;
      
      if (lastColon >=0)
      {
         if (addressPort.indexOf(":") == lastColon)
         {
            // Handle ipv4 address
            address = addressPort.substring(0, lastColon);
            port = Integer.parseInt(addressPort.substring(lastColon + 1));
         }
         else // handle ipv6 address
         {
            // Detect url-style: [ipv6-address]:port
            int openBracket = addressPort.indexOf("[");
            int closeBracket = addressPort.indexOf("]");
            
            if ((openBracket >= 0) && (closeBracket >= 0))
            {
               address = addressPort.substring(openBracket + 1, closeBracket);
               
               if (closeBracket < lastColon)
               {
                  port = Integer.parseInt(addressPort.substring(lastColon + 1));
               }
            }
         }
      }
      
      return new InetSocketAddress((address != null) && (address.length() > 0) ? InetAddress.getByName(address) : InetAddress.getLocalHost(), port);
   }
   
   public static List<InetSocketAddress> parseSocketAddresses(String addresses, int defaultPort)
   {
      if ((addresses == null) || (addresses.length() == 0)) return Collections.emptyList();
      
      String[] tokens = addresses.split(",");
      
      List<InetSocketAddress> proxies = new ArrayList<InetSocketAddress>(tokens.length);
      
      for (String token: tokens)
      {
         try
         {
            InetSocketAddress addressPort = parseSocketAddress(token.trim(), defaultPort);
            
            proxies.add(addressPort);
         }
         catch (UnknownHostException e)
         {
            log.error(Strings.ERROR_HOST_INVALID.getString(token), e);
         }
      }

      return proxies;
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
         
         String path = trimmedContext.equals(ROOT_CONTEXT) ? "/" : "/" + trimmedContext;
         
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
