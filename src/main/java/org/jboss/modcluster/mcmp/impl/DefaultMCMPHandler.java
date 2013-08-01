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

package org.jboss.modcluster.mcmp.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.net.SocketFactory;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

import org.apache.catalina.util.StringManager;
import org.jboss.logging.Logger;
import org.jboss.modcluster.Constants;
import org.jboss.modcluster.Utils;
import org.jboss.modcluster.config.MCMPHandlerConfiguration;
import org.jboss.modcluster.mcmp.AbstractMCMPHandler;
import org.jboss.modcluster.mcmp.MCMPHandler;
import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPRequestFactory;
import org.jboss.modcluster.mcmp.MCMPServerState;
import org.jboss.modcluster.mcmp.MCMPURLEncoder;
import org.jboss.modcluster.mcmp.ResetRequestSource;

/**
 * Default implementation of {@link MCMPHandler}.
 * 
 * @author Brian Stansberry
 * @version $Revision$
 */
@ThreadSafe
public class DefaultMCMPHandler extends AbstractMCMPHandler
{
   private static final String NEW_LINE = "\r\n";
   
   protected static final Logger log = Logger.getLogger(DefaultMCMPHandler.class);
   
   /** The string manager for this package. */
   protected StringManager sm = StringManager.getManager(Constants.Package);

   // -------------------------------------------------------------- Constants

   // ----------------------------------------------------------------- Fields

   private final MCMPHandlerConfiguration config;
   /** Source for reset requests when we need to reset a proxy. */
   private final ResetRequestSource resetRequestSource;
   private final MCMPRequestFactory requestFactory;
   
   private final ReadWriteLock proxiesLock = new ReentrantReadWriteLock();
   private final Lock addRemoveProxiesLock = new ReentrantLock();
   
   /** Proxies. */
   @GuardedBy("proxiesLock")
   private final List<Proxy> proxies = new ArrayList<Proxy>();
   
   /** Add proxy list. */
   @GuardedBy("addRemoveProxiesLock")
   private final List<Proxy> addProxies = new ArrayList<Proxy>();
   
   /** Remove proxy list. */
   @GuardedBy("addRemoveProxiesLock")
   private final List<Proxy> removeProxies = new ArrayList<Proxy>();
   
   /** Initialization completion flag */
   private volatile boolean init = false;

   // -----------------------------------------------------------  Constructors

   public DefaultMCMPHandler(MCMPHandlerConfiguration config, ResetRequestSource source, MCMPRequestFactory requestFactory)
   {
      this.resetRequestSource = source;
      this.config = config;
      this.requestFactory = requestFactory;
   }

   // ------------------------------------------------------------  MCMPHandler

   public void init(List<InetSocketAddress> initialProxies)
   {
      Lock lock = this.proxiesLock.writeLock();
      lock.lock();
      
      try
      {
         if (initialProxies != null)
         {
            for (InetSocketAddress initialProxy: initialProxies)
            {
               this.addProxyInternal(initialProxy.getAddress(), initialProxy.getPort());
            }
         }
   
         this.status(false);
      }
      finally
      {
         lock.unlock();
      }
      
      this.init = true;
   }

   public void shutdown()
   {
      this.init = false;
      
      Lock lock = this.proxiesLock.readLock();
      lock.lock();
      
      try
      {
         for (Proxy proxy: this.proxies)
         {
            proxy.closeConnection();
         }
      }
      finally
      {
         lock.unlock();
      }
   }

   public void addProxy(InetAddress address, int port)
   {
      this.addProxyInternal(address, port);
   }

   private Proxy addProxyInternal(InetAddress address, int port)
   {
      Proxy proxy = new Proxy(address, port, this.config);
      
      this.addRemoveProxiesLock.lock();
      
      try
      {
         Lock lock = this.proxiesLock.readLock();
         lock.lock();
         
         try
         {
            for (Proxy candidate: this.proxies)
            {
               if (candidate.equals(proxy)) return candidate;
            }
         }
         finally
         {
            lock.unlock();
         }
         
         for (Proxy candidate: this.addProxies)
         {
            if (candidate.equals(proxy)) return candidate;
         }
         for (Proxy candidate: this.removeProxies)
         {
            if (candidate.equals(proxy)) return candidate;
         }
   
         proxy.setState(Proxy.State.ERROR);
         
         this.addProxies.add(proxy);
      }
      finally
      {
         this.addRemoveProxiesLock.unlock();
      }
      
      return proxy;
   }

   public void addProxy(InetAddress address, int port, boolean established)
   {
      Proxy proxy = this.addProxyInternal(address, port);
      proxy.setEstablished(established);
   }

   public void removeProxy(InetAddress address, int port)
   {
      Proxy proxy = new Proxy(address, port, this.config);
      
      this.addRemoveProxiesLock.lock();
      
      try
      {
         this.removeProxies.add(proxy);
      }
      finally
      {
         this.addRemoveProxiesLock.unlock();
      }
   }

   public Set<MCMPServerState> getProxyStates()
   {
      Lock lock = this.proxiesLock.readLock();
      lock.lock();
      
      try
      {
         if (this.proxies.isEmpty()) return Collections.emptySet();
   
         Set<MCMPServerState> result = new LinkedHashSet<MCMPServerState>(this.proxies.size());
         
         for (Proxy proxy: this.proxies)
         {
            result.add(new MCMPServerStateImpl(proxy));
         }
         
         return result;
      }
      finally
      {
         lock.unlock();
      }
   }

   public boolean isProxyHealthOK()
   {
      Lock lock = this.proxiesLock.readLock();
      lock.lock();
      
      try
      {
         for (Proxy proxy: this.proxies)
         {
            if (proxy.getState() != MCMPServerState.State.OK)
            {
               return false;
            }
         }
         return true;
      }
      finally
      {
         lock.unlock();
      }
   }

   public void markProxiesInError()
   {
      Lock lock = this.proxiesLock.readLock();
      lock.lock();
      
      try
      {
         for (Proxy proxy: this.proxies)
         {
            if (proxy.getState() == MCMPServerState.State.OK)
            {
               proxy.setState(Proxy.State.ERROR);
            }
         }
      }
      finally
      {
         lock.unlock();
      }
   }

   /**
    * Retrieves the full proxy configuration. To be used through JMX or similar.
    * 
    *         response: HTTP/1.1 200 OK
    *   response:
    *   node: [1:1] JVMRoute: node1 Domain: [bla] Host: 127.0.0.1 Port: 8009 Type: ajp
    *   host: 1 [] vhost: 1 node: 1
    *   context: 1 [/] vhost: 1 node: 1 status: 1
    *   context: 2 [/myapp] vhost: 1 node: 1 status: 1
    *   context: 3 [/host-manager] vhost: 1 node: 1 status: 1
    *   context: 4 [/docs] vhost: 1 node: 1 status: 1
    *   context: 5 [/manager] vhost: 1 node: 1 status: 1
    *
    * @return the proxy confguration
    */
   public String getProxyConfiguration()
   {
      // Send DUMP * request
      return this.getProxyMessage(this.requestFactory.createDumpRequest());
   }

   /**
    * Retrieves the full proxy info message.
    *
    * @return the proxy info confguration
    */
   public String getProxyInfo()
   {
      // Send INFO * request
      return this.getProxyMessage(this.requestFactory.createInfoRequest());
   }
   
   private String getProxyMessage(MCMPRequest request)
   {
      StringBuilder result = null;
      
      Lock lock = this.proxiesLock.readLock();
      lock.lock();
      
      try
      {
         for (int i = 0; i < this.proxies.size(); ++i)
         {
            Proxy proxy = this.proxies.get(i);
            String string = this.sendRequest(request, proxy);
            
            if (string != null)
            {
               if (result == null)
               {
                  result = new StringBuilder();
               }
               result.append("Proxy[").append(i).append("]: [").append(proxy.getAddress()).append(':').append(proxy.getPort()).append("]: ").append(NEW_LINE);
               result.append(string).append(NEW_LINE);
            }
         }
      }
      finally
      {
         lock.unlock();
      }
      
      return (result != null) ? result.toString() : null;
   }
   
   public InetAddress getLocalAddress() throws IOException
   {
      IOException firstException = null;
      
      Lock lock = this.proxiesLock.readLock();
      lock.lock();
      
      try
      {
         for (Proxy proxy: this.proxies)
         {
            try
            {
               return proxy.getLocalAddress();
            }
            catch (IOException e)
            {
               // Cache the exception in case no other connection works,
               // but keep trying
               if (firstException == null)
               {
                  firstException = e;
               }
            }
         }
      }
      finally
      {
         lock.unlock();
      }
      
      if (firstException != null) throw firstException;

      // We get here if there are no proxies
      return null;
   }

   /**
    * Reset a DOWN connection to the proxy up to ERROR, where the configuration will
    * be refreshed. To be used through JMX or similar.
    */
   public void reset()
   {
      Lock lock = this.proxiesLock.readLock();
      lock.lock();
      
      try
      {
         for (Proxy proxy: this.proxies)
         {
            if (proxy.getState() == Proxy.State.DOWN)
            {
               proxy.setState(Proxy.State.ERROR);
            }
         }
      }
      finally
      {
         lock.unlock();
      }
   }

   /**
    * Send a periodic status request. If in error state, the listener will attempt to refresh
    * the configuration on the front end server.
    * 
    * @param engine
    */
   public synchronized void status()
   {
      // Don't respond if not yet initialized
      if (!this.init) return;
      
      this.status(true);
   }
   
   private void status(boolean sendResetRequests)
   {
      this.processPendingDiscoveryEvents();

      Lock lock = this.proxiesLock.readLock();
      lock.lock();
      
      try
      {
         for (Proxy proxy: this.proxies)
         {
            // Attempt to reset any proxies in error         
            if (proxy.getState() == Proxy.State.ERROR)
            {
               proxy.setState(Proxy.State.OK);
   
               String response = this.sendRequest(this.requestFactory.createInfoRequest(), proxy);
   
               if (proxy.getState() == Proxy.State.OK)
               {
                  // We recovered above; if we get another IOException
                  // we should log it
                  proxy.setIoExceptionLogged(false);
   
                  if (sendResetRequests)
                  {
                     Map<String, Set<ResetRequestSource.VirtualHost>> parsedResponse = this.parseInfoResponse(response);
                     
                     List<MCMPRequest> requests = this.resetRequestSource.getResetRequests(parsedResponse);
                     
                     log.trace(requests);
                     
                     this.sendRequests(requests);
                  }
               }
               
               proxy.closeConnection();
            }
         }
      }
      finally
      {
         lock.unlock();
      }
   }

   private Map<String, Set<ResetRequestSource.VirtualHost>> parseInfoResponse(String response)
   {
      if (response == null) return Collections.emptyMap();

      log.trace(response);
      
      // Map node id -> node name (i.e. jvm route)
      Map<String, String> nodeMap = new HashMap<String, String>();
      // Map node name -> vhost id -> virtual host
      Map<String, Map<String, ResetRequestSource.VirtualHost>> virtualHostMap = new HashMap<String, Map<String, ResetRequestSource.VirtualHost>>();
      
      for (String line: response.split("\r\n|\r|\n"))
      {
         if (line.startsWith("Node:"))
         {
            String[] entries = line.split(",");
            String nodeId = this.parseIds(entries[0])[0];
            
            // We can skip the first entry
            for (int i = 1; i < entries.length; ++i)
            {
               String entry = entries[i];
               int index = entry.indexOf(':');
               
               if (index < 0)
               {
                  throw new IllegalArgumentException(response);
               }

               String key = entry.substring(0, index).trim();
               String value = entry.substring(index + 1).trim();
               
               if ("Name".equals(key))
               {
                  nodeMap.put(nodeId, value);
                  virtualHostMap.put(value, new HashMap<String, ResetRequestSource.VirtualHost>());
                  break;
               }
            }
         }
         else if (line.startsWith("Vhost:"))
         {
            String[] entries = line.split(",");
            String[] ids = this.parseIds(entries[0]);
            
            if (ids.length != 3)
            {
               throw new IllegalArgumentException(response);
            }
            
            String node = nodeMap.get(ids[0]);
            
            if (node == null)
            {
               throw new IllegalArgumentException(response);
            }
            
            Map<String, ResetRequestSource.VirtualHost> hostMap = virtualHostMap.get(node);
            String hostId = ids[1];

            ResetRequestSource.VirtualHost host = hostMap.get(hostId);

            if (host == null)
            {
               host = new VirtualHostImpl();
               hostMap.put(hostId, host);
            }
            
            for (int i = 1; i < entries.length; ++i)
            {
               String entry = entries[i];
               int index = entry.indexOf(':');
               
               if (index < 0)
               {
                  throw new IllegalArgumentException(response);
               }
               
               String key = entry.substring(0, index).trim();
               String value = entry.substring(index + 1).trim();
               
               if ("Alias".equals(key))
               {
                  host.getAliases().add(value);
                  break;
               }
            }
         }
         else if (line.startsWith("Context:"))
         {
            String[] entries = line.split(",");
            String[] ids = this.parseIds(entries[0]);
            
            if (ids.length != 3)
            {
               throw new IllegalArgumentException(response);
            }
            
            String nodeId = ids[0];
            String node = nodeMap.get(nodeId);
            
            if (node == null)
            {
               throw new IllegalArgumentException(response);
            }
            
            Map<String, ResetRequestSource.VirtualHost> hostMap = virtualHostMap.get(node);
            String hostId = ids[1];

            ResetRequestSource.VirtualHost host = hostMap.get(hostId);
            
            if (host == null)
            {
               throw new IllegalArgumentException(response);
            }
            
            String context = null;
            ResetRequestSource.Status status = null;
            
            for (int i = 1; i < entries.length; ++i)
            {
               String entry = entries[i];
               int index = entry.indexOf(':');
               
               if (index < 0)
               {
                  throw new IllegalArgumentException(response);
               }

               String key = entry.substring(0, index).trim();
               String value = entry.substring(index + 1).trim();
               
               if ("Context".equals(key))
               {
                  context = value;
               }
               else if ("Status".equals(key))
               {
                  status = ResetRequestSource.Status.valueOf(value);
               }
            }
            
            if ((context == null) || (status == null))
            {
               throw new IllegalArgumentException(response);
            }
            
            host.getContexts().put(context, status);
         }
      }
      
      Map<String, Set<ResetRequestSource.VirtualHost>> result = new HashMap<String, Set<ResetRequestSource.VirtualHost>>();
      
      for (Map.Entry<String, Map<String, ResetRequestSource.VirtualHost>> entry: virtualHostMap.entrySet())
      {
         result.put(entry.getKey(), new HashSet<ResetRequestSource.VirtualHost>(entry.getValue().values()));
      }
      
      log.trace(result);
      
      return result;
   }

   private String[] parseIds(String entry)
   {
      int start = entry.indexOf('[') + 1;
      int end = entry.indexOf(']');
      
      if (start >= end)
      {
         throw new IllegalArgumentException(entry);
      }

      String ids = entry.substring(start, end);
      
      return (ids.length() > 2) ? ids.split(":") : new String[] { ids };
   }
   
   /**
    * Send HTTP request, with the specified list of parameters. If an IO error occurs, the error state will
    * be set. If the front end server reports an error, will mark as error Proxy.State. Other unexpected exceptions
    * will be thrown and the error state will be set.
    * 
    * @param command
    * @param wildcard
    * @param parameters
    * @return the response body as a String; null if in error state or a normal error occurs
    */
   public Map<MCMPServerState, String> sendRequest(MCMPRequest request)
   {
      Map<MCMPServerState, String> map = new HashMap<MCMPServerState, String>();
      
      Lock lock = this.proxiesLock.readLock();
      lock.lock();
      
      try
      {
         for (Proxy proxy: this.proxies)
         {
            map.put(new MCMPServerStateImpl(proxy), this.sendRequest(request, proxy));
         }
      }
      finally
      {
         lock.unlock();
      }
      
      return map;
   }

   public Map<MCMPServerState, List<String>> sendRequests(List<MCMPRequest> requests)
   {
      Map<MCMPServerState, List<String>> map = new HashMap<MCMPServerState, List<String>>();
      
      Lock lock = this.proxiesLock.readLock();
      lock.lock();
      
      try
      {
         for (Proxy proxy: this.proxies)
         {
            List<String> list = new ArrayList<String>(requests.size());
            
            for (MCMPRequest request: requests)
            {
               list.add(this.sendRequest(request, proxy));
            }
            
            map.put(new MCMPServerStateImpl(proxy), list);
         }
      }
      finally
      {
         lock.unlock();
      }
      
      return map;
   }

   // ----------------------------------------------------------------  Private

   private void processPendingDiscoveryEvents()
   {
      this.addRemoveProxiesLock.lock();
      
      try
      {
         // Check to add or remove proxies, and rebuild a new list if needed
         if (!this.addProxies.isEmpty() || !this.removeProxies.isEmpty())
         {
            Lock lock = this.proxiesLock.writeLock();
            lock.lock();
            
            try
            {
               this.proxies.addAll(this.addProxies);
               this.proxies.removeAll(this.removeProxies);
      
               this.addProxies.clear();
               this.removeProxies.clear();
               
               // Reset all connections
               for (Proxy proxy: this.proxies)
               {
                  proxy.closeConnection();
               }
            }
            finally
            {
               lock.unlock();
            }
         }
      }            
      finally
      {
         this.addRemoveProxiesLock.unlock();
      }
   }

   private String sendRequest(Proxy proxy, String command, char[] body, int length) throws IOException
   {
      Writer writer = proxy.getConnectionWriter();
      
      writer.write(command);
      writer.write(NEW_LINE);
            
      writer.write("Content-Length: ");
      writer.write(Integer.toString(length));
      writer.write(NEW_LINE);
      writer.write("User-Agent: ClusterListener/1.0");
      writer.write(NEW_LINE);
      writer.write("Connection: Keep-Alive");
      writer.write(NEW_LINE);
      writer.write(NEW_LINE);
      writer.write(body, 0, length);
      writer.write(NEW_LINE);
      writer.flush();
   
      // Read the first response line and skip the rest of the HTTP header
      return proxy.getConnectionReader().readLine();
   }
   
   private String sendRequest(MCMPRequest request, Proxy proxy)
   {
      // If there was an error, do nothing until the next periodic event, where the whole configuration
      // will be refreshed
      if (proxy.getState() != Proxy.State.OK) return null;

      if (log.isTraceEnabled())
      {
         log.trace(this.sm.getString("modcluster.request", request, proxy));
      }

      String command = request.getRequestType().getCommand();
      boolean wildcard = request.isWildcard();
      String jvmRoute = request.getJvmRoute();
      Map<String, String> parameters = request.getParameters();

      MCMPURLEncoder encoder = Utils.createMCMPURLEncoder();

      // First, encode the POST body
      try
      {
         if (jvmRoute != null)
         {
            encoder.encodeParameter("JVMRoute", jvmRoute, !parameters.isEmpty());
         }
         
         Iterator<Map.Entry<String, String>> entries = parameters.entrySet().iterator();

         while (entries.hasNext())
         {
            Map.Entry<String, String> entry = entries.next();
            
            encoder.encodeParameter(entry.getKey(), entry.getValue(), entries.hasNext());
         }
      }
      catch (IOException e)
      {
         // Error encoding URL, should not happen
         throw new IllegalArgumentException(e);
      }

      // Then, connect to the proxy
      // Generate and write request
      StringBuilder builder = new StringBuilder();
      
      builder.append(command).append(" ");
      
      String proxyURL = this.config.getProxyURL();
      
      if (proxyURL != null)
      {
         builder.append(proxyURL);
      }
      
      if (builder.charAt(builder.length() - 1) != '/')
      {
         builder.append('/');
      }
      
      if (wildcard)
      {
         builder.append('*');
      }
      
      builder.append(" HTTP/1.1\r\n");
      builder.append("Host: ");

      String head = builder.toString();
      int length = encoder.getLength();
      char[] body = encoder.getBuffer();
      
      // Require exclusive access to proxy socket
      synchronized (proxy)
      {
         try
         {
            String line = null;
            StringBuilder proxyheadBuilder = new StringBuilder(head);
            proxyheadBuilder.append(proxy.getAddress().getHostName() + ":" + proxy.getPort());
            String proxyhead = proxyheadBuilder.toString();
            try
            {
               line = sendRequest(proxy, proxyhead, body, length);
            }
            catch (IOException e)
            {
               // Ignore first write failure
            }
            
            if (line == null)
            {
               // Retry failed read/write with fresh connection
               proxy.closeConnection();
               line = sendRequest(proxy, head, body, length);
            }

            BufferedReader reader = proxy.getConnectionReader();
            // Parse the line, which is formed like HTTP/1.x YYY Message
            int status = 500;
//            String version = "0";
            String message = null;
            String errorType = null;
            int contentLength = 0;
            boolean close = false;
            boolean chuncked = false;
            if (line != null)
            {
               try
               {
                  int spaceIndex = line.indexOf(' ');
                  /* Ignore everything until we have a HTTP headers */
                  while (spaceIndex == -1) {
                      line = reader.readLine();
                      if (line == null)
                          return null; // Connection closed...
                      spaceIndex = line.indexOf(' ');
                  }
                  String responseStatus = line.substring(spaceIndex + 1, line.indexOf(' ', spaceIndex + 1));
                  status = Integer.parseInt(responseStatus);
                  line = reader.readLine();
                  while ((line != null) && (line.length() > 0))
                  {
                     int colon = line.indexOf(':');
                     String headerName = line.substring(0, colon).trim();
                     String headerValue = line.substring(colon + 1).trim();
                     if ("version".equalsIgnoreCase(headerName))
                     {
//                      version = headerValue;
                     }
                     else if ("type".equalsIgnoreCase(headerName))
                     {
                        errorType = headerValue;
                     }
                     else if ("mess".equalsIgnoreCase(headerName))
                     {
                        message = headerValue;
                     }
                     else if ("content-length".equalsIgnoreCase(headerName))
                     {
                        contentLength = Integer.parseInt(headerValue);
                     }
                     else if ("connection".equalsIgnoreCase(headerName))
                     {
                        if ("close".equalsIgnoreCase(headerValue))
                           close = true;
                     } else if ("Transfer-Encoding".equalsIgnoreCase(headerName)) {
                       	if ("chunked".equalsIgnoreCase(headerValue))
                           chuncked = true;
                     }
                     line = reader.readLine();
                  }
               }
               catch (Exception e)
               {
                  log.info(this.sm.getString("modcluster.error.parse", command), e);
               }
            }
   
            // Mark as error if the front end server did not return 200; the configuration will
            // be refreshed during the next periodic event
            if (status == 200)
            {
               if (request.getRequestType().getEstablishesServer())
               {
                  // We know the request succeeded, so if appropriate
                  // mark the proxy as established before any possible
                  // later exception happens
                  proxy.setEstablished(true);
               }
            }
            else
            {
               if ("SYNTAX".equals(errorType))
               {
                  // Syntax error means the protocol is incorrect, which cannot be automatically fixed
                  proxy.setState(Proxy.State.DOWN);
                  log.error(this.sm.getString("modcluster.error.syntax", command, proxy, errorType, message));
               }
               else
               {
                  proxy.setState(Proxy.State.ERROR);
                  log.error(this.sm.getString("modcluster.error.other", command, proxy, errorType, message));
               }
            }
            
            if (close) {
                contentLength = Integer.MAX_VALUE;
            } else if (contentLength == 0 && ! chuncked) {
                return null;
            }
            
            // Read the request body
            StringBuilder result = new StringBuilder();
            char[] buffer = new char[512];

            if (chuncked) {
                boolean skipcrlf = false;
                for (;;) {
                    if (skipcrlf)
                        reader.readLine(); // Skip CRLF
                    else
                        skipcrlf = true;
                    line = reader.readLine();
                    contentLength = Integer.parseInt(line, 16);
                    if (contentLength == 0) {
                        reader.readLine(); // Skip last CRLF.
                        break;
                    }
                    while (contentLength > 0) {
                        int bytes = reader.read(buffer, 0, (contentLength > buffer.length) ? buffer.length : contentLength);
                        if (bytes <= 0)
                            break;
                        result.append(buffer, 0, bytes);
                        contentLength -= bytes;
                    }
                }
            } else {
       	        while (contentLength > 0) {
           	    int bytes = reader.read(buffer, 0, (contentLength > buffer.length) ? buffer.length : contentLength);
               
                    if (bytes <= 0) break;

                    result.append(buffer, 0, bytes);
                    contentLength -= bytes;
                }
            }

            return result.toString();
         }
         catch (IOException e)
         {
            // Most likely this is a connection error with the proxy
            proxy.setState(Proxy.State.ERROR);
   
            // Log it only if we haven't done so already. Don't spam the log
            if (proxy.isIoExceptionLogged() == false)
            {
               log.info(this.sm.getString("modcluster.error.io", command, proxy), e);
               proxy.setIoExceptionLogged(true);
            }
            
            return null;
         }
         finally
         {
            // If there's an error of any sort, or if the proxy did not return 200, it is an error
            if (proxy.getState() != Proxy.State.OK)
            {
               proxy.closeConnection();
            }
         }
      }
   }
   
   /**
    * This class represents a front-end httpd server.
    */
   @ThreadSafe
   private static class Proxy implements MCMPServerState
   {
      /** The string manager for this package. */
      private final StringManager sm = StringManager.getManager(Constants.Package);

      private final InetAddress address;
      private final int port;
      private final int socketTimeout;
      private final SocketFactory socketFactory;
      
      private volatile State state = State.OK;
      private volatile boolean established;
      private volatile boolean ioExceptionLogged;

      @GuardedBy("Proxy.this")
      private volatile Socket socket = null;
      @GuardedBy("Proxy.this")
      private volatile BufferedReader reader = null;
      @GuardedBy("Proxy.this")
      private volatile BufferedWriter writer = null;

      Proxy(InetAddress address, int port, MCMPHandlerConfiguration config)
      {
         if (address == null)
         {
            throw new IllegalArgumentException(this.sm.getString("modcluster.error.iae.null", "address"));
         }
         if (port <= 0)
         {
            throw new IllegalArgumentException(this.sm.getString("modcluster.error.iae.invalid", Integer.valueOf(port), "port"));
         }

         this.address = address;
         this.port = port;
         this.socketFactory = config.isSsl() ? new JSSESocketFactory(config) : SocketFactory.getDefault();
         this.socketTimeout = config.getSocketTimeout();
      }

      // -------------------------------------------- MCMPServerState

      public State getState()
      {
         return this.state;
      }

      // ----------------------------------------------------------- MCMPServer

      public InetAddress getAddress()
      {
         return this.address;
      }

      public int getPort()
      {
         return this.port;
      }

      public boolean isEstablished()
      {
         return this.established;
      }

      // ------------------------------------------------------------ Overrides

      @Override
      public String toString()
      {
         StringBuilder builder = new StringBuilder();
         
         if (this.address != null)
         {
            builder.append(this.address.getHostAddress());
         }
         
         return builder.append(':').append(this.port).toString();
      }

      @Override
      public boolean equals(Object object)
      {
         if (!(object instanceof Proxy)) return false;

         Proxy proxy = (Proxy) object;
         InetAddress address = proxy.address;
         
         return (this.port == proxy.port) && ((this.address != null) && (address != null) ? this.address.equals(address) : this.address == address);
      }

      @Override
      public int hashCode()
      {
         int result = 17;
         result += 23 * (this.address == null ? 0 : this.address.hashCode());
         result += 23 * this.port;
         return result;
      }

      // -------------------------------------------------------------- Private

      void setState(State state)
      {
         if (state == null)
         {
            throw new IllegalArgumentException(this.sm.getString("modcluster.error.iae.null", "state"));
         }
         
         this.state = state;
      }

      void setEstablished(boolean established)
      {
         this.established = established;
      }
      
      InetAddress getLocalAddress() throws IOException
      {
         return this.getConnection().getLocalAddress();
      }
      
      /**
       * Return a reader to the proxy.
       */
      private synchronized Socket getConnection() throws IOException
      {
         if ((this.socket == null) || this.socket.isClosed())
         {
            this.socket = this.socketFactory.createSocket();
            InetSocketAddress address = new InetSocketAddress(this.address, this.port);
            this.socket.connect(address, this.socketTimeout);
            this.socket.setSoTimeout(this.socketTimeout);
         }
         return this.socket;
      }

      /**
       * Convenience method that returns a reader to the proxy.
       */
      synchronized BufferedReader getConnectionReader() throws IOException
      {
         if (this.reader == null)
         {
            this.reader = new BufferedReader(new InputStreamReader(this.getConnection().getInputStream()));
         }
         return this.reader;
      }

      /**
       * Convenience method that returns a writer to the proxy.
       */
      synchronized BufferedWriter getConnectionWriter() throws IOException
      {
         if (this.writer == null)
         {
            this.writer = new BufferedWriter(new OutputStreamWriter(this.getConnection().getOutputStream()));
         }
         return this.writer;
      }

      /**
       * Close connection.
       */
      synchronized void closeConnection()
      {
         if (this.reader != null)
         {
            try
            {
               this.reader.close();
            }
            catch (IOException e)
            {
               // Ignore
            }
            this.reader = null;
         }
         if (this.writer != null)
         {
            try
            {
               this.writer.close();
            }
            catch (IOException e)
            {
               // Ignore
            }
            this.writer = null;
         }
         if (this.socket != null)
         {
            if (!this.socket.isClosed())
            {
               try
               {
                  this.socket.close();
               }
               catch (IOException e)
               {
                  // Ignore
               }
            }
            this.socket = null;
         }
      }

      boolean isIoExceptionLogged()
      {
         return this.ioExceptionLogged;
      }

      void setIoExceptionLogged(boolean ioErrorLogged)
      {
         this.ioExceptionLogged = ioErrorLogged;
      }
   }
   
   @Immutable
   private static class MCMPServerStateImpl implements MCMPServerState, Serializable
   {
      /** The serialVersionUID */
      private static final long serialVersionUID = 5219680414337319908L;

      private final State state;
      private final InetAddress address;
      private final int port;
      private final boolean established;

      MCMPServerStateImpl(MCMPServerState source)
      {
         this.state = source.getState();
         this.address = source.getAddress();
         this.port = source.getPort();
         this.established = source.isEstablished();
      }

      public State getState()
      {
         return this.state;
      }

      public InetAddress getAddress()
      {
         return this.address;
      }

      public int getPort()
      {
         return this.port;
      }

      public boolean isEstablished()
      {
         return this.established;
      }

      @Override
      public boolean equals(Object obj)
      {
         if (this == obj) return true;

         if (obj instanceof MCMPServerStateImpl)
         {
            MCMPServerStateImpl other = (MCMPServerStateImpl) obj;
            return (this.port == other.port && this.address.equals(other.address) && this.state == other.state && this.established == other.established);
         }
         return false;
      }

      @Override
      public int hashCode()
      {
         int result = 17;
         result += 23 * (this.address == null ? 0 : this.address.hashCode());
         result += 23 * this.port;
         result += 23 * this.state.hashCode();
         result += 23 * (this.established ? 0 : 1);
         return result;
      }

      @Override
      public String toString()
      {
         StringBuilder sb = new StringBuilder(this.getClass().getSimpleName());
         sb.append("{address=").append(this.address);
         sb.append(",port=").append(this.port);
         sb.append(",state=").append(this.state);
         sb.append(",established=").append(this.established);
         sb.append("}");
         return sb.toString();
      }
   }
   
   static class VirtualHostImpl implements ResetRequestSource.VirtualHost, Externalizable
   {
      private final Set<String> aliases = new LinkedHashSet<String>();
      private final Map<String, ResetRequestSource.Status> contexts = new HashMap<String, ResetRequestSource.Status>();
      
      public VirtualHostImpl()
      {
         // Expose for deserialization
      }
      
      /**
       * @{inheritDoc}
       * @see org.jboss.modcluster.mcmp.ResetRequestSource.VirtualHost#getAliases()
       */
      public Set<String> getAliases()
      {
         return this.aliases;
      }
      
      /**
       * @{inheritDoc}
       * @see org.jboss.modcluster.mcmp.ResetRequestSource.VirtualHost#getContexts()
       */
      public Map<String, ResetRequestSource.Status> getContexts()
      {
         return this.contexts;
      }

      /**
       * @{inheritDoc}
       * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
       */
      public void readExternal(ObjectInput input) throws IOException
      {
         int aliases = input.readInt();
         for (int i = 0; i < aliases; ++i)
         {
            this.aliases.add(input.readUTF());
         }

         ResetRequestSource.Status[] stati = ResetRequestSource.Status.values();
         int contexts = input.readInt();
         
         for (int i = 0; i < contexts; ++i)
         {
            this.contexts.put(input.readUTF(), stati[input.readInt()]);
         }
      }

      /**
       * @{inheritDoc}
       * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
       */
      public void writeExternal(ObjectOutput output) throws IOException
      {
         output.writeInt(this.aliases.size());
         
         for (String alias: this.aliases)
         {
            output.writeUTF(alias);
         }
         
         output.writeInt(this.contexts.size());
         
         for (Map.Entry<String, ResetRequestSource.Status> context: this.contexts.entrySet())
         {
            output.writeUTF(context.getKey());
            output.writeInt(context.getValue().ordinal());
         }
      }
   }
}
