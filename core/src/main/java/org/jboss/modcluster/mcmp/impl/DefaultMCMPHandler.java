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
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.net.SocketFactory;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import org.jboss.logging.Logger;
import org.jboss.modcluster.ModClusterLogger;
import org.jboss.modcluster.config.MCMPHandlerConfiguration;
import org.jboss.modcluster.config.ProxyConfiguration;
import org.jboss.modcluster.mcmp.MCMPConnectionListener;
import org.jboss.modcluster.mcmp.MCMPHandler;
import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPRequestFactory;
import org.jboss.modcluster.mcmp.MCMPRequestType;
import org.jboss.modcluster.mcmp.MCMPResponseParser;
import org.jboss.modcluster.mcmp.MCMPServer;
import org.jboss.modcluster.mcmp.MCMPServerState;
import org.jboss.modcluster.mcmp.MCMPServerState.State;
import org.jboss.modcluster.mcmp.ResetRequestSource;

/**
 * Default implementation of {@link MCMPHandler}.
 *
 * @author Jean-Frederic Clere
 * @author Brian Stansberry
 * @author Paul Ferraro
 */
@ThreadSafe
public class DefaultMCMPHandler implements MCMPHandler {
    private static final String NEW_LINE = "\r\n";

    static final Logger log = Logger.getLogger(DefaultMCMPHandler.class);

    // -------------------------------------------------------------- Constants

    // ----------------------------------------------------------------- Fields

    private final MCMPHandlerConfiguration config;
    /** Source for reset requests when we need to reset a proxy. */
    private final ResetRequestSource resetRequestSource;
    private final MCMPRequestFactory requestFactory;
    private final MCMPResponseParser responseParser;

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

    private final AtomicBoolean established = new AtomicBoolean(false);
    private volatile MCMPConnectionListener connectionListener;
    private volatile boolean init = false;

    // ----------------------------------------------------------- Constructors

    public DefaultMCMPHandler(MCMPHandlerConfiguration config, ResetRequestSource source, MCMPRequestFactory requestFactory,
            MCMPResponseParser responseParser) {
        this.resetRequestSource = source;
        this.config = config;
        this.requestFactory = requestFactory;
        this.responseParser = responseParser;
    }

    // ------------------------------------------------------------ MCMPHandler

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.modcluster.mcmp.MCMPHandler#init(java.util.List)
     */
    @Override
    public void init(Collection<ProxyConfiguration> proxies, MCMPConnectionListener connectionListener) {
        this.connectionListener = connectionListener;

        if (proxies != null) {
            Lock lock = this.proxiesLock.writeLock();
            lock.lock();

            try {
                for (final ProxyConfiguration proxy : proxies) {
                    this.add(proxy.getRemoteAddress(), proxy.getLocalAddress());
                }

                this.status(false);
            } finally {
                lock.unlock();
            }
        }

        this.init = true;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.modcluster.mcmp.MCMPHandler#shutdown()
     */
    @Override
    public void shutdown() {
        this.init = false;

        Lock lock = this.proxiesLock.readLock();
        lock.lock();

        try {
            for (Proxy proxy : this.proxies) {
                proxy.closeConnection();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.modcluster.mcmp.MCMPHandler#addProxy(java.net.InetSocketAddress)
     */
    @Override
    public void addProxy(InetSocketAddress socketAddress) {
        this.add(socketAddress);
    }

    @Override
    public void addProxy(ProxyConfiguration proxyConfiguration) {
        this.add(proxyConfiguration.getRemoteAddress(), proxyConfiguration.getLocalAddress());
    }

    private Proxy add(InetSocketAddress socketAddress) {
        return this.add(socketAddress, null);
    }

    private Proxy add(InetSocketAddress socketAddress, InetSocketAddress localAddress) {
        Proxy proxy = new Proxy(socketAddress, localAddress, this.config);

        this.addRemoveProxiesLock.lock();

        try {
            Lock lock = this.proxiesLock.readLock();
            lock.lock();

            try {
                for (Proxy candidate : this.proxies) {
                    if (candidate.equals(proxy))
                        return candidate;
                }
            } finally {
                lock.unlock();
            }

            for (Proxy candidate : this.addProxies) {
                if (candidate.equals(proxy))
                    return candidate;
            }
            for (Proxy candidate : this.removeProxies) {
                if (candidate.equals(proxy))
                    return candidate;
            }

            proxy.setState(Proxy.State.ERROR);

            this.addProxies.add(proxy);
        } finally {
            this.addRemoveProxiesLock.unlock();
        }

        return proxy;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.modcluster.mcmp.MCMPHandler#addProxy(java.net.InetSocketAddress, boolean)
     */
    @Override
    public void addProxy(InetSocketAddress socketAddress, boolean established) {
        this.add(socketAddress).setEstablished(established);
    }

    @Override
    public void addProxy(ProxyConfiguration proxyConfiguration, boolean established) {
        this.add(proxyConfiguration.getRemoteAddress(), proxyConfiguration.getLocalAddress()).setEstablished(established);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.modcluster.mcmp.MCMPHandler#removeProxy(java.net.InetSocketAddress)
     */
    @Override
    public void removeProxy(InetSocketAddress socketAddress) {
        Proxy proxy = new Proxy(socketAddress, this.config);

        this.addRemoveProxiesLock.lock();

        try {
            this.removeProxies.add(proxy);
        } finally {
            this.addRemoveProxiesLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.modcluster.mcmp.MCMPHandler#getProxyStates()
     */
    @Override
    public Set<MCMPServerState> getProxyStates() {
        Lock lock = this.proxiesLock.readLock();
        lock.lock();

        try {
            if (this.proxies.isEmpty())
                return Collections.emptySet();

            Set<MCMPServerState> result = new LinkedHashSet<MCMPServerState>(this.proxies.size());

            for (Proxy proxy : this.proxies) {
                result.add(proxy);
            }

            return result;
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.modcluster.mcmp.MCMPHandler#isProxyHealthOK()
     */
    @Override
    public boolean isProxyHealthOK() {
        Lock lock = this.proxiesLock.readLock();
        lock.lock();

        try {
            for (Proxy proxy : this.proxies) {
                if (proxy.getState() != MCMPServerState.State.OK) {
                    return false;
                }
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.modcluster.mcmp.MCMPHandler#markProxiesInError()
     */
    @Override
    public void markProxiesInError() {
        Lock lock = this.proxiesLock.readLock();
        lock.lock();

        try {
            for (Proxy proxy : this.proxies) {
                if (proxy.getState() == MCMPServerState.State.OK) {
                    proxy.setState(Proxy.State.ERROR);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.modcluster.mcmp.MCMPHandler#reset()
     */
    @Override
    public void reset() {
        Lock lock = this.proxiesLock.readLock();
        lock.lock();

        try {
            for (Proxy proxy : this.proxies) {
                if (proxy.getState() == Proxy.State.DOWN) {
                    proxy.setState(Proxy.State.ERROR);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.modcluster.mcmp.MCMPHandler#status()
     */
    @Override
    public synchronized void status() {
        if (this.init) {
            this.processPendingDiscoveryEvents();

            this.status(true);
        }
    }

    /**
     * Send a periodic status request.
     *
     * @param sendResetRequests if enabled, when in error state, the listener will attempt to refresh the configuration on the
     *        front end server
     */
    private void status(boolean sendResetRequests) {
        Lock lock = this.proxiesLock.readLock();
        lock.lock();

        try {
            for (Proxy proxy : this.proxies) {
                // Attempt to reset any proxies in error
                if (proxy.getState() == Proxy.State.ERROR) {

                    proxy.closeConnection();
                    proxy.setState(Proxy.State.OK);

                    String response = this.sendRequest(this.requestFactory.createInfoRequest(), proxy);

                    if (proxy.getState() == Proxy.State.OK) {
                        // Only notify connection listener once
                        if (this.established.compareAndSet(false, true)) {
                            this.connectionListener.connectionEstablished(proxy.getLocalAddress());
                        }

                        if (sendResetRequests) {
                            Map<String, Set<ResetRequestSource.VirtualHost>> parsedResponse = this.responseParser
                                    .parseInfoResponse(response);

                            List<MCMPRequest> requests = this.resetRequestSource.getResetRequests(parsedResponse);

                            log.trace(requests);

                            this.sendRequestsToProxy(requests, proxy);
                        }
                    } else {
                        proxy.closeConnection();
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.modcluster.mcmp.MCMPHandler#sendRequest(org.jboss.modcluster.mcmp.MCMPRequest)
     */
    @Override
    public Map<MCMPServerState, String> sendRequest(MCMPRequest request) {
        Map<MCMPServerState, String> map = new HashMap<MCMPServerState, String>();

        Lock lock = this.proxiesLock.readLock();
        lock.lock();

        try {
            for (Proxy proxy : this.proxies) {
                map.put(proxy, this.sendRequest(request, proxy));
            }
        } finally {
            lock.unlock();
        }

        return map;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.modcluster.mcmp.MCMPHandler#sendRequests(java.util.List)
     */
    @Override
    public Map<MCMPServerState, List<String>> sendRequests(List<MCMPRequest> requests) {
        Map<MCMPServerState, List<String>> map = new HashMap<MCMPServerState, List<String>>();

        Lock lock = this.proxiesLock.readLock();
        lock.lock();

        try {
            for (Proxy proxy : this.proxies) {
                List<String> list = new ArrayList<String>(requests.size());

                for (MCMPRequest request : requests) {
                    list.add(this.sendRequest(request, proxy));
                }

                map.put(proxy, list);
            }
        } finally {
            lock.unlock();
        }

        return map;
    }

    /**
     * Like sendRequests, but only to a given Proxy, to be used when sending reset requests.
     *
     * @param requests list of MCMP requests to send
     * @param proxy    The designated proxy to send the requests to
     */
    private List<String> sendRequestsToProxy(List<MCMPRequest> requests, Proxy proxy) {
        List<String> list = new ArrayList<String>(requests.size());
        for (MCMPRequest request : requests) {
            list.add(this.sendRequest(request, proxy));
        }

        return list;
    }

    // ---------------------------------------------------------------- Private

    private void processPendingDiscoveryEvents() {
        this.addRemoveProxiesLock.lock();

        try {
            // Check to add or remove proxies, and rebuild a new list if needed
            if (!this.addProxies.isEmpty() || !this.removeProxies.isEmpty()) {
                Lock lock = this.proxiesLock.writeLock();
                lock.lock();

                try {
                    this.proxies.addAll(this.addProxies);
                    this.proxies.removeAll(this.removeProxies);

                    this.addProxies.clear();
                    this.removeProxies.clear();

                    // Reset all connections
                    for (Proxy proxy : this.proxies) {
                        proxy.closeConnection();
                    }
                } finally {
                    lock.unlock();
                }
            }
        } finally {
            this.addRemoveProxiesLock.unlock();
        }
    }

    private String sendRequest(Proxy proxy, String command, String body) throws IOException {
        Writer writer = proxy.getConnectionWriter();

        writer.append(command).append(NEW_LINE);

        if (body.length()>0)
            writer.append("Content-Length: ").append(String.valueOf(body.length())).append(NEW_LINE);
        writer.append("User-Agent: ClusterListener/1.0").append(NEW_LINE);
        writer.append("Connection: Keep-Alive").append(NEW_LINE);
        writer.write(NEW_LINE);
        if (body.length()>0) {
            writer.write(body);
        }
        writer.flush();

        // Read the first response line and skip the rest of the HTTP header
        return proxy.getConnectionReader().readLine();
    }

    private void appendParameter(Appendable appender, String name, String value, boolean more) throws IOException {
        appender.append(URLEncoder.encode(name, "UTF-8")).append('=').append(URLEncoder.encode(value, "UTF-8"));

        if (more) {
            appender.append('&');
        }
    }

    private String sendRequest(MCMPRequest request, Proxy proxy) {
        // If there was an error, do nothing until the next periodic event, where the whole configuration
        // will be refreshed
        if (proxy.getState() != Proxy.State.OK)
            return null;

        log.tracef("Sending to %s: %s", proxy, request);

        MCMPRequestType requestType = request.getRequestType();
        boolean wildcard = request.isWildcard();
        String jvmRoute = request.getJvmRoute();
        Map<String, String> parameters = request.getParameters();

        StringBuilder bodyBuilder = new StringBuilder();

        // First, encode the POST body
        try {
            if (jvmRoute != null) {
                this.appendParameter(bodyBuilder, "JVMRoute", jvmRoute, !parameters.isEmpty());
            }

            Iterator<Map.Entry<String, String>> entries = parameters.entrySet().iterator();

            while (entries.hasNext()) {
                Map.Entry<String, String> entry = entries.next();

                this.appendParameter(bodyBuilder, entry.getKey(), entry.getValue(), entries.hasNext());
            }
        } catch (IOException e) {
            // Error encoding URL, should not happen
            throw new IllegalArgumentException(e);
        }

        // Then, connect to the proxy
        // Generate and write request
        StringBuilder headBuilder = new StringBuilder();

        headBuilder.append(requestType).append(" ");

        String proxyURL = this.config.getProxyURL();

        if (proxyURL != null) {
            headBuilder.append(proxyURL);
        }

        if (headBuilder.charAt(headBuilder.length() - 1) != '/') {
            headBuilder.append('/');
        }

        if (wildcard) {
            headBuilder.append('*');
        }

        headBuilder.append(" HTTP/1.1\r\n");
        headBuilder.append("Host: ");

        String head = headBuilder.toString();
        String body = bodyBuilder.toString();

        // Require exclusive access to proxy socket
        synchronized (proxy) {
            try {
                String line = null;
                String host = proxy.getSocketAddress().getHostString();
                String proxyhead;

                if(host != null && host.contains(":")) {
                    proxyhead = head + "[" + host + "]:" + proxy.getSocketAddress().getPort();
                } else {
                    proxyhead = head + host + ":" + proxy.getSocketAddress().getPort();
                }

                try {
                    line = sendRequest(proxy, proxyhead, body);
                } catch (IOException e) {
                    // Ignore first write failure
                }

                if (line == null) {
                    // Retry failed read/write with fresh connection
                    proxy.closeConnection();
                    line = sendRequest(proxy, proxyhead, body);
                }

                BufferedReader reader = proxy.getConnectionReader();
                // Parse the line, which is formed like HTTP/1.x YYY Message
                int status = 500;
                // String version = "0";
                String message = null;
                String errorType = null;
                int contentLength = 0;
                boolean close = false;
                boolean chuncked = false;
                if (line != null) {
                    try {
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
                        while ((line != null) && (line.length() > 0)) {
                            int colon = line.indexOf(':');
                            String headerName = line.substring(0, colon).trim();
                            String headerValue = line.substring(colon + 1).trim();
                            if ("version".equalsIgnoreCase(headerName)) {
                                // version = headerValue;
                            } else if ("type".equalsIgnoreCase(headerName)) {
                                errorType = headerValue;
                            } else if ("mess".equalsIgnoreCase(headerName)) {
                                message = headerValue;
                            } else if ("content-length".equalsIgnoreCase(headerName)) {
                                contentLength = Integer.parseInt(headerValue);
                            } else if ("connection".equalsIgnoreCase(headerName)) {
                                close = "close".equalsIgnoreCase(headerValue);
                            } else if ("Transfer-Encoding".equalsIgnoreCase(headerName)) {
                                if ("chunked".equalsIgnoreCase(headerValue))
                                    chuncked = true;
                            }
                            line = reader.readLine();
                        }
                    } catch (Exception e) {
                        ModClusterLogger.LOGGER.parseHeaderFailed(e, requestType, proxy.getSocketAddress());
                    }
                }

                // Mark as error if the front end server did not return 200; the configuration will
                // be refreshed during the next periodic event
                if (status == 200) {
                    if (request.getRequestType().getEstablishesServer()) {
                        // We know the request succeeded, so if appropriate
                        // mark the proxy as established before any possible
                        // later exception happens
                        proxy.setEstablished(true);
                    }
                } else {
                    if ("SYNTAX".equals(errorType)) {
                        // Syntax error means the protocol is incorrect, which cannot be automatically fixed
                        proxy.setState(Proxy.State.DOWN);
                        ModClusterLogger.LOGGER.unrecoverableErrorResponse(errorType, requestType, proxy.getSocketAddress(), message);
                    } else {
                        proxy.setState(Proxy.State.ERROR);
                        ModClusterLogger.LOGGER.recoverableErrorResponse(errorType, requestType, proxy.getSocketAddress(), message);
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

                        if (bytes <= 0)
                            break;

                        result.append(buffer, 0, bytes);
                        contentLength -= bytes;
                    }
                }

                if (proxy.getState() == State.OK) {
                    proxy.setIoExceptionLogged(false);
                }

                return result.toString();
            } catch (IOException e) {
                // Most likely this is a connection error with the proxy
                proxy.setState(Proxy.State.ERROR);

                // Log it only if we haven't done so already. Don't spam the log
                if (!proxy.isIoExceptionLogged()) {
                    ModClusterLogger.LOGGER.sendFailed(requestType, proxy.getSocketAddress(), e.getLocalizedMessage());
                    ModClusterLogger.LOGGER.catchingDebug(e);
                    proxy.setIoExceptionLogged(true);
                }

                return null;
            } finally {
                // If there's an error of any sort, or if the proxy did not return 200, it is an error
                if (proxy.getState() != Proxy.State.OK) {
                    proxy.closeConnection();
                }
            }
        }
    }

    /**
     * This class represents a front-end httpd server.
     */
    @ThreadSafe
    private static class Proxy implements MCMPServerState, Serializable {
        /** The serialVersionUID */
        private static final long serialVersionUID = 5219680414337319908L;

        private final InetSocketAddress socketAddress;
        private final InetSocketAddress sourceAddress;

        private volatile State state = State.OK;
        private volatile boolean established = false;

        private final transient int socketTimeout;
        private final transient SocketFactory socketFactory;

        private transient volatile boolean ioExceptionLogged = false;
        private transient volatile InetAddress localAddress = null;

        @GuardedBy("Proxy.this")
        private transient volatile Socket socket = null;
        @GuardedBy("Proxy.this")
        private transient volatile BufferedReader reader = null;
        @GuardedBy("Proxy.this")
        private transient volatile BufferedWriter writer = null;

        Proxy(InetSocketAddress socketAddress, MCMPHandlerConfiguration config) {
            this(socketAddress, null, config);
        }

        Proxy(InetSocketAddress socketAddress, InetSocketAddress sourceAddress, MCMPHandlerConfiguration config) {
            this.socketAddress = socketAddress;
            this.sourceAddress = sourceAddress;
            this.socketFactory = config.getSocketFactory();
            this.socketTimeout = config.getSocketTimeout();
        }

        // -------------------------------------------- MCMPServerState

        @Override
        public State getState() {
            return this.state;
        }

        // ----------------------------------------------------------- MCMPServer

        @Override
        public InetSocketAddress getSocketAddress() {
            return this.socketAddress;
        }

        @Override
        public boolean isEstablished() {
            return this.established;
        }

        // ------------------------------------------------------------ Overrides

        @Override
        public String toString() {
            return this.socketAddress.toString();
        }

        @Override
        public boolean equals(Object object) {
            if ((object == null) || !(object instanceof MCMPServer))
                return false;

            MCMPServer proxy = (MCMPServer) object;

            return this.socketAddress.equals(proxy.getSocketAddress());
        }

        @Override
        public int hashCode() {
            return this.socketAddress.hashCode();
        }

        // -------------------------------------------------------------- Private

        void setState(State state) {
            this.state = state;
        }

        void setEstablished(boolean established) {
            this.established = established;
        }

        /**
         * Return a reader to the proxy.
         */
        private synchronized Socket getConnection() throws IOException {
            if (this.socket == null || this.socket.isClosed()) {
                this.socket = this.socketFactory.createSocket();
                InetAddress address = this.socketAddress.getAddress();
                if (sourceAddress != null) {
                    // If using a specific port enable SO_REUSEADDR to avoid "Address already in use" errors
                    if (sourceAddress.getPort() != 0) {
                        this.socket.setReuseAddress(true);
                    }
                    // If bind address is specified for the proxy, use it
                    this.socket.bind(sourceAddress);
                } else if (address instanceof Inet6Address && address.isLinkLocalAddress()) {
                    // If the bind address is unspecified, workaround a JDK 6 IPv6 bug
                    InetSocketAddress bindAddr = new InetSocketAddress(address, 0);
                    this.socket.bind(bindAddr);
                }
                this.socket.connect(this.socketAddress, this.socketTimeout);
                this.socket.setSoTimeout(this.socketTimeout);
                this.localAddress = this.socket.getLocalAddress();
            }
            return this.socket;
        }

        /**
         * Convenience method that returns a reader to the proxy.
         */
        synchronized BufferedReader getConnectionReader() throws IOException {
            if (this.reader == null) {
                this.reader = new BufferedReader(new InputStreamReader(this.getConnection().getInputStream()));
            }
            return this.reader;
        }

        /**
         * Convenience method that returns a writer to the proxy.
         */
        synchronized BufferedWriter getConnectionWriter() throws IOException {
            if (this.writer == null) {
                this.writer = new BufferedWriter(new OutputStreamWriter(this.getConnection().getOutputStream()));
            }
            return this.writer;
        }

        InetAddress getLocalAddress() {
            return this.localAddress;
        }

        /**
         * Close connection.
         */
        synchronized void closeConnection() {
            if (this.reader != null) {
                try {
                    this.reader.close();
                } catch (IOException e) {
                    // Ignore
                }
                this.reader = null;
            }
            if (this.writer != null) {
                try {
                    this.writer.close();
                } catch (IOException e) {
                    // Ignore
                }
                this.writer = null;
            }
            if (this.socket != null) {
                if (!this.socket.isClosed()) {
                    try {
                        this.socket.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
                this.socket = null;
            }
        }

        boolean isIoExceptionLogged() {
            return this.ioExceptionLogged;
        }

        void setIoExceptionLogged(boolean ioErrorLogged) {
            this.ioExceptionLogged = ioErrorLogged;
        }
    }

    public static class VirtualHostImpl implements ResetRequestSource.VirtualHost, Externalizable {
        private final Set<String> aliases = new LinkedHashSet<String>();
        private final Map<String, ResetRequestSource.Status> contexts = new HashMap<String, ResetRequestSource.Status>();

        public VirtualHostImpl() {
            // Expose for deserialization
        }

        /**
         * @{inheritDoc
         * @see org.jboss.modcluster.mcmp.ResetRequestSource.VirtualHost#getAliases()
         */
        @Override
        public Set<String> getAliases() {
            return this.aliases;
        }

        /**
         * @{inheritDoc
         * @see org.jboss.modcluster.mcmp.ResetRequestSource.VirtualHost#getContexts()
         */
        @Override
        public Map<String, ResetRequestSource.Status> getContexts() {
            return this.contexts;
        }

        /**
         * @{inheritDoc
         * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
         */
        @Override
        public void readExternal(ObjectInput input) throws IOException {
            int aliases = input.readInt();
            for (int i = 0; i < aliases; ++i) {
                this.aliases.add(input.readUTF());
            }

            ResetRequestSource.Status[] stati = ResetRequestSource.Status.values();
            int contexts = input.readInt();

            for (int i = 0; i < contexts; ++i) {
                this.contexts.put(input.readUTF(), stati[input.readInt()]);
            }
        }

        /**
         * @{inheritDoc
         * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
         */
        @Override
        public void writeExternal(ObjectOutput output) throws IOException {
            output.writeInt(this.aliases.size());

            for (String alias : this.aliases) {
                output.writeUTF(alias);
            }

            output.writeInt(this.contexts.size());

            for (Map.Entry<String, ResetRequestSource.Status> context : this.contexts.entrySet()) {
                output.writeUTF(context.getKey());
                output.writeInt(context.getValue().ordinal());
            }
        }
    }
}
