/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.modcluster.demo.servlet;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Iterator;
import java.util.Map;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Engine;
import org.apache.catalina.ServerFactory;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;

/**
 * @author Paul Ferraro
 * 
 */
public class LoadServlet extends HttpServlet {
    /** The serialVersionUID */
    private static final long serialVersionUID = 5665079393261425098L;

    protected static final String DURATION = "duration";
    protected static final String DEFAULT_DURATION = "15";
    protected static final String COUNT = "count";

    private static final String HOST = "host";
    private static final String PORT = "port";

    private static final String SERVER_OBJECT_NAME = "server-object-name";
    private static final String SERVICE_NAME = "service-name";
    private static final String DEFAULT_SERVER_OBJECT_NAME = "jboss.web:type=Server";
    private static final String DEFAULT_SERVICE_NAME = "jboss.web";
	private static final String SERIVCE_OBJECT_NAME = "service-object-name";
	private static final String DEFAULT_SERVICE_OBJECT_NAME = "jboss.web:type=Service,serviceName=jboss.web";

	private static final String ENGINE_OBJECT_NAME = "engine-object-name";

	private static final String DEFAULT_ENGINE_OBJECT_NAME = "jboss.web:type=Engine";

	private static final String TOMCAT_SERVER_OBJECT_NAME = "Catalina:type=Server";

	private static final String TOMCAT_SERVICE_NAME = "Catalina";

	private static final String TOMCAT_ENGINE_OBJECT_NAME = "Catalina:type=Engine";

	private static final String CONNECTOR_OBJECT_NAME = "connector-object-name";

	private static final String TOMCAT_CONNECTOR_OBJECT_NAME = "Catalina:type=Connector,port=*";

 	private String jvmRoute;

    /**
     * @{inheritDoc
     * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        try {
        	MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        	ObjectName name = ObjectName.getInstance(this.getInitParameter(SERVER_OBJECT_NAME, DEFAULT_SERVER_OBJECT_NAME));
			if (server.isRegistered(name)) {
				/* jbossweb 5.0 and 6.0 */
				String serviceName = this.getInitParameter(SERVICE_NAME, DEFAULT_SERVICE_NAME);
		        Service service = (Service) server.invoke(name, "findService", new Object[] { serviceName },
		                new String[] { String.class.getName() });

		        for (Connector connector : service.findConnectors()) {
		            if (connector.getProtocol().startsWith("HTTP")) {
		                ServletContext context = config.getServletContext();
		                context.setAttribute(PORT, Integer.valueOf(connector.getPort()));
		            }
		        }
		        this.jvmRoute = ((Engine) service.getContainer()).getJvmRoute();
		        return;
			} else {
				name = ObjectName.getInstance(this.getInitParameter(SERIVCE_OBJECT_NAME, DEFAULT_SERVICE_OBJECT_NAME));
            	if (server.isRegistered(name)) {
            		/* AS7 Void.class.getName() */
                    for (Connector connector : (Connector []) server.invoke(name, "findConnectors", new Object[] { },
                            new String[] { })) {
                        if (connector.getProtocol().startsWith("HTTP")) {
                            ServletContext context = config.getServletContext();
                            context.setAttribute(PORT, Integer.valueOf(connector.getPort()));
                        }
                    }
                    name = ObjectName.getInstance(this.getInitParameter(ENGINE_OBJECT_NAME, DEFAULT_ENGINE_OBJECT_NAME));
                    this.jvmRoute = (String) server.getAttribute(name, "jvmRoute");
                    return;
            		
            	} else {
            		/* must be Tomcat6 */
            		name = ObjectName.getInstance(this.getInitParameter(CONNECTOR_OBJECT_NAME, TOMCAT_CONNECTOR_OBJECT_NAME));
            		for (ObjectName connectorname: server.queryNames(name, null)) {
            			if (((String) server.getAttribute(connectorname, "protocol")).startsWith("HTTP")) {
            				ServletContext context = config.getServletContext();
            				context.setAttribute(PORT, Integer.valueOf(server.getAttribute(connectorname, "port").toString()));
             			}
            		}
    		        name = ObjectName.getInstance(this.getInitParameter(ENGINE_OBJECT_NAME, TOMCAT_ENGINE_OBJECT_NAME));
                    this.jvmRoute = (String) server.getAttribute(name, "jvmRoute");
            		return;
            	}
				
			}
			
		} catch (MalformedObjectNameException e) {
			throw new ServletException(e);
		} catch (NullPointerException e) {
			throw new ServletException(e);
		} catch (InstanceNotFoundException e) {
			throw new ServletException(e);
		} catch (ReflectionException e) {
			throw new ServletException(e);
		} catch (MBeanException e) {
			throw new ServletException(e);
		} catch (AttributeNotFoundException e) {
			throw new ServletException(e);
		}
    }

    protected String getJvmRoute() {
        return this.jvmRoute;
    }

    protected String createServerURL(HttpServletRequest request, Map<String, String> parameterMap) {
        return this.createURL(request, request.getServerName(), request.getServerPort(), parameterMap);
    }

    protected String createLocalURL(HttpServletRequest request, Map<String, String> parameterMap) {
        ServletContext context = this.getServletContext();

        String contextHost = (String) context.getAttribute(HOST);
        String host = (contextHost != null) ? contextHost : System.getProperty("jboss.bind.address", request.getLocalName());

        Integer contextPort = (Integer) context.getAttribute(PORT);
        int port = (contextPort != null) ? contextPort.intValue() : request.getLocalPort();

        return this.createURL(request, host, port, parameterMap);
    }

    private String createURL(HttpServletRequest request, String host, int port, Map<String, String> parameterMap) {
        StringBuilder builder = new StringBuilder();

        builder.append(request.getScheme()).append("://");
        builder.append(host).append(':').append(port);
        builder.append(request.getContextPath()).append(request.getServletPath());

        if ((parameterMap != null) && !parameterMap.isEmpty()) {
            builder.append("?");

            Iterator<Map.Entry<String, String>> entries = parameterMap.entrySet().iterator();

            while (entries.hasNext()) {
                Map.Entry<String, String> entry = entries.next();

                builder.append(entry.getKey()).append('=').append(entry.getValue());

                if (entries.hasNext()) {
                    builder.append('&');
                }
            }
        }

        return builder.toString();
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
