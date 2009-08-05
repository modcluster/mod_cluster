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
import java.util.concurrent.atomic.AtomicBoolean;

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
   
   private static final Logger log = Logger.getLogger(AdvertiseListenerImpl.class);
   private static final String RFC_822_FMT = "EEE, d MMM yyyy HH:mm:ss Z";

   private int advertisePort = DEFAULT_PORT;
   private InetAddress groupAddress = null;

   private MulticastSocketFactory socketFactory;
   MulticastSocket socket;

   private boolean initialized = false;
   volatile boolean listening = true;
   final AtomicBoolean running = new AtomicBoolean(false);
   final AtomicBoolean paused = new AtomicBoolean(false);
   private boolean daemon = true;

   private final byte[] secure = new byte[16];
   private String securityKey = null;
   MessageDigest md = null;

   final Map<String, AdvertisedServer> servers = new HashMap<String, AdvertisedServer>();
   final MCMPHandler commHandler;

   private Thread workerThread;

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
      if (!this.initialized)
      {
         this.socket = this.socketFactory.createMulticastSocket(this.groupAddress, this.advertisePort);
         
         this.socket.setTimeToLive(16);
         this.socket.joinGroup(this.groupAddress);
         
         this.initialized = true;
      }
   }

   private void interruptDatagramReader()
   {
      if (!this.initialized) return;
      
      try
      {
         // Restrict to localhost.
         this.socket.setTimeToLive(0);
         DatagramPacket dp = new DatagramPacket(this.secure, this.secure.length, this.groupAddress, this.advertisePort);
         this.socket.send(dp);
      }
      catch (IOException e)
      {
         // Ignore
      }
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.advertise.AdvertiseListener#start()
    */
   public void start() throws IOException
   {
      this.init();
      
      if (this.running.compareAndSet(false, true))
      {
         SecureRandom random = new SecureRandom();
         
         synchronized (this.secure)
         {
            random.nextBytes(this.secure);
            this.secure[0] = 0;
         }
         
         this.paused.set(false);
         this.listening = true;
         
         AdvertiseListenerWorker aw = new AdvertiseListenerWorker();
         this.workerThread = new Thread(aw);
         this.workerThread.setDaemon(this.daemon);
         this.workerThread.start();
      }
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.advertise.AdvertiseListener#pause()
    */
   public void pause()
   {
      if (this.running.get() && this.paused.compareAndSet(false, true))
      {
         this.interruptDatagramReader();
      }
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.advertise.AdvertiseListener#resume()
    */
   public void resume()
   {
      if (this.running.get() && this.paused.compareAndSet(true, false))
      {
         // Genererate new private secure
         SecureRandom random = new SecureRandom();
         
         synchronized (this.secure)
         {
            random.nextBytes(this.secure);
            this.secure[0] = 0;
         }
      }
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.advertise.AdvertiseListener#stop()
    */
   public void stop()
   {
      if (this.running.compareAndSet(true, false))
      {
         this.interruptDatagramReader();
         this.workerThread = null;
      }
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.advertise.AdvertiseListener#destroy()
    */
   public void destroy() throws IOException
   {
      this.stop();

      if (this.initialized)
      {
         this.socket.leaveGroup(this.groupAddress);
         this.socket.close();
         this.initialized = false;
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
   
   boolean matchesSecure(DatagramPacket dp)
   {
      byte[] data = dp.getData();
      
      synchronized (this.secure)
      {
         if (dp.getLength() != this.secure.length) return false;
         
         for (int i = 0; i < this.secure.length; i++)
         {
            if (data[i] != this.secure[i])
            {
               return false;
            }
         }
      }
      
      return true;
   }

   // ------------------------------------ AdvertiseListenerWorker Inner Class
   class AdvertiseListenerWorker implements Runnable
   {
      private DateFormat df = new SimpleDateFormat(RFC_822_FMT, Locale.US);
      
      /**
       * The background thread that listens for incoming Advertise packets
       * and hands them off to an appropriate AdvertiseEvent handler.
       */
      public void run()
      {
         byte[] buffer = new byte[512];
         // Loop until we receive a shutdown command
         while (AdvertiseListenerImpl.this.running.get())
         {
            // Loop if endpoint is paused
            while (AdvertiseListenerImpl.this.paused.get())
            {
               try
               {
                  Thread.sleep(1000);
               }
               catch (InterruptedException e)
               {
                  Thread.currentThread().interrupt();
               }
            }
            try
            {
               DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
               AdvertiseListenerImpl.this.socket.receive(dp);
               if (!AdvertiseListenerImpl.this.running.get())
               {
                  break;
               }
               
               if (AdvertiseListenerImpl.this.matchesSecure(dp)) continue;
               
               byte[] data = dp.getData();

               String s = new String(data, 0, dp.getLength(), DEFAULT_ENCODING);
               if (!s.startsWith("HTTP/1."))
               {
                  continue;
               }

               String[] headers = s.split("\r\n");
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
                           date = this.df.parse(date_str);
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
            catch (IOException e)
            {
               // Do not blow the CPU in case of communication error
               AdvertiseListenerImpl.this.listening = false;
               Thread.yield();
            }
         }
      }
   }

}
