/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.mcmp.impl;

import java.net.Inet6Address;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.jboss.modcluster.config.BalancerConfiguration;
import org.jboss.modcluster.config.NodeConfiguration;
import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Host;
import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPRequestFactory;
import org.jboss.modcluster.mcmp.MCMPRequestType;

/**
 * @author Paul Ferraro
 */
public class DefaultMCMPRequestFactory implements MCMPRequestFactory {
    public static final String DEFAULT_SESSION_COOKIE_NAME = "JSESSIONID";
    public static final String DEFAULT_SESSION_PARAMETER_NAME = "jsessionid";

    private final MCMPRequest infoRequest = new DefaultMCMPRequest(MCMPRequestType.INFO, false, null,
            Collections.<String, String> emptyMap());
    private final MCMPRequest dumpRequest = new DefaultMCMPRequest(MCMPRequestType.DUMP, true, null,
            Collections.<String, String> emptyMap());

    @Override
    public MCMPRequest createConfigRequest(Engine engine, NodeConfiguration nodeConfig, BalancerConfiguration balancerConfig) {
        Connector connector = engine.getProxyConnector();
        Map<String, String> parameters = new TreeMap<String, String>();

        if (connector.isReverse()) {
            parameters.put("Reversed", "true");
        }

        // If address was specified as a host name, we would prefer it
        // toString() will not perform reverse dns lookup
        // so send host name portion, if it exists
        String address = connector.getAddress().toString().toLowerCase(Locale.ROOT);
        int index = address.indexOf("/");
        if (connector.getAddress() instanceof Inet6Address) {
            // IPv6 address require a []
            // ^No it does not. The RFC only requires [] in case when used in conjunction with port, lets keep it
            // if some implementations depend on it. (Rado)
            String saddr;
            if (index > 0) {
                saddr = address.substring(0, index); // Name.
            } else {
                saddr = "[";
                // MODCLUSTER-483 remove zone id completely
                String ipv6addr = address.substring(1);
                int zoneIndex = ipv6addr.indexOf("%");
                saddr = saddr.concat((zoneIndex < 0) ? ipv6addr : ipv6addr.substring(0, zoneIndex));
                saddr = saddr.concat("]");
            }
            parameters.put("Host", saddr);
        } else {
            parameters.put("Host", (index > 0) ? address.substring(0, index) : address.substring(1));
        }

        parameters.put("Port", String.valueOf(connector.getPort()));
        parameters.put("Type", connector.getType().toString());

        // Other configuration parameters
        String lbGroup = nodeConfig.getLoadBalancingGroup();
        if (lbGroup != null) {
            parameters.put("Domain", lbGroup);
        }
        if (nodeConfig.getFlushPackets()) {
            parameters.put("flushpackets", "On");
        }
        int flushWait = nodeConfig.getFlushWait();
        if (flushWait != -1) {
            parameters.put("flushwait", String.valueOf(flushWait));
        }
        int ping = nodeConfig.getPing();
        if (ping != -1) {
            parameters.put("ping", String.valueOf(ping));
        }
        int smax = nodeConfig.getSmax();
        if (smax != -1) {
            parameters.put("smax", String.valueOf(smax));
        }
        int ttl = nodeConfig.getTtl();
        if (ttl != -1) {
            parameters.put("ttl", String.valueOf(ttl));
        }
        int nodeTimeout = nodeConfig.getNodeTimeout();
        if (nodeTimeout != -1) {
            parameters.put("Timeout", String.valueOf(nodeTimeout));
        }
        String balancer = nodeConfig.getBalancer();
        if (balancer != null) {
            parameters.put("Balancer", balancer);
        }
        if (!balancerConfig.getStickySession()) {
            parameters.put("StickySession", "No");
        }
        String sessionCookieName = engine.getSessionCookieName();
        if (!sessionCookieName.equals(DEFAULT_SESSION_COOKIE_NAME)) {
            parameters.put("StickySessionCookie", sessionCookieName);
        }
        String sessionParameterName = engine.getSessionParameterName();
        if (!sessionParameterName.equals(DEFAULT_SESSION_PARAMETER_NAME)) {
            parameters.put("StickySessionPath", sessionParameterName);
        }
        if (balancerConfig.getStickySessionRemove()) {
            parameters.put("StickySessionRemove", "Yes");
        }
        if (!balancerConfig.getStickySessionForce()) {
            parameters.put("StickySessionForce", "No");
        }
        int workerTimeout = balancerConfig.getWorkerTimeout();
        if (workerTimeout != -1) {
            parameters.put("WaitWorker", "" + workerTimeout);
        }
        int maxAttempts = balancerConfig.getMaxAttempts();
        if (maxAttempts != -1) {
            parameters.put("Maxattempts", "" + maxAttempts);
        }

        return new DefaultMCMPRequest(MCMPRequestType.CONFIG, false, engine.getJvmRoute(), parameters);
    }

    @Override
    public MCMPRequest createDisableRequest(Context context) {
        return this.createRequest(MCMPRequestType.DISABLE_APP, context);
    }

    @Override
    public MCMPRequest createDisableRequest(Engine engine) {
        return this.createRequest(MCMPRequestType.DISABLE_APP, engine);
    }

    @Override
    public MCMPRequest createEnableRequest(Context context) {
        return this.createRequest(MCMPRequestType.ENABLE_APP, context);
    }

    @Override
    public MCMPRequest createEnableRequest(Engine engine) {
        return this.createRequest(MCMPRequestType.ENABLE_APP, engine);
    }

    @Override
    public MCMPRequest createRemoveRequest(Engine engine) {
        return this.createRequest(MCMPRequestType.REMOVE_APP, engine);
    }

    @Override
    public MCMPRequest createRemoveRequest(Context context) {
        return this.createRequest(MCMPRequestType.REMOVE_APP, context);
    }

    @Override
    public MCMPRequest createStatusRequest(String jvmRoute, int lbf) {
        return new DefaultMCMPRequest(MCMPRequestType.STATUS, false, jvmRoute, Collections.singletonMap("Load",
                String.valueOf(lbf)));
    }

    @Override
    public MCMPRequest createStopRequest(Engine engine) {
        return this.createRequest(MCMPRequestType.STOP_APP, engine);
    }

    @Override
    public MCMPRequest createStopRequest(Context context) {
        return this.createRequest(MCMPRequestType.STOP_APP, context);
    }

    @Override
    public MCMPRequest createDumpRequest() {
        return this.dumpRequest;
    }

    @Override
    public MCMPRequest createInfoRequest() {
        return this.infoRequest;
    }

    @Override
    public MCMPRequest createPingRequest() {
        return new DefaultMCMPRequest(MCMPRequestType.PING, false, null, Collections.<String, String> emptyMap());
    }

    @Override
    public MCMPRequest createPingRequest(String jvmRoute) {
        return new DefaultMCMPRequest(MCMPRequestType.PING, false, jvmRoute, Collections.<String, String> emptyMap());
    }

    @Override
    public MCMPRequest createPingRequest(String scheme, String host, int port) {
        Map<String, String> parameters = new TreeMap<String, String>();
        parameters.put("Scheme", scheme);
        parameters.put("Host", host);
        parameters.put("Port", String.valueOf(port));

        return new DefaultMCMPRequest(MCMPRequestType.PING, false, null, parameters);
    }

    private MCMPRequest createRequest(MCMPRequestType type, Context context) {
        Host host = context.getHost();

        return this.createContextRequest(type, host.getEngine().getJvmRoute(), host.getAliases(), context.getPath());
    }

    @Override
    public MCMPRequest createRemoveContextRequest(String jvmRoute, Set<String> aliases, String path) {
        return this.createContextRequest(MCMPRequestType.REMOVE_APP, jvmRoute, aliases, path);
    }

    private MCMPRequest createContextRequest(MCMPRequestType type, String jvmRoute, Set<String> aliases, String path) {
        Map<String, String> parameters = new TreeMap<String, String>();

        parameters.put("Context", (path.length() == 0) ? "/" : path);
        parameters.put("Alias", join(aliases, ','));

        return new DefaultMCMPRequest(type, false, jvmRoute, parameters);
    }

    private MCMPRequest createRequest(MCMPRequestType type, Engine engine) {
        return this.createEngineRequest(type, engine.getJvmRoute());
    }

    private MCMPRequest createEngineRequest(MCMPRequestType type, String jvmRoute) {
        return new DefaultMCMPRequest(type, true, jvmRoute, Collections.<String, String> emptyMap());
    }

    @Override
    public MCMPRequest createRemoveEngineRequest(String jvmRoute) {
        return this.createEngineRequest(MCMPRequestType.REMOVE_APP, jvmRoute);
    }

    private static String join(Iterable<String> collection, char delimiter) {
        StringBuilder builder = new StringBuilder();
        Iterator<String> values = collection.iterator();
        if (values.hasNext()) {
            builder.append(values.next());
        }
        while (values.hasNext()) {
            builder.append(delimiter).append(values.next());
        }
        return builder.toString();
    }
}
