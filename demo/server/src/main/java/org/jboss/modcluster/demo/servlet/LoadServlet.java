/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.demo.servlet;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Paul Ferraro
 */
public class LoadServlet extends HttpServlet {
    /** The serialVersionUID */
    private static final long serialVersionUID = 5665079393261425098L;

    protected static final String DURATION = "duration";
    protected static final String DEFAULT_DURATION = "15";
    protected static final String COUNT = "count";

    public static final String JVM_ROUTE_SYSTEM_PROPERTY = "jboss.mod_cluster.jvmRoute";

    private String jvmRoute;

    @Override
    public void init() throws ServletException {
        this.jvmRoute = System.getProperty(JVM_ROUTE_SYSTEM_PROPERTY);

        if (this.jvmRoute == null) {
            try {
                MBeanServer server = ManagementFactory.getPlatformMBeanServer();
                ObjectName name = this.findObjectName(server, "type", "Engine", "jboss.web", "Catalina");
                if (name != null) {
                    this.jvmRoute = (String) server.getAttribute(name, "jvmRoute");
                }
            } catch (JMException e) {
                throw new ServletException(e);
            }
            if (this.jvmRoute == null) {
                throw new ServletException("Failed to locate jvm route!");
            }
        }
        this.log("Discovered jvm-route: " + this.jvmRoute);
    }

    private ObjectName findObjectName(MBeanServer server, String key, String value, String... domains) throws MalformedObjectNameException {
        for (String domain: domains) {
            ObjectName name = ObjectName.getInstance(domain, key, value);
            if (server.isRegistered(name)) {
                return name;
            }
        }
        return null;
    }

    protected String getJvmRoute() {
        return this.jvmRoute;
    }

    protected URI createServerURI(HttpServletRequest request, Map<String, String> parameterMap) {
        return this.createURI(request, request.getServerName(), request.getServerPort(), parameterMap);
    }

    protected URI createLocalURI(HttpServletRequest request, Map<String, String> parameterMap) {
        return this.createURI(request, request.getLocalName(), request.getLocalPort(), parameterMap);
    }

    private URI createURI(HttpServletRequest request, String host, int port, Map<String, String> parameters) {
        String query = null;
        if ((parameters != null) && !parameters.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            Iterator<Map.Entry<String, String>> entries = parameters.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry<String, String> entry = entries.next();
                builder.append(entry.getKey()).append('=').append(entry.getValue());
                if (entries.hasNext()) {
                    builder.append('&');
                }
            }
            query = builder.toString();
        }
        try {
            return new URI(request.getScheme(), null, host, port, request.getContextPath() + request.getServletPath(), query, null);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    protected String getParameter(HttpServletRequest request, String name, String defaultValue) {
        String value = request.getParameter(name);

        return (value != null) ? value : this.getInitParameter(name, defaultValue);
    }

    protected String getInitParameter(String name, String defaultValue) {
        String value = this.getInitParameter(name);

        if (value == null) {
            value = this.getServletContext().getInitParameter(name);
        }

        return (value != null) ? value : defaultValue;
    }

    protected void writeLocalName(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.getWriter().append("Handled By: ").append(this.getJvmRoute());
    }
}
