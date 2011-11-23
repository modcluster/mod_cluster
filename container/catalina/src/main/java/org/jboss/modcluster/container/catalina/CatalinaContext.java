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
package org.jboss.modcluster.container.catalina;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.catalina.Pipeline;
import org.apache.catalina.Valve;
import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Host;

/**
 * {@link Context} implementation that wraps a {@link org.apache.catalina.Context}.
 * 
 * @author Paul Ferraro
 */
public class CatalinaContext implements Context {
    private final RequestListenerValveFactory valveFactory;
    protected final org.apache.catalina.Context context;
    protected final Host host;

    /**
     * Constructs a new CatalinaContext wrapping the specified context.
     * 
     * @param context the catalina context
     * @param host the parent container
     */
    public CatalinaContext(org.apache.catalina.Context context, Host host, RequestListenerValveFactory valveFactory) {
        this.context = context;
        this.host = host;
        this.valveFactory = valveFactory;
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
        return this.context.getAvailable();
    }

    @Override
    public void addRequestListener(final ServletRequestListener listener) {
        // Add a valve rather than using Context.setApplicationEventListeners(...), since these will be overwritten at the end of Context.start()
        if (this.valveFactory != null) {
            this.context.getPipeline().addValve(this.valveFactory.createValve(listener));
        }
    }

    @Override
    public void removeRequestListener(ServletRequestListener listener) {
        if (this.valveFactory != null) {
            Valve listenerValve = this.valveFactory.createValve(listener);

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
        return this.context.getManager().getDistributable();
    }

    @Override
    public void addSessionListener(HttpSessionListener listener) {
        synchronized (this.context) {
            this.context.setApplicationLifecycleListeners(this.addListener(listener, this.context.getApplicationLifecycleListeners()));
        }
    }

    @Override
    public void removeSessionListener(HttpSessionListener listener) {
        synchronized (this.context) {
            this.context.setApplicationLifecycleListeners(this.removeListener(listener, this.context.getApplicationLifecycleListeners()));
        }
    }

    private Object[] addListener(Object listener, Object[] listeners) {
        if (listeners == null) return new Object[] { listener };

        List<Object> listenerList = new ArrayList<Object>(listeners.length + 1);

        listenerList.add(listener);
        listenerList.addAll(Arrays.asList(listeners));

        return listenerList.toArray();
    }

    private Object[] removeListener(Object listener, Object[] listeners) {
        if (listeners == null)  return null;

        List<Object> listenerList = new ArrayList<Object>(listeners.length - 1);

        for (Object existingListener : listeners) {
            if (!existingListener.equals(listener)) {
                listenerList.add(existingListener);
            }
        }

        return listenerList.toArray();
    }

    @Override
    public boolean equals(Object object) {
        if ((object == null) || !(object instanceof CatalinaContext)) return false;

        CatalinaContext context = (CatalinaContext) object;

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
