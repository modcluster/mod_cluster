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
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.jcip.annotations.GuardedBy;

import org.jboss.logging.Logger;
import org.jboss.modcluster.ModClusterLogger;
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
public class AdvertiseListenerImpl implements AdvertiseListener {
    public static final String DEFAULT_ENCODING = "8859_1";
    public static final String RFC_822_FMT = "EEE, d MMM yyyy HH:mm:ss Z";

    static final Logger log = Logger.getLogger(AdvertiseListenerImpl.class);

    volatile boolean listening = false;

    private final AdvertiseConfiguration config;
    private final MulticastSocketFactory socketFactory;

    final MessageDigest md;

    final Map<String, AdvertisedServer> servers = new ConcurrentHashMap<String, AdvertisedServer>();
    final MCMPHandler handler;

    @GuardedBy("this")
    private MulticastSocket socket;
    @GuardedBy("this")
    private AdvertiseListenerWorker worker;
    @GuardedBy("this")
    private Thread thread;

    private static void digestString(MessageDigest md, String s) {
        int len = s.length();
        byte[] b = new byte[len];
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c < 127) {
                b[i] = (byte) c;
            } else {
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
    public AdvertiseListenerImpl(MCMPHandler commHandler, AdvertiseConfiguration config, MulticastSocketFactory socketFactory) throws IOException {
        this.handler = commHandler;
        this.socketFactory = socketFactory;
        this.config = config;
        this.md = (config.getAdvertiseSecurityKey() != null) ? this.getMessageDigest() : null;
    }

    private MessageDigest getMessageDigest() throws IOException {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Get Collection of all AdvertisedServer instances.
     */
    public Collection<AdvertisedServer> getServers() {
        return this.servers.values();
    }

    /**
     * Get AdvertiseServer server.
     * 
     * @param name Server name to get.
     */
    public AdvertisedServer getServer(String name) {
        return this.servers.get(name);
    }

    /**
     * Remove the AdvertisedServer from the collection.
     * 
     * @param server Server to remove.
     */
    // TODO why is this here? it is never used.
    public void removeServer(AdvertisedServer server) {
        this.servers.values().remove(server);
    }

    private synchronized void init() throws IOException {
        if (this.socket == null) {
            InetSocketAddress socketAddress = this.config.getAdvertiseSocketAddress();
            MulticastSocket socket = this.socketFactory.createMulticastSocket(socketAddress.getAddress(), socketAddress.getPort());

            // Limit socket send to localhost
            socket.setTimeToLive(0);
            InetAddress socketInterface = this.config.getAdvertiseInterface();
            if (socketInterface != null) {
                socket.setInterface(socketInterface);
            }
            socket.joinGroup(socketAddress.getAddress());

            this.socket = socket;
        }
    }

    /**
     * @{inheritDoc
     * @see org.jboss.modcluster.advertise.AdvertiseListener#start()
     */
    @Override
    public synchronized void start() throws IOException {
        this.init();

        if (this.worker == null) {
            this.worker = new AdvertiseListenerWorker(this.socket);
            this.thread = this.config.getAdvertiseThreadFactory().newThread(this.worker);
            this.thread.start();

            this.listening = true;

            ModClusterLogger.LOGGER.startAdvertise(this.config.getAdvertiseSocketAddress());
        }
    }

    /**
     * @{inheritDoc
     * @see org.jboss.modcluster.advertise.AdvertiseListener#pause()
     */
    @Override
    public synchronized void pause() {
        if (this.worker != null) {
            this.worker.suspendWorker();
            this.interruptDatagramReader();
        }
    }

    /**
     * @{inheritDoc
     * @see org.jboss.modcluster.advertise.AdvertiseListener#resume()
     */
    @Override
    public synchronized void resume() {
        if (this.worker != null) {
            this.worker.resumeWorker();
        }
    }

    public synchronized void interruptDatagramReader() {
        if (this.socket == null)
            return;

        InetSocketAddress socketAddress = this.config.getAdvertiseSocketAddress();
        DatagramPacket packet = this.worker.createInterruptPacket(socketAddress.getAddress(), socketAddress.getPort());

        try {
            this.socket.send(packet);
        } catch (IOException e) {
            ModClusterLogger.LOGGER.socketInterruptFailed(e);
        }
    }

    /**
     * @{inheritDoc
     * @see org.jboss.modcluster.advertise.AdvertiseListener#stop()
     */
    @Override
    public synchronized void stop() {
        // In case worker is paused
        this.resume();

        if (this.thread != null) {
            this.thread.interrupt();

            // In case worker is stuck on socket.receive(...)
            this.interruptDatagramReader();

            this.thread = null;
            this.worker = null;

            this.listening = false;
        }
    }

    /**
     * @{inheritDoc
     * @see org.jboss.modcluster.advertise.AdvertiseListener#destroy()
     */
    @Override
    public synchronized void destroy() {
        // In case worker has not been stopped
        this.stop();

        if (this.socket != null) {
            try {
                this.socket.leaveGroup(this.config.getAdvertiseSocketAddress().getAddress());
            } catch (IOException e) {
                log.warn(e.getLocalizedMessage(), e);
            }

            this.socket.close();
            this.socket = null;
        }
    }

    // Check the digest, using our key and server + date.
    // digest is a hex string for httpd.
    boolean verifyDigest(String digest, String server, String date, String sequence) {
        // Neither side is configured to use digest -- pass verification
        if (this.md == null && digest == null) return true;

        // If either the digest is missing or security key is not set -- fail verification
        String securityKey = this.config.getAdvertiseSecurityKey();
        if (securityKey == null || digest == null) return false;

        this.md.reset();
        digestString(this.md, securityKey);
        byte[] ssalt = this.md.digest();
        this.md.update(ssalt);
        digestString(this.md, date);
        digestString(this.md, sequence);
        digestString(this.md, server);
        byte[] our = this.md.digest();

        if (our.length != digest.length() / 2) {
            return false;
        }

        int val = 0;
        for (int i = 0; i < digest.length(); i++) {
            int ch = digest.charAt(i);
            if (i % 2 == 0) {
                val = ((ch >= 'A') ? ((ch & 0xdf) - 'A') + 10 : (ch - '0'));
            } else {
                val = val * 16 + ((ch >= 'A') ? ((ch & 0xdf) - 'A') + 10 : (ch - '0'));

                if (our[i / 2] != (byte) val) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * True if listener is accepting the advetise messages.<br/>
     * If false it means that listener is experiencing some network problems if running.
     */
    public boolean isListening() {
        return this.listening;
    }

    // ------------------------------------ AdvertiseListenerWorker Inner Class
    class AdvertiseListenerWorker implements Runnable {
        private final MulticastSocket socket;
        @GuardedBy("this")
        private boolean paused = false;
        @GuardedBy("this")
        private byte[] secure = this.generateSecure();

        AdvertiseListenerWorker(MulticastSocket socket) {
            this.socket = socket;
        }

        public synchronized void suspendWorker() {
            this.paused = true;
        }

        public synchronized void resumeWorker() {
            if (this.paused) {
                this.paused = false;
                this.secure = this.generateSecure();
                // Notify run() thread waiting on pause
                this.notify();
            }
        }

        public synchronized DatagramPacket createInterruptPacket(InetAddress address, int port) {
            return new DatagramPacket(this.secure, this.secure.length, address, port);
        }

        /**
         * The background thread that listens for incoming Advertise packets and hands them off to an appropriate AdvertiseEvent
         * handler.
         */
        @Override
        public void run() {
            DateFormat dateFormat = new SimpleDateFormat(RFC_822_FMT, Locale.US);
            byte[] buffer = new byte[512];

            // Loop until interrupted
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    synchronized (this) {
                        if (this.paused) {
                            // Wait for notify in resumeWorker()
                            this.wait();
                        }
                    }

                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    this.socket.receive(packet);

                    // Skip processing if interrupt packet received
                    if (this.matchesSecure(packet))
                        continue;

                    String message = new String(packet.getData(), 0, packet.getLength(), DEFAULT_ENCODING);

                    if (!message.startsWith("HTTP/1."))
                        continue;

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
                    for (int i = 0; i < headers.length; i++) {
                        if (i == 0) {
                            String[] sline = headers[i].split(" ", 3);
                            if (sline == null || sline.length != 3) {
                                break;
                            }
                            status = Integer.parseInt(sline[1]);
                            if (status < 100) {
                                break;
                            }
                            status_desc = sline[2];
                        } else {
                            String[] hdrv = headers[i].split(": ", 2);
                            if (hdrv == null || hdrv.length != 2) {
                                break;
                            }
                            if (hdrv[0].equals("Date")) {
                                date_str = hdrv[1];
                                try {
                                    date = dateFormat.parse(date_str);
                                } catch (ParseException e) {
                                    date = new Date();
                                }
                            } else if (hdrv[0].equals("Digest")) {
                                digest = hdrv[1];
                            } else if (hdrv[0].equals("Sequence")) {
                                sequence = hdrv[1];
                            } else if (hdrv[0].equals("Server")) {
                                server_name = hdrv[1];
                                server = AdvertiseListenerImpl.this.servers.get(server_name);
                                if (server == null) {
                                    server = new AdvertisedServer(server_name);
                                    added = true;
                                }
                            } else if (server != null) {
                                server.setParameter(hdrv[0], hdrv[1]);
                            }
                        }
                    }
                    if (server != null && status > 0) {
                        /* We need a digest to match */
                        if (!AdvertiseListenerImpl.this.verifyDigest(digest, server_name, date_str, sequence)) {
                            continue;
                        }

                        server.setDate(date);
                        boolean rc = server.setStatus(status, status_desc);
                        if (added) {
                            AdvertiseListenerImpl.this.servers.put(server_name, server);
                            // Call the new server callback
                            // eventHandler.onEvent(AdvertiseEventType.ON_NEW_SERVER, server);
                            String proxy = server.getParameter(AdvertisedServer.MANAGER_ADDRESS);
                            if (proxy != null) {
                                AdvertiseListenerImpl.this.handler.addProxy(Utils.parseSocketAddress(proxy, 0));
                            }
                        } else if (rc) {
                            // Call the status change callback
                            // eventHandler.onEvent(AdvertiseEventType.ON_STATUS_CHANGE, server);
                        }
                    }

                    AdvertiseListenerImpl.this.listening = true;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch(InterruptedIOException e) {
                	Thread.currentThread().interrupt();
                } catch (IOException e) {
                    AdvertiseListenerImpl.this.listening = false;
                    if (this.socket == null || this.socket.isClosed())
                    	Thread.currentThread().interrupt();
                    else {

                    	// Do not blow the CPU in case of communication error
                    	Thread.yield();
                    }
                }
            }
        }

        private byte[] generateSecure() {
            SecureRandom random = new SecureRandom();

            byte[] secure = new byte[16];

            random.nextBytes(secure);
            secure[0] = 0; // why exactly?

            return secure;
        }

        synchronized boolean matchesSecure(DatagramPacket packet) {
            if (packet.getLength() != this.secure.length)
                return false;

            byte[] data = packet.getData();

            for (int i = 0; i < this.secure.length; i++) {
                if (data[i] != this.secure[i]) {
                    return false;
                }
            }

            return true;
        }
    }
}
