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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.StandardSocketOptions;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;
import org.jboss.modcluster.ModClusterLogger;
import org.jboss.modcluster.Utils;
import org.jboss.modcluster.advertise.AdvertiseListener;
import org.jboss.modcluster.advertise.DatagramChannelFactory;
import org.jboss.modcluster.config.AdvertiseConfiguration;
import org.jboss.modcluster.config.impl.ProxyConfigurationImpl;
import org.jboss.modcluster.mcmp.MCMPHandler;

/**
 * Listens for advertise messages sent by mod_cluster reverse proxy.
 *
 * @author Mladen Turk
 * @author Radoslav Husar
 */
public class AdvertiseListenerImpl implements AdvertiseListener {
    public static final String DEFAULT_ENCODING = "8859_1";
    public static final String RFC_822_FMT = "EEE, d MMM yyyy HH:mm:ss Z";
    private DateFormat dateFormat = new SimpleDateFormat(RFC_822_FMT, Locale.US);

    private static final Logger log = Logger.getLogger(AdvertiseListenerImpl.class);

    private volatile boolean listening = false;

    private final AdvertiseConfiguration config;
    private final DatagramChannelFactory channelFactory;

    private final MessageDigest md;

    private final Map<String, AdvertisedServer> servers = new ConcurrentHashMap<>();
    private final MCMPHandler handler;

    private DatagramChannel channel;

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
     * @param commHandler    event handler that will be used for status and new server notifications
     * @param config         advertise configuration
     * @param channelFactory a multicast channel factory
     */
    public AdvertiseListenerImpl(MCMPHandler commHandler, AdvertiseConfiguration config, DatagramChannelFactory channelFactory) throws IOException {
        this.handler = commHandler;
        this.channelFactory = channelFactory;
        this.config = config;
        this.md = this.getMessageDigest();

        this.start();
    }

    private MessageDigest getMessageDigest() throws IOException {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Get AdvertiseServer server.
     *
     * @param name Server name to get.
     */
    public AdvertisedServer getServer(String name) {
        return this.servers.get(name);
    }

    private synchronized void initializeDatagramChannel() throws IOException {
        InetSocketAddress advertiseSocketAddress = this.config.getAdvertiseSocketAddress();
        DatagramChannel channel = channelFactory.createDatagramChannel(advertiseSocketAddress);

        InetAddress group = this.config.getAdvertiseSocketAddress().getAddress();

        NetworkInterface advertiseInterface = this.config.getAdvertiseInterface();
        if (advertiseInterface != null) {
            channel.setOption(StandardSocketOptions.IP_MULTICAST_IF, advertiseInterface);
            channel.join(group, advertiseInterface);
        } else {
            throw ModClusterLogger.LOGGER.noValidAdvertiseInterfaceConfigured();
        }

        this.channel = channel;
    }

    private synchronized void start() throws IOException {
        this.initializeDatagramChannel();

        final AdvertiseListenerWorker worker = new AdvertiseListenerWorker(this.channel);
        final Thread thread = this.config.getAdvertiseThreadFactory().newThread(worker);
        thread.start();

        this.listening = true;

        ModClusterLogger.LOGGER.startAdvertise(this.config.getAdvertiseSocketAddress());
    }

    /**
     * Stops the advertise listener.
     *
     * @see java.nio.channels.MulticastChannel#close()
     */
    @Override
    public void close() throws IOException {
        this.listening = false;

        this.channel.close();
    }

    // Check the digest, using our key and server + date.
    // digest is a hex string for httpd.
    private boolean verifyDigest(String digest, String server, String date, String sequence) {
        // Neither side is configured to use digest -- pass verification
        if (this.md == null && digest == null) return true;

        String securityKey = this.config.getAdvertiseSecurityKey();
        byte[] salt;

        if (securityKey == null) {
            // Security key is not configured, so the result hash was zero bytes
            salt = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        } else {
            // Use security key hash to calculate the final hash
            this.md.reset();
            digestString(this.md, securityKey);
            salt = this.md.digest();
        }

        this.md.reset();
        this.md.update(salt);

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

    @Override
    public boolean isListening() {
        return this.listening;
    }

    // ------------------------------------ AdvertiseListenerWorker Inner Class
    private class AdvertiseListenerWorker implements Runnable {
        private final DatagramChannel channel;

        AdvertiseListenerWorker(DatagramChannel channel) {
            this.channel = channel;
        }

        /**
         * The background thread that listens for incoming Advertise packets and hands them off to an appropriate AdvertiseEvent
         * handler.
         */
        @Override
        public void run() {
            ByteBuffer buffer = ByteBuffer.allocate(512);

            // Loop until channel is closed
            while (true) {
                try {
                    channel.receive(buffer);
                    flipBuffer(buffer);

                    String message = new String(buffer.array(), 0, buffer.remaining(), DEFAULT_ENCODING);

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
                            log.tracef("Advertise message digest verification failed for server %s", server_name);
                            continue;
                        }
                        log.tracef("Advertise message digest verification passed for server %s", server_name);

                        server.setDate(date);
                        server.setStatus(status, status_desc);
                        if (added) {
                            AdvertiseListenerImpl.this.servers.put(server_name, server);
                            // Call the new server callback
                            // eventHandler.onEvent(AdvertiseEventType.ON_NEW_SERVER, server);
                            String proxy = server.getParameter(AdvertisedServer.MANAGER_ADDRESS);
                            if (proxy != null) {
                                InetSocketAddress proxyAddress = Utils.parseSocketAddress(proxy, 0);
                                AdvertiseListenerImpl.this.handler.addProxy(new ProxyConfigurationImpl(proxyAddress));
                            }
                        }
                    }

                    AdvertiseListenerImpl.this.listening = true;

                } catch (ClosedChannelException e) {
                    // Channel is closed: break the loop, stop the thread
                    AdvertiseListenerImpl.this.listening = false;
                    log.trace("DatagramChannel closed");
                    return;
                } catch (IOException e) {
                    AdvertiseListenerImpl.this.listening = false;
                    if (!this.channel.isOpen()) {
                        return;
                    } else {
                        // Do not blow the CPU in case of temporary communication error
                        Thread.yield();
                    }
                } finally {
                    clearBuffer(buffer);
                }
            }
        }
    }

    /**
     * JDK compatible flip operating on {@link Buffer} instead of {@link ByteBuffer}. See MODCLUSTER-743.
     *
     * @param buffer a buffer to flip
     */
    public static void flipBuffer(Buffer buffer) {
        buffer.flip();
    }

    /**
     * JDK compatible clear operating on {@link Buffer} instead of {@link ByteBuffer}. See MODCLUSTER-743.
     *
     * @param buffer a buffer to clear
     */
    public static void clearBuffer(Buffer buffer) {
        buffer.clear();
    }

}
