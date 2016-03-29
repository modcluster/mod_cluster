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
package org.jboss.modcluster.container.jbossweb;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.catalina.Pipeline;
import org.apache.catalina.Valve;
import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Host;
import org.junit.Test;
import org.mockito.ArgumentCaptor;


/**
 * @author Paul Ferraro
 *
 */
public class ContextTestCase extends org.jboss.modcluster.container.catalina.ContextTestCase {
    @Override
    protected Context createContext(org.apache.catalina.Context context, Host host) {
        return new JBossWebContext(context, host);
    }

    @Override
    public void isStarted() {
        when(this.context.isStarted()).thenReturn(false);
        
        boolean result = this.catalinaContext.isStarted();
        
        assertFalse(result);
        
        when(this.context.isStarted()).thenReturn(true);
        when(this.context.getAvailable()).thenReturn(false);
    
        result = this.catalinaContext.isStarted();
    
        assertFalse(result);
    
        when(this.context.getAvailable()).thenReturn(true);
    
        result = this.catalinaContext.isStarted();
    
        assertTrue(result);
    }

    @Override
    public void requestListener() throws IOException, ServletException {
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
    public void addSessionListener() {
        HttpSessionListener listener = mock(HttpSessionListener.class);
        ArgumentCaptor<Object[]> capturedListeners = ArgumentCaptor.forClass(Object[].class);
        Object otherListener = new Object();

        when(this.context.getApplicationSessionLifecycleListeners()).thenReturn(new Object[] { otherListener });

        this.catalinaContext.addSessionListener(listener);

        verify(this.context).setApplicationSessionLifecycleListeners(capturedListeners.capture());
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

        when(this.context.getApplicationSessionLifecycleListeners()).thenReturn(new Object[] { otherListener, listener });

        this.catalinaContext.removeSessionListener(listener);

        verify(this.context).setApplicationSessionLifecycleListeners(capturedListeners.capture());
        Object[] listeners = capturedListeners.getValue();

        assertEquals(1, listeners.length);
        assertSame(otherListener, listeners[0]);
    }
}
