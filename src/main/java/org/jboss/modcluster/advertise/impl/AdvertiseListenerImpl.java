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
package org.jboss.modcluster.advertise.impl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadFactory;

import net.jcip.annotations.GuardedBy;

import org.jboss.logging.Logger;
import org.jboss.modcluster.Strings;
import org.jboss.modcluster.Utils;
import org.jboss.modcluster.advertise.AdvertiseListener;
import org.jboss.modcluster.advertise.MulticastSocketFactory;
import org.jboss.modcluster.config.AdvertiseConfiguration;
import org.jboss.modcluster.mcmp.MCMPHandler;

/**
 * Listens for Advertise messages from mod_cluster
 *
 * @author Mladen Turk
 */
public class AdvertiseListenerImpl implements AdvertiseListener
{
   /** Default port for listening Advertise messages. */
   public static final int DEFAULT_PORT = 23364;
   /** Default Multicast group address for listening Advertise messages. */
   public static final String DEFAULT_GROUP = "224.0.1.105";
   public static final String DEFAULT_ENCODING = "8859_1";
   public static final String RFC_822_FMT = "EEE, d MMM yyyy HH:mm:ss Z";
   
   static final Logger log = Logger.getLogger(AdvertiseListenerImpl.class);

   volatile boolean listening = false;
   
   final int advertisePort;
   final InetAddress groupAddress;
   private final InetAddress socketInterface;

   private final ThreadFactory threadFactory;
   private final MulticastSocketFactory socketFactory;
   
   private final String securityKey;
   MessageDigest md = null;

   final Map<String, AdvertisedServer> servers = new HashMap<String, AdvertisedServer>();
   final MCMPHandler handler;

   @GuardedBy("this")
   private MulticastSocket socket;
   @GuardedBy("this")
   private AdvertiseListenerWorker worker;
   @GuardedBy("this")
   private Thread thread;

   private static void digestString(MessageDigest md, String s)
   {
      int len = s.length();
      byte[] b = new byte[len];
      for (int i = 0; i < len; i++)
      {
         char c = s.charAt(i);
         if (c < 127)
         {
            b[i] = (byte) c;
         }
         else
         {
            b[i] = '?';
         }
      }
      md.update(b);
   }
   
   /**
    * Constructors a new AdvertiseListenerImpl
    * 
    * @param eventHandler The event handler that will be used for status and new server notifications.
    * @param config our configuration 
    * @param socketFactory a multicast socket factory
    */
   public AdvertiseListenerImpl(MCMPHandler commHandler, AdvertiseConfiguration config, MulticastSocketFactory socketFactory) throws IOException
   {
      this.handler = commHandler;
      this.socketFactory = socketFactory;
      this.threadFactory = config.getAdvertiseThreadFactory();
      
      String groupAddress = config.getAdvertiseGroupAddress();
      this.groupAddress = InetAddress.getByName((groupAddress != null) ? groupAddress : DEFAULT_GROUP);

      int port = config.getAdvertisePort();
      this.advertisePort = (port > 0) ? port : DEFAULT_PORT;
      
      this.securityKey = config.getAdvertiseSecurityKey();
      
      String advertiseInterface = config.getAdvertiseInterface();
      this.socketInterface = (advertiseInterface != null) ? InetAddress.getByName(advertiseInterface) : null;
   }

   /**
    * Get Collection of all AdvertisedServer instances.
    */
   public Collection<AdvertisedServer> getServers()
   {
      return this.servers.values();
   }

   /**
    * Get AdvertiseServer server.
    * @param name Server name to get.
    */
   public AdvertisedServer getServer(String name)
   {
      return this.servers.get(name);
   }

   /**
    * Remove the AdvertisedServer from the collection.
    *
    * @param server Server to remove.
    */
   // TODO why is this here?  it is never used.
   public void removeServer(AdvertisedServer server)
   {
      this.servers.values().remove(server);
   }

   private synchronized void init() throws IOException
   {
      if (this.socket == null)
      {
         MulticastSocket socket = this.socketFactory.createMulticastSocket(this.groupAddress, this.advertisePort);
         
         // Limit socket send to localhost
         socket.setTimeToLive(0);
         if (this.socketInterface != null)
         {
            socket.setInterface(this.socketInterface);
         }
         socket.joinGroup(this.groupAddress);

         this.socket = socket;
      }
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.advertise.AdvertiseListener#start()
    */
   public synchronized void start() throws IOException
   {
      this.init();
      
      if (this.worker == null)
      {
         this.worker = new AdvertiseListenerWorker(this.socket);
         this.thread = this.threadFactory.newThread(this.worker);
         this.thread.start();
         
         this.listening = true;
         
         log.info(Strings.ADVERTISE_START.getString(this.groupAddress.getHostAddress(), String.valueOf(this.advertisePort)));
      }
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.advertise.AdvertiseListener#pause()
    */
   public synchronized void pause()
   {
      if (this.worker != null)
      {
         this.worker.suspendWorker();
         this.interruptDatagramReader();
      }
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.advertise.AdvertiseListener#resume()
    */
   public synchronized void resume()
   {
      if (this.worker != null)
      {
         this.worker.resumeWorker();
      }
   }

   public synchronized void interruptDatagramReader()
   {
      if (this.socket == null) return;
      
      DatagramPacket packet = this.worker.createInterruptPacket(this.groupAddress, this.advertisePort);
      
      try
      {
         this.socket.send(packet);
      }
      catch (IOException e)
      {
         log.warn("Failed to interrupt socket reception", e);
      }
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.advertise.AdvertiseListener#stop()
    */
   public synchronized void stop()
   {
      // In case worker is paused
      this.resume();
      
      if (this.thread != null)
      {
         this.thread.interrupt();
         
         // In case worker is stuck on socket.receive(...)
         this.interruptDatagramReader();
         
         this.thread = null;
         this.worker = null;
         
         this.listening = false;
      }
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.advertise.AdvertiseListener#destroy()
    */
   public synchronized void destroy()
   {
      // In case worker has not been stopped
      this.stop();

      if (this.socket != null)
      {
         try
         {
            this.socket.leaveGroup(this.groupAddress);
         }
         catch (IOException e)
         {
            log.warn(e.getMessage(), e);
         }

         this.socket.close();
         this.socket = null;
      }
   }

   // Check the digest, using our key and server + date.
   // digest is a hex string for httpd.
   boolean verifyDigest(String digest, String server, String date, String sequence)
   {
      if (this.md == null) return true;
      if (this.securityKey == null) return true; // Not set: No used
      
      this.md.reset();
      digestString(this.md, this.securityKey);
      byte[] ssalt = this.md.digest();
      this.md.update(ssalt);
      digestString(this.md, date);
      digestString(this.md, sequence);
      digestString(this.md, server);
      byte[] our = this.md.digest();
      
      if (our.length != digest.length() / 2)
      {
         return false;
      }

      int val = 0;
      for (int i = 0; i < digest.length(); i++)
      {
         int ch = digest.charAt(i);
         if (i % 2 == 0)
         {
            val = ((ch >= 'A') ? ((ch & 0xdf) - 'A') + 10 : (ch - '0'));
         }
         else
         {
            val = val * 16 + ((ch >= 'A') ? ((ch & 0xdf) - 'A') + 10 : (ch - '0'));
            
            if (our[i / 2] != (byte) val)
            {
               return false;
            }
         }
      }
     
      return true;
   }

   /**
    * True if listener is accepting the advetise messages.<br/>
    * If false it means that listener is experiencing some
    * network problems if running.
    */
   public boolean isListening()
   {
      return this.listening;
   }

   // ------------------------------------ AdvertiseListenerWorker Inner Class
   class AdvertiseListenerWorker implements Runnable
   {
      private final MulticastSocket socket;
      @GuardedBy("this")
      private boolean paused = false;
      @GuardedBy("this")
      private byte[] secure = this.generateSecure();
      
      AdvertiseListenerWorker(MulticastSocket socket)
      {
         this.socket = socket;
      }
      
      public synchronized void suspendWorker()
      {
         this.paused = true;
      }
      
      public synchronized void resumeWorker()
      {
         if (this.paused)
         {
            this.paused = false;
            this.secure = this.generateSecure();
            // Notify run() thread waiting on pause
            this.notify();
         }
      }
      
      public synchronized DatagramPacket createInterruptPacket(InetAddress address, int port)
      {
         return new DatagramPacket(this.secure, this.secure.length, address, port);
      }
      
      /**
       * The background thread that listens for incoming Advertise packets
       * and hands them off to an appropriate AdvertiseEvent handler.
       */
      public void run()
      {
         DateFormat dateFormat = new SimpleDateFormat(RFC_822_FMT, Locale.US);
         byte[] buffer = new byte[512];
         
         // Loop until interrupted
         while (!Thread.currentThread().isInterrupted())
         {
            try
            {
               synchronized (this)
               {
                  if (this.paused)
                  {
                     // Wait for notify in resumeWorker()
                     this.wait();
                  }
               }

               DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
               
               this.socket.receive(packet);

               // Skip processing if interrupt packet received
               if (this.matchesSecure(packet)) continue;
               
               String message = new String(packet.getData(), 0, packet.getLength(), DEFAULT_ENCODING);
               
               if (!message.startsWith("HTTP/1.")) continue;

               String[] headers = message.split("\r\n");
               String date_str = null;
               Date date = null;
               int status = 0;
               String status_desc = null;
               String digest = null;
               String server_name = null;
               String sequence = null;
               AdvertisedServer server = null;
               boolean added = false;
               for (int i = 0; i < headers.length; i++)
               {
                  if (i == 0)
                  {
                     String[] sline = headers[i].split(" ", 3);
                     if (sline == null || sline.length != 3)
                     {
                        break;
                     }
                     status = Integer.parseInt(sline[1]);
                     if (status < 100)
                     {
                        break;
                     }
                     status_desc = sline[2];
                  }
                  else
                  {
                     String[] hdrv = headers[i].split(": ", 2);
                     if (hdrv == null || hdrv.length != 2)
                     {
                        break;
                     }
                     if (hdrv[0].equals("Date"))
                     {
                        date_str = hdrv[1];
                        try
                        {
                           date = dateFormat.parse(date_str);
                        }
                        catch (ParseException e)
                        {
                           date = new Date();
                        }
                     }
                     else if (hdrv[0].equals("Digest"))
                     {
                        digest = hdrv[1];
                     }
                     else if (hdrv[0].equals("Sequence"))
                     {
                        sequence = hdrv[1];
                     }
                     else if (hdrv[0].equals("Server"))
                     {
                        server_name = hdrv[1];
                        server = AdvertiseListenerImpl.this.servers.get(server_name);
                        if (server == null)
                        {
                           server = new AdvertisedServer(server_name);
                           added = true;
                        }
                     }
                     else if (server != null)
                     {
                        server.setParameter(hdrv[0], hdrv[1]);
                     }
                  }
               }
               if (server != null && status > 0)
               {
                  /* We need a digest to match */
                  if (!AdvertiseListenerImpl.this.verifyDigest(digest, server_name, date_str, sequence))
                  {
                     continue;
                  }

                  server.setDate(date);
                  boolean rc = server.setStatus(status, status_desc);
                  if (added)
                  {
                     AdvertiseListenerImpl.this.servers.put(server_name, server);
                     // Call the new server callback
                     //eventHandler.onEvent(AdvertiseEventType.ON_NEW_SERVER, server);
                     String proxy = server.getParameter(AdvertisedServer.MANAGER_ADDRESS);
                     if (proxy != null)
                     {
                        AdvertiseListenerImpl.this.handler.addProxy(Utils.parseSocketAddress(proxy));
                     }
                  }
                  else if (rc)
                  {
                     // Call the status change callback
                     //eventHandler.onEvent(AdvertiseEventType.ON_STATUS_CHANGE, server);
                  }
               }
               
               AdvertiseListenerImpl.this.listening = true;
            }
            catch (InterruptedException e)
            {
               Thread.currentThread().interrupt();
            }
            catch (IOException e)
            {
               AdvertiseListenerImpl.this.listening = false;
               
               // Do not blow the CPU in case of communication error
               Thread.yield();
            }
         }
      }

      private byte[] generateSecure()
      {
         SecureRandom random = new SecureRandom();
         
         byte[] secure = new byte[16];
         
         random.nextBytes(secure);
         secure[0] = 0; // why exactly?
         
         return secure;
      }
      
      synchronized boolean matchesSecure(DatagramPacket packet)
      {
         if (packet.getLength() != this.secure.length) return false;
         
         byte[] data = packet.getData();
         
         for (int i = 0; i < this.secure.length; i++)
         {
            if (data[i] != this.secure[i])
            {
               return false;
            }
         }
         
         return true;
      }
   }
}
