/*
 *
 *  Copyright(c) 2008 Red Hat Middleware, LLC,
 *  and individual contributors as indicated by the @authors tag.
 *  See the copyright.txt in the distribution for a
 *  full listing of individual contributors.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library in the file COPYING.LIB;
 *  if not, write to the Free Software Foundation, Inc.,
 *  59 Temple Place - Suite 330, Boston, MA 02111-1307, USA
 *
 */
package org.jboss.modcluster.advertise.impl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.jcip.annotations.GuardedBy;

import org.apache.catalina.util.StringManager;
import org.jboss.logging.Logger;
import org.jboss.modcluster.Constants;
import org.jboss.modcluster.advertise.AdvertiseListener;
import org.jboss.modcluster.advertise.MulticastSocketFactory;
import org.jboss.modcluster.config.MCMPHandlerConfiguration;
import org.jboss.modcluster.mcmp.MCMPHandler;

/**
 * Listens for Advertise messages from mod_cluster
 *
 * @author Mladen Turk
 *
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
   
   int advertisePort = DEFAULT_PORT;
   InetAddress groupAddress = null;

   private MulticastSocketFactory socketFactory;
   private MulticastSocket socket;

   private boolean daemon = true;

   private String securityKey = null;
   MessageDigest md = null;

   final Map<String, AdvertisedServer> servers = new HashMap<String, AdvertisedServer>();
   final MCMPHandler commHandler;

   private AdvertiseListenerWorker workerThread;

   /** The string manager for this package. */
   private StringManager sm = StringManager.getManager(Constants.Package);

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
   public AdvertiseListenerImpl(MCMPHandler commHandler, MCMPHandlerConfiguration config, MulticastSocketFactory socketFactory)
   {
      this.commHandler = commHandler;
      this.socketFactory = socketFactory;
      
      try
      {
         this.setGroupAddress(config.getAdvertiseGroupAddress());
         this.setAdvertisePort(config.getAdvertisePort());
         this.setSecurityKey(config.getAdvertiseSecurityKey());
      }
      catch (IOException e)
      {
         log.error(this.sm.getString("modcluster.error.startListener"), e);
      }
      catch (NoSuchAlgorithmException e)
      {
         // Should never happen
         log.error(this.sm.getString("modcluster.error.startListener"), e);
      }
   }

   /**
    * The default is true - the control thread will be
    * in daemon mode. If set to false, the control thread
    * will not be daemon - and will keep the process alive.
    */
   public void setDaemon(boolean b)
   {
      this.daemon = b;
   }

   public boolean getDaemon()
   {
      return this.daemon;
   }

   /**
    * Set Advertise security key
    * @param key The key to use.<br/>
    *      Security key must match the AdvertiseKey
    *      on the advertised server.
    */
   public void setSecurityKey(String key) throws NoSuchAlgorithmException
   {
      this.securityKey = key;
      if (this.md == null)
      {
         this.md = MessageDigest.getInstance("MD5");
      }
   }

   /**
    * Set Advertise port
    * @param port The UDP port to use.
    */
   public void setAdvertisePort(int port)
   {
      this.advertisePort = (port > 0) ? port : DEFAULT_PORT;
   }

   public int getAdvertisePort()
   {
      return this.advertisePort;
   }

   /**
    * Set Advertise Multicaset group address
    * @param address The IP or host address to use.
    * @throws UnknownHostException 
    */
   public void setGroupAddress(String address) throws UnknownHostException
   {
      this.groupAddress = InetAddress.getByName((address != null) ? address : DEFAULT_GROUP);
   }

   /**
    * Get Advertise Multicaset group address
    */
   public String getGroupAddress()
   {
      return this.groupAddress.getHostAddress();
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
      
      if (this.workerThread == null)
      {
         this.workerThread = new AdvertiseListenerWorker(this.socket);
         this.workerThread.setDaemon(this.daemon);
         this.workerThread.start();
         
         this.listening = true;
      }
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.advertise.AdvertiseListener#pause()
    */
   public synchronized void pause()
   {
      if (this.workerThread != null)
      {
         this.workerThread.suspendWorker();
         this.interruptDatagramReader();
      }
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.advertise.AdvertiseListener#resume()
    */
   public synchronized void resume()
   {
      if (this.workerThread != null)
      {
         this.workerThread.resumeWorker();
      }
   }

   public synchronized void interruptDatagramReader()
   {
      if (this.socket == null) return;
      
      DatagramPacket packet = this.workerThread.createInterruptPacket(this.groupAddress, this.advertisePort);
      
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
      
      if (this.workerThread != null)
      {
         this.workerThread.interrupt();
         
         // In case worker is stuck on socket.receive(...)
         this.interruptDatagramReader();
         
         this.workerThread = null;
         
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

   // TODO This isn't correct - it always returns true!
   boolean verifyDigest(String digest, String server, String date)
   {
      if (this.md == null) return true;
      if (this.securityKey == null) return true; // Not set: No used
      
      this.md.reset();
      digestString(this.md, this.securityKey);
      digestString(this.md, date);
      digestString(this.md, server);
//      byte[] our = this.md.digest();
      byte[] dst = new byte[digest.length() * 2];
      for (int i = 0, j = 0; i < digest.length(); i++)
      {
         char ch = digest.charAt(i);
         dst[j++] = (byte) ((ch >= 'A') ? ((ch & 0xdf) - 'A') + 10 : (ch - '0'));
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
   class AdvertiseListenerWorker extends Thread
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
      @Override
      public void run()
      {
         DateFormat dateFormat = new SimpleDateFormat(RFC_822_FMT, Locale.US);
         byte[] buffer = new byte[512];
         
         // Loop until interrupted
         while (!this.isInterrupted())
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
                  if (!AdvertiseListenerImpl.this.verifyDigest(digest, server_name, date_str))
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
                        AdvertiseListenerImpl.this.commHandler.addProxy(proxy);
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
               this.interrupt();
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
