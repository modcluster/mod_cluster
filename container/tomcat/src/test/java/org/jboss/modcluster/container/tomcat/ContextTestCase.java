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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.catalina.LifecycleState;
import org.apache.catalina.Manager;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Valve;
import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Host;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Paul Ferraro
 */
public class ContextTestCase {
    protected final Host host = mock(Host.class);
    protected final org.apache.catalina.Context context = mock(org.apache.catalina.Context.class);
    private final RequestListenerValveFactory valveFactory = mock(RequestListenerValveFactory.class);
    protected final Context catalinaContext = this.createContext(this.context, this.host);

    protected Context createContext(org.apache.catalina.Context context, Host host) {
        return new TomcatContext(this.context, this.host, this.valveFactory);
    }

    @Test
    public void getHost() {
        assertSame(this.host, this.catalinaContext.getHost());
    }

    @Test
    public void getPath() {
        String expected = "path";

        when(this.context.getPath()).thenReturn(expected);

        String result = this.catalinaContext.getPath();

        assertSame(expected, result);
    }

    @Test
    public void isStarted() {
        when(this.context.getState()).thenReturn(LifecycleState.STOPPED);

        boolean result = this.catalinaContext.isStarted();

        assertFalse(result);

        when(this.context.getState()).thenReturn(LifecycleState.STARTED);

        result = this.catalinaContext.isStarted();

        assertTrue(result);
    }

    @Test
    public void requestListener() throws IOException, ServletException {
        // Test addRequestListener()
        ServletRequestListener listener = mock(ServletRequestListener.class);
        Pipeline pipeline = mock(Pipeline.class);
        Valve valve = mock(Valve.class);

        when(this.context.getPipeline()).thenReturn(pipeline);
        when(this.valveFactory.createValve(same(listener))).thenReturn(valve);

        this.catalinaContext.addRequestListener(listener);

        verify(pipeline).addValve(same(valve));

        // Test removeRequestListener()
        when(this.context.getPipeline()).thenReturn(pipeline);
        when(pipeline.getValves()).thenReturn(new Valve[] { valve });
        when(this.valveFactory.createValve(same(listener))).thenReturn(valve);

        this.catalinaContext.removeRequestListener(listener);

        verify(pipeline).removeValve(same(valve));
    }

    @Test
    public void getActiveSessionCount() {
        Manager manager = mock(Manager.class);

        when(this.context.getManager()).thenReturn(manager);
        when(manager.getActiveSessions()).thenReturn(10);

        int result = this.catalinaContext.getActiveSessionCount();

        assertEquals(10, result);
    }

    @Test
    public void addSessionListener() {
        HttpSessionListener listener = mock(HttpSessionListener.class);
        ArgumentCaptor<Object[]> capturedListeners = ArgumentCaptor.forClass(Object[].class);
        Object otherListener = new Object();

        when(this.context.getApplicationLifecycleListeners()).thenReturn(new Object[] { otherListener });

        this.catalinaContext.addSessionListener(listener);

        verify(this.context).setApplicationLifecycleListeners(capturedListeners.capture());
        Object[] listeners = capturedListeners.getValue();

        assertEquals(2, listeners.length);
        assertSame(listener, listeners[0]);
        assertSame(otherListener, listeners[1]);
    }

    @Test
    public void removeSessionListener() {
        HttpSessionListener listener = mock(HttpSessionListener.class);
        ArgumentCaptor<Object[]> capturedListeners = ArgumentCaptor.forClass(Object[].class);
        Object otherListener = new Object();

        when(this.context.getApplicationLifecycleListeners()).thenReturn(new Object[] { otherListener, listener });

        this.catalinaContext.removeSessionListener(listener);

        verify(this.context).setApplicationLifecycleListeners(capturedListeners.capture());
        Object[] listeners = capturedListeners.getValue();

        assertEquals(1, listeners.length);
        assertSame(otherListener, listeners[0]);
    }

    @Test
    public void isDistributable() {
        when(this.context.getDistributable()).thenReturn(true);

        boolean result = this.catalinaContext.isDistributable();

        assertTrue(result);
    }
}
