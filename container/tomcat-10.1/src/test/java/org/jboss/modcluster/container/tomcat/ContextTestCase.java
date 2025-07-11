/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.container.tomcat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.catalina.LifecycleState;
import org.apache.catalina.Manager;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.Valve;
import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.listeners.HttpSessionListener;
import org.jboss.modcluster.container.listeners.ServletRequestListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Test for {@link Context}.
 *
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
class ContextTestCase {
    protected final TomcatRegistry registry = mock(TomcatRegistry.class);
    protected Server serverMock = mock(Server.class);
    protected Service serviceMock = mock(Service.class);
    protected org.apache.catalina.Engine engineMock = mock(org.apache.catalina.Engine.class);
    protected org.apache.catalina.Host hostMock = mock(org.apache.catalina.Host.class);

    protected final org.apache.catalina.Context context = mock(org.apache.catalina.Context.class);
    protected Context catalinaContext;

    @BeforeEach
    void before() {
        when(this.serviceMock.getServer()).thenReturn(this.serverMock);
        when(this.engineMock.getService()).thenReturn(this.serviceMock);
        when(this.hostMock.getParent()).thenReturn(this.engineMock);

        when(context.getParent()).thenReturn(this.hostMock);
        this.catalinaContext = new TomcatContext(this.registry, this.context);
    }

    @Test
    void getHost() {
        assertEquals(new TomcatHost(registry, hostMock), this.catalinaContext.getHost());
    }

    @Test
    void getPath() {
        String expected = "path";

        when(this.context.getPath()).thenReturn(expected);

        String result = this.catalinaContext.getPath();

        assertSame(expected, result);
    }

    @Test
    void isStarted() {
        when(this.context.getState()).thenReturn(LifecycleState.STOPPED);

        boolean result = this.catalinaContext.isStarted();

        assertFalse(result);

        when(this.context.getState()).thenReturn(LifecycleState.STARTED);

        result = this.catalinaContext.isStarted();

        assertTrue(result);
    }

    @Test
    void requestListener() throws Exception {
        // Test addRequestListener()
        ServletRequestListener listener = mock(ServletRequestListener.class);
        Pipeline pipeline = mock(Pipeline.class);
        ArgumentCaptor<Valve> capturedValve = ArgumentCaptor.forClass(Valve.class);

        when(this.context.getPipeline()).thenReturn(pipeline);

        this.catalinaContext.addRequestListener(listener);

        verify(pipeline).addValve(capturedValve.capture());

        Valve valve = capturedValve.getValue();

        // Test removeRequestListener()
        when(this.context.getPipeline()).thenReturn(pipeline);
        when(pipeline.getValves()).thenReturn(new Valve[] { valve });

        this.catalinaContext.removeRequestListener(listener);

        verify(pipeline).removeValve(same(valve));
    }

    @Test
    void getActiveSessionCount() {
        Manager manager = mock(Manager.class);

        when(this.context.getManager()).thenReturn(manager);
        when(manager.getActiveSessions()).thenReturn(10);

        int result = this.catalinaContext.getActiveSessionCount();

        assertEquals(10, result);
    }

    @Test
    void addSessionListener() {
        HttpSessionListener listener = mock(HttpSessionListener.class);
        ArgumentCaptor<Object[]> capturedListeners = ArgumentCaptor.forClass(Object[].class);
        Object otherListener = new Object();

        when(this.context.getApplicationLifecycleListeners()).thenReturn(new Object[] { otherListener });

        this.catalinaContext.addSessionListener(listener);

        verify(this.context).setApplicationLifecycleListeners(capturedListeners.capture());
        Object[] listeners = capturedListeners.getValue();

        assertEquals(2, listeners.length);
        assertEquals(new JakartaHttpSessionListener(listener), listeners[0]);
        assertSame(otherListener, listeners[1]);
    }

    @Test
    void removeSessionListener() {
        HttpSessionListener listener = mock(HttpSessionListener.class);
        ArgumentCaptor<Object[]> capturedListeners = ArgumentCaptor.forClass(Object[].class);
        Object otherListener = new Object();

        when(this.context.getApplicationLifecycleListeners()).thenReturn(new Object[] { otherListener, new JakartaHttpSessionListener(listener) });

        this.catalinaContext.removeSessionListener(listener);

        verify(this.context).setApplicationLifecycleListeners(capturedListeners.capture());
        Object[] listeners = capturedListeners.getValue();

        assertEquals(1, listeners.length);
        assertSame(otherListener, listeners[0]);
    }

    @Test
    void isDistributable() {
        when(this.context.getDistributable()).thenReturn(true);

        boolean result = this.catalinaContext.isDistributable();

        assertTrue(result);
    }
}
