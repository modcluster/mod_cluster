/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.demo.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Brian Stansberry
 */
public class RequestDriver {
    private Client[] clients;

    private final ConcurrentMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<String, AtomicInteger>();
    private final ConcurrentMap<String, AtomicInteger> sessionCounts = new ConcurrentHashMap<String, AtomicInteger>();

    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private Thread startThread;

    public void start(final URL request_url, final URL destroy_url, int num_threads, final int session_life, final int sleep_time, int startup_time) {
        this.requestCounts.clear();
        this.sessionCounts.clear();

        this.clients = new Client[num_threads];

        this.stopped.set(false);
        final int startupPause = startup_time > 0 ? (startup_time * 1000 / num_threads) : 0;

        System.out.println("Request URL is " + request_url);
        System.out.println("Terminal URL is " + destroy_url);
        System.out.println("Starting " + num_threads + " clients");
        Runnable r = new Runnable() {
            public void run() {
                for (int i = 0; i < clients.length & !stopped.get(); i++) {
                    Client client = new Client(request_url, destroy_url, session_life, sleep_time, requestCounts, sessionCounts, stopped);
                    clients[i] = client;
                    client.start();
                    try {
                        Thread.sleep(startupPause);
                    } catch (InterruptedException e) {
                        e.printStackTrace(System.err);
                        return;
                    }
                }

            }
        };

        this.startThread = new Thread(r, "RequestDriverStartThread");
        this.startThread.start();
    }

    public void stop() {
        // Stop creating new clients (if we still are)
        this.stopped.set(true);
        if (this.startThread != null && this.startThread.isAlive()) {
            try {
                this.startThread.join(2000);
            } catch (InterruptedException e) {
            }

            if (this.startThread.isAlive()) {
                this.startThread.interrupt();
            }
        }

        // Stop the clients we've created
        if (this.clients != null) {
            for (Client client : this.clients) {
                if (client != null && client.isAlive()) {
                    client.terminate();
                }
            }
        }
    }

    public ConcurrentMap<String, AtomicInteger> getRequestCounts() {
        return requestCounts;
    }

    public ConcurrentMap<String, AtomicInteger> getSessionCounts() {
        return sessionCounts;
    }

    public ClientStatus getClientStatus() {
        ClientStatus result = new ClientStatus();
        if (this.clients != null) {
            for (Client client : this.clients) {
                if (client != null) {
                    result.clientCount++;
                    if (client.isAlive()) {
                        result.liveClientCount++;
                    }
                    if (client.isSuccessful()) {
                        result.successfulClientCount++;
                    }
                }
            }
        }
        return result;
    }

    public static class ClientStatus {
        public int clientCount;
        public int liveClientCount;
        public int successfulClientCount;
    }

    private static class Client extends Thread {
        private final URL request_url, destroy_url;

        private final long sessionLife;

        private final long sleepTime;

        private boolean successful = true;

        private final byte[] buffer = new byte[1024];

        private String cookie = null;

        private String lastHandler = null;

        private final AtomicBoolean stopped;

        private final ConcurrentMap<String, AtomicInteger> requests;

        private final ConcurrentMap<String, AtomicInteger> sessions;

        private Client(URL request_url, URL destroy_url, long sessionLife, int sleepTime, ConcurrentMap<String, AtomicInteger> requests, ConcurrentMap<String, AtomicInteger> sessions, AtomicBoolean stopped) {
            this.request_url = request_url;
            this.destroy_url = (request_url.equals(destroy_url)) ? null : destroy_url;
            this.sessionLife = sessionLife * 1000;
            this.sleepTime = sleepTime;
            this.requests = requests;
            this.sessions = sessions;
            this.stopped = stopped;
            // Don't keep the VM alive
            setDaemon(false);
        }

        public void run() {
            try {
                loop();
            } catch (Exception e) {
                error("failure", e);
                successful = false;
            } finally {
                try {
                    // Make an attempt to terminate any ongoing session
                    // Only bother if our destroy
                    if (cookie != null && destroy_url != null) {
                        executeRequest(destroy_url);
                    }
                } catch (IOException e) {
                } finally {
                    // If we haven't already cleaned up this thread's
                    // session info, do so now
                    handleSessionTermination();
                }
            }
        }

        public boolean isSuccessful() {
            return successful;
        }

        private void terminate() {
            if (this.isAlive()) {
                try {
                    this.join(5000);
                    if (this.isAlive()) {
                        this.interrupt();
                    }
                } catch (InterruptedException e) {
                }
            }
        }

        private void loop() throws IOException {
            int rc;

            while (!stopped.get()) {
                long sessionStart = System.currentTimeMillis();
                long elapsed = 0;
                boolean failed = false;
                while ((elapsed < sessionLife) && !stopped.get()) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        break;
                    }

                    rc = executeRequest(request_url);
                    if (rc == 200) {
                        elapsed = System.currentTimeMillis() - sessionStart;
                    } else {
                        failed = true;
                        break;
                    }
                }

                if (!failed && destroy_url != null) {
                    // Send invalidation request
                    executeRequest(destroy_url);
                }

                handleSessionTermination();
            }
        }

        private int executeRequest(URL url) throws IOException {
            InputStream input = null;
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) url.openConnection(); // not yet connected
                if (cookie != null)
                    conn.setRequestProperty("Cookie", cookie);

                input = conn.getInputStream(); // NOW it is connected
                while (input.read(buffer) > 0) {
                }
                input.close(); // discard data

                String handlerNode = conn.getHeaderField("X-ClusterNode");

                modifyCount(handlerNode, requests, true);

                String tmp_cookie = conn.getHeaderField("set-cookie");

                if (tmp_cookie != null && cookie == null) {
                    // New session -- track it and its handler
                    cookie = tmp_cookie;
                    modifyCount(handlerNode, sessions, true);
                    // Track this handler so we can decrement the session
                    // count in case of failover or error
                    lastHandler = handlerNode;
                } else if (lastHandler != null && !lastHandler.equals(handlerNode)) {
                    // Ongoing session has failed over in an unplanned way,
                    // so decrement the previous handler's count.
                    modifyCount(lastHandler, sessions, false);
                    lastHandler = null;
                }

                return conn.getResponseCode();
            } catch (Exception e) {
                handleSessionTermination();

                if (e instanceof IOException)
                    throw (IOException) e;
                else
                    throw (RuntimeException) e;
            } finally {
                if (conn != null)
                    conn.disconnect();
            }
        }

        private void handleSessionTermination() {
            cookie = null;
            String last = lastHandler;
            lastHandler = null;
            if (last != null) {
                // Decrement the session count for whoever last handled a request
                modifyCount(last, sessions, false);
            }
        }

        private static void modifyCount(String handlerNode, ConcurrentMap<String, AtomicInteger> map, boolean increment) {
            if (handlerNode != null) {
                AtomicInteger count = map.get(handlerNode);
                if (count == null) {
                    count = new AtomicInteger();
                    AtomicInteger existing = map.putIfAbsent(handlerNode, count);
                    if (existing != null) {
                        count = existing;
                    }
                }

                if (increment)
                    count.incrementAndGet();
                else
                    count.decrementAndGet();
            }
        }

        private static void error(String msg, Throwable th) {
            String tmp = "[thread-" + Thread.currentThread().getId() + "]: " + msg;
            if (th != null) {
                tmp += ", ex: " + th + "\n";
                StringWriter writer = new StringWriter();
                PrintWriter pw = new PrintWriter(writer);
                th.printStackTrace(pw);
                pw.flush();
                tmp += writer.toString();
            }
            System.err.println(tmp);
        }
    }
}