/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.container.tomcat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.servlet.ServletException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Host;
import org.jboss.modcluster.container.listeners.HttpSessionListener;
import org.jboss.modcluster.container.listeners.ServletRequestListener;

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
    protected final TomcatRegistry registry;

    public TomcatContext(TomcatRegistry registry, org.apache.catalina.Context context, RequestListenerValveFactory valveFactory) {
        this.registry = registry;
        this.context = context;
        this.host = new TomcatHost(registry, (org.apache.catalina.Host) context.getParent());
        this.valveFactory = valveFactory;
    }

    public TomcatContext(TomcatRegistry registry, org.apache.catalina.Context context) {
        this(registry, context, RequestListenerValve::new);
    }

    private static class RequestListenerValve extends ValveBase {
        private final ServletRequestListener listener;

        RequestListenerValve(ServletRequestListener listener) {
            this.listener = listener;
        }

        @Override
        public void invoke(Request request, Response response) throws IOException, ServletException {
            this.listener.requestInitialized();

            Valve valve = this.getNext();

            try {
                valve.invoke(request, response);
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
        if (this.context.getManager() == null) {
            return 0;
        }
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
        return new JakartaHttpSessionListener(sessionListener);
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
        if (listeners.length == 0) return null;

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
