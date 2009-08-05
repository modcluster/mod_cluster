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

package org.jboss.modcluster.demo.client.load;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Brian Stansberry
 *
 */
public enum ServerLoadServlets
{
   ACTIVE_SESSIONS("Active Sessions", 
                   "Generates server load by causing session creation on the target server.", 
                   "sessions", 
                   new ServerLoadParam("count", "Number of Sessions", 
                                       "Number of sessions to create", "20")),
                                       
   DATASOURCE_USAGE("Datasource Use", 
                    "Generates server load by taking connections from the java:DefaultDS datasource for a period", 
                    "database", 
                    new ServerLoadParam("count", "Number of Connections", 
                                        "Number of connections to request from the datasource", "20"), 
                    new ServerLoadParam("duration", "Duration", 
                                        "Number of seconds to hold the connections before returning to datasource", "15")),
                                        
   CONNECTION_POOL_USAGE("Connection Pool Use", 
                         "Generates server load by tieing up threads in the webserver connections pool for a period", 
                         "connectors", 
                         new ServerLoadParam("count", "Number of Connections", 
                                             "Number of connection pool threads to tie up", "50"), 
                         new ServerLoadParam("duration", "Duration", 
                                             "Number of seconds to tie up the connections", "15")),
                                         
   HEAP_MEMORY_USAGE("Heap Memory Use", 
                     "Generates server load by filling 50% of free heap memory for a period", 
                     "heap", 
                     new ServerLoadParam("duration", "Duration", 
                                         "Number of seconds to maintain memory usage", "15")),
                                          
   CPU_USAGE("CPU Use", 
             "Generates server CPU load by initiating a tight loop in a thread", 
             "cpu", 
             new ServerLoadParam("duration", "Duration", 
                                 "Number of seconds to maintain CPU usage", "15")),
                                 
   THREAD_USAGE("Thread Use", 
                "Generates server load by spawning threads (which do nothing but sleep)", 
                "database", 
                new ServerLoadParam("count", "Number of Threads", 
                                    "Number of threads to spawn", "50"), 
                new ServerLoadParam("duration", "Duration", 
                                    "Number of seconds threads should live before exiting", "15")),
                                          
   RECEIVE_TRAFFIC_USAGE("Server Receive Traffic", 
                         "Generates server traffic receipt load by POSTing a large byte array to the server once per second for a period", 
                         "receive", 
                         new ServerLoadParam("size", "POST size", 
                                             "Number of bytes to POST, divided by 1000", "100"), 
                         new ServerLoadParam("duration", "Duration", 
                                             "Number of seconds to continue POSTing", "15")),
                                           
   SEND_TRAFFIC_USAGE("Server Send Traffic", 
                      "Generates server traffic send load by making a request once per second to which the server responds with a large byte array", 
                      "send", 
                      new ServerLoadParam("size", "Response size", 
                                          "Size of the server response in bytes, divided by 1000", "100"), 
                      new ServerLoadParam("duration", "Duration", 
                                          "Number of seconds to continue POSTing", "15")),
                                          
   REQUEST_COUNT_USAGE("Request Count", 
                       "Generates server load by making numerous requests, increasing the request count on the target server.", 
                       "requests", 
                       new ServerLoadParam("count", "Number of Requests", 
                                           "Number of requestss to make", "50"));
   
   private final String label;
   private final String description;
   private final String servletPath;
   private final List<ServerLoadParam> params;
   
   private ServerLoadServlets(String label, String description, String servletPath, ServerLoadParam... params)
   {
      this.label = label;
      this.description = description;
      this.servletPath = servletPath;
      if (params != null)
      {
         List<ServerLoadParam> asList = Arrays.asList(params);
         this.params = Collections.unmodifiableList(asList);
      }
      else
      {
         @SuppressWarnings("unchecked")
         List<ServerLoadParam> unchecked = Collections.EMPTY_LIST;
         this.params = unchecked;
      }
   }

   public String getLabel()
   {
      return label;
   }

   public String getDescription()
   {
      return description;
   }

   public String getServletPath()
   {
      return servletPath;
   }

   public List<ServerLoadParam> getParams()
   {
      return params;
   }

   @Override
   public String toString()
   {
      return label;
   }   
   
}
