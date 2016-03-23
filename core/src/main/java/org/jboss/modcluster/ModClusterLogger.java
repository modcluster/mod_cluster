/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.modcluster;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Host;
import org.jboss.modcluster.mcmp.MCMPRequestType;

/**
 * @author Paul Ferraro
 */
@MessageLogger(projectCode = "MODCLUSTER")
public interface ModClusterLogger {
    ModClusterLogger LOGGER = Logger.getMessageLogger(ModClusterLogger.class, ModClusterLogger.class.getPackage().getName());

    @LogMessage(level = INFO)
    @Message(id = 1, value = "Initializing mod_cluster version %s")
    void init(String version);

    @LogMessage(level = INFO)
    @Message(id = 2, value = "Initiating mod_cluster shutdown")
    void shutdown();

    @LogMessage(level = DEBUG)
    @Message(id = 3, value = "Received server start event")
    void startServer();

    @LogMessage(level = DEBUG)
    @Message(id = 4, value = "Received server stop event")
    void stopServer();

    @LogMessage(level = DEBUG)
    @Message(id = 5, value = "Received add context event for %s:%s")
    void addContext(Host host, Context context);

    @LogMessage(level = DEBUG)
    @Message(id = 6, value = "Received remove context event for %s:%s")
    void removeContext(Host host, Context context);

    @LogMessage(level = DEBUG)
    @Message(id = 7, value = "Received start context event for %s:%s")
    void startContext(Host host, Context context);

    @LogMessage(level = DEBUG)
    @Message(id = 8, value = "Received stop context event for %s:%s")
    void stopContext(Host host, Context context);

    @LogMessage(level = DEBUG)
    @Message(id = 9, value = "Sending %s for %s")
    void sendEngineCommand(MCMPRequestType command, Engine engine);

    @LogMessage(level = DEBUG)
    @Message(id = 10, value = "Sending %s for %s:%s")
    void sendContextCommand(MCMPRequestType command, Host host, Context context);

    @LogMessage(level = INFO)
    @Message(id = 11, value = "%s will use %s as jvm-route")
    void detectJvmRoute(Engine engine, String jvmRoute);

    @LogMessage(level = INFO)
    @Message(id = 12, value = "%s connector will use %s")
    void detectConnectorAddress(Engine engine, InetAddress address);

    @LogMessage(level = DEBUG)
    @Message(id = 20, value = "Waiting to drain %d pending requests from %s:%s")
    void drainRequests(int requests, Host host, Context context);

    @LogMessage(level = INFO)
    @Message(id = 21, value = "All pending requests drained from %s:%s in %.1f seconds")
    void requestsDrained(Host host, Context context, float seconds);

    @LogMessage(level = WARN)
    @Message(id = 22, value = "Failed to drain %d remaining pending requests from %s:%s within %.1f seconds")
    void requestDrainTimeout(int requests, Host host, Context context, float seconds);

    @LogMessage(level = DEBUG)
    @Message(id = 23, value = "Waiting to drain %d active sessions from %s:%s")
    void drainSessions(int sessions, Host host, Context context);

    @LogMessage(level = INFO)
    @Message(id = 24, value = "All active sessions drained from %s:%s in %.1f seconds")
    void sessionsDrained(Host host, Context context, float seconds);

    @LogMessage(level = WARN)
    @Message(id = 25, value = "Failed to drain %d remaining active sessions from %s:%s within %.1f seconds")
    void sessionDrainTimeout(int sessions, Host host, Context context, float seconds);

    @LogMessage(level = WARN)
    @Message(id = 30, value = "Attempted to bind multicast socket to a unicast address: %s.  Multicast socket will not be bound to an address.")
    void createMulticastSocketWithUnicastAddress(InetAddress address);

    @LogMessage(level = WARN)
    @Message(id = 31, value = "Could not bind multicast socket to %s (%s address); make sure your multicast address is of the same type as the IP stack (IPv4 or IPv6). Multicast socket will not be bound to an address, but this may lead to cross talking (see http://www.jboss.org/community/docs/DOC-9469 for details).")
    void potentialCrossTalking(@Cause Throwable cause, InetAddress address, String addressType);

    @LogMessage(level = INFO)
    @Message(id = 32, value = "Listening to proxy advertisements on %s")
    void startAdvertise(InetSocketAddress address);

    @LogMessage(level = WARN)
    @Message(id = 33, value = "Failed to interrupt socket reception.")
    void socketInterruptFailed(@Cause Throwable cause);

    @LogMessage(level = ERROR)
    @Message(id = 34, value = "Failed to start advertise listener")
    void advertiseStartFailed(@Cause Throwable cause);

    @LogMessage(level = ERROR)
    @Message(id = 40, value = "Failed to parse response header from %2$s for %1$s command")
    void parseHeaderFailed(@Cause Throwable cause, MCMPRequestType command, InetSocketAddress proxy);

    @LogMessage(level = ERROR)
    @Message(id = 41, value = "Unrecoverable syntax error %s sending %s command to %s: %s")
    void unrecoverableErrorResponse(String errorType, MCMPRequestType type, InetSocketAddress proxy, String message);

    @LogMessage(level = ERROR)
    @Message(id = 42, value = "Error %s sending %s command to %s, configuration will be reset: %s")
    void recoverableErrorResponse(String errorType, MCMPRequestType type, InetSocketAddress proxy, String message);

    @LogMessage(level = ERROR)
    @Message(id = 43, value = "Failed to send %s to %s")
    void sendFailed(@Cause Throwable cause, MCMPRequestType type, InetSocketAddress proxy);

    @LogMessage(level = WARN)
    @Message(id = 44, value = "%s requires com.sun.management.OperatingSystemMXBean.")
    void missingOSBean(String classname);

    @LogMessage(level = WARN)
    @Message(id = 45, value = "%s is not supported on this system and will be disabled.")
    void notSupportedOnSystem(String classname);

    @LogMessage(level = INFO)
    @Message(id = 46, value = "Starting to drain %d active sessions from %s:%s in %d seconds.")
    void startSessionDraining(int sessions, Host host, Context context, long timeout);
}
