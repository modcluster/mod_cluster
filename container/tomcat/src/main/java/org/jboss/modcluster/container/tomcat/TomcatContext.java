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
package org.jboss.modcluster.container.tomcat;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.catalina.LifecycleState;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Valve;
import org.apache.catalina.comet.CometEvent;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Host;
import org.jboss.modcluster.container.listeners.ServletRequestListener;
import org.jboss.modcluster.container.listeners.HttpSessionListener;

/**
 * {@link Context} implementation that wraps a {@link org.apache.catalina.Context}.
 *
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public class TomcatContext implements Context {

    private final RequestListenerValveFactory valveFactory;
    protected final org.apache.catalina.Context context;
    protected final Host host;

    /**
     * Constructs a new CatalinaContext wrapping the specified context.
     *
     * @param context the catalina context
     * @param host    the parent container
     */
    public TomcatContext(org.apache.catalina.Context context, Host host, RequestListenerValveFactory valveFactory) {
        this.context = context;
        this.host = host;
        this.valveFactory = valveFactory;
    }

    public TomcatContext(org.apache.catalina.Context context, Host host) {
        this(context, host, new RequestListenerValveFactory() {
            @Override
            public Valve createValve(ServletRequestListener listener) {
                return new RequestListenerValve(listener);
            }
        });
    }

    private static class RequestListenerValve extends ValveBase {
        private final ServletRequestListener listener;

        RequestListenerValve(ServletRequestListener listener) {
            this.listener = listener;
        }

        @Override
        public void invoke(Request request, Response response) throws IOException, ServletException {
            this.event(request, response, null);
        }

        @Override
        public void event(Request request, Response response, CometEvent event) throws IOException, ServletException {
            this.listener.requestInitialized();

            Valve valve = this.getNext();

            try {
                if (event != null) {
                    valve.event(request, response, event);
                } else {
                    valve.invoke(request, response);
                }
            } finally {
                this.listener.requestDestroyed();
            }
        }

        @Override
        public int hashCode() {
            return this.listener.hashCode();
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof RequestListenerValve)) return false;

            RequestListenerValve valve = (RequestListenerValve) object;

            return this.listener == valve.listener;
        }
    }

    @Override
    public Host getHost() {
        return this.host;
    }

    @Override
    public String getPath() {
        return this.context.getPath();
    }

    @Override
    public boolean isStarted() {
        return LifecycleState.STARTED == this.context.getState();
    }

    @Override
    public void addRequestListener(ServletRequestListener requestListener) {
        // Add a valve rather than using Context.setApplicationEventListeners(...), since these will be overwritten at the end of Context.start()
        if (this.valveFactory != null) {
            this.context.getPipeline().addValve(this.valveFactory.createValve(requestListener));
        }
    }

    @Override
    public void removeRequestListener(ServletRequestListener requestListener) {
        if (this.valveFactory != null) {
            Valve listenerValve = this.valveFactory.createValve(requestListener);

            Pipeline pipeline = this.context.getPipeline();

            for (Valve valve : pipeline.getValves()) {
                if (listenerValve.equals(valve)) {
                    pipeline.removeValve(valve);
                    break;
                }
            }
        }
    }

    @Override
    public int getActiveSessionCount() {
        return this.context.getManager().getActiveSessions();
    }

    @Override
    public boolean isDistributable() {
        return this.context.getDistributable();
    }

    @Override
    public void addSessionListener(HttpSessionListener sessionListener) {
        synchronized (this.context) {
            this.context.setApplicationLifecycleListeners(this.addListener(this.adaptSessionListener(sessionListener), this.context.getApplicationLifecycleListeners()));
        }
    }

    @Override
    public void removeSessionListener(HttpSessionListener sessionListener) {
        synchronized (this.context) {
            this.context.setApplicationLifecycleListeners(this.removeListener(this.adaptSessionListener(sessionListener), this.context.getApplicationLifecycleListeners()));
        }
    }

    public Object adaptSessionListener(HttpSessionListener sessionListener) {
        return new JavaxHttpSessionListener(sessionListener);
    }

    private Object[] addListener(Object listener, Object[] listeners) {
        if (listeners == null) return new Object[] { listener };

        List<Object> listenerList = new ArrayList<>(listeners.length + 1);

        listenerList.add(listener);
        listenerList.addAll(Arrays.asList(listeners));

        return listenerList.toArray();
    }

    private Object[] removeListener(Object listener, Object[] listeners) {
        if (listeners == null) return null;

        List<Object> listenerList = new ArrayList<>(listeners.length - 1);

        for (Object existingListener : listeners) {
            if (!existingListener.equals(listener)) {
                listenerList.add(existingListener);
            }
        }

        return listenerList.toArray();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof TomcatContext)) return false;

        TomcatContext context = (TomcatContext) object;

        return this.context == context.context;
    }

    @Override
    public int hashCode() {
        return this.context.hashCode();
    }

    @Override
    public String toString() {
        return this.context.getPath();
    }
}
