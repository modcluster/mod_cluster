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
package org.jboss.modcluster.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPRequestType;
import org.jboss.modcluster.mcmp.MCMPServerState;
import org.jboss.modcluster.mcmp.impl.DefaultMCMPRequest;

/**
 * Mock httpd proxy that queues all received messages
 * @author Paul Ferraro
 */
@SuppressWarnings("boxing")
public class MockProxy implements Runnable
{
   private static final String MOD_CLUSTER_SERVICE = "jboss.web:service=ModCluster";
   
   private final BlockingQueue<MCMPRequest> requests = new LinkedBlockingQueue<MCMPRequest>();
   private final int port = 6666;
   private final int socketTimeout = 10000;
   
   private ServerSocket server;
   private Thread worker;
   
   private volatile MCMPServerState.State state = MCMPServerState.State.OK;
   
   private static final Map<String, MCMPRequestType> requestTypes = new HashMap<String, MCMPRequestType>();
   {
      for (MCMPRequestType type: MCMPRequestType.values())
      {
         requestTypes.put(type.getCommand(), type);
      }
   }
   
   public void run()
   {
      try
      {
         while (!Thread.currentThread().isInterrupted())
         {
            try
            {
               Socket socket = this.server.accept();
               System.out.println("Socket connection accepted");
               socket.setSoTimeout(this.socketTimeout);
               
               try
               {
                  BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                  BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                  String line = reader.readLine();
                  
                  while ((line != null) && !Thread.currentThread().isInterrupted())
                  {
                     if (line.length() == 0)
                     {
                        line = reader.readLine();
                        continue;
                     }
                     
                     String[] parts = line.split("\\s");
                     MCMPRequestType type = requestTypes.get(parts[0]);
                     boolean wildcard = parts[1].endsWith("*");

                     line = reader.readLine();
                     
                     while ((line != null) && (line.length() > 0))
                     {
                        // Ignore headers
                        line = reader.readLine();
                     }
                     
                     if (line != null)
                     {
                        line = reader.readLine();
                     }

                     Map<String, String> parameters = new HashMap<String, String>();
                     String jvmRoute = null;
                     
                     if (line != null)
                     {
                        if (line.length() > 0)
                        {
                           for (String parameter: line.split("&"))
                           {
                              parts = parameter.split("=");
                              
                              String name = parts[0];
                              String value = URLDecoder.decode(parts[1], "UTF-8");
                              
                              if (name.equals("JVMRoute"))
                              {
                                 jvmRoute = value;
                              }
                              else
                              {
                                 parameters.put(name, value);
                              }
                           }
                        }
                        
                        this.requests.add(new DefaultMCMPRequest(type, wildcard, jvmRoute, parameters));
                        
                        if (this.state == MCMPServerState.State.OK)
                        {
                           writer.write("HTTP/1.0 200 OK");
                        }
                        else
                        {
                           writer.write("HTTP/1.0 500 ERROR");
                           
                           if (this.state == MCMPServerState.State.DOWN)
                           {
                              writer.newLine();
                              writer.write("Type: SYNTAX");
                           }
                        }
                        
                        writer.newLine();
                        writer.newLine();
                        writer.flush();
                        
                        line = reader.readLine();
                     }
                  }
               }
               finally
               {
                  socket.close();
               }
            }
            catch (SocketTimeoutException e)
            {
               System.out.println("Socket accept timeout");
            }
         }
      }
      catch (Throwable e)
      {
         e.printStackTrace(System.err);
      }
   }
   
   public void setState(MCMPServerState.State state)
   {
      this.state = state;
   }
   
   public BlockingQueue<MCMPRequest> getRequests()
   {
      return this.requests;
   }
   
   public void start() throws Exception
   {
      this.server = new ServerSocket(this.port, 50, null);
      this.server.setSoTimeout(this.socketTimeout);
      
      this.worker = new Thread(this);
      this.worker.start();
      
      MBeanServerConnection server = new MBeanServerConnector(0).getServer();
      
      ObjectName name = ObjectName.getInstance(MOD_CLUSTER_SERVICE);
      
      String host = InetAddress.getLocalHost().getHostName();
      int port = this.server.getLocalPort();
      
      server.invoke(name, "addProxy", new Object[] { host, port }, new String[] { String.class.getName(), Integer.TYPE.getName() });
   }
   
   public void close() throws IOException
   {
      System.out.println("Begin close " + new java.util.Date());
      if (this.worker != null) this.worker.interrupt();
      
      // Break ServerSocket.accept()
      if (this.server != null) this.server.close();
      
      try
      {
         // Wait for worker to finish
         this.worker.join();
      }
      catch (InterruptedException e)
      {
         Thread.currentThread().interrupt();
      }
      
      this.requests.clear();
      System.out.println("End close " + new java.util.Date());
   }
}