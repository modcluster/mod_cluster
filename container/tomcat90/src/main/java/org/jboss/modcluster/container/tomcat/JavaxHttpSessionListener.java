/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat Middleware LLC, and individual contributors
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

import javax.servlet.http.HttpSessionEvent;

import org.jboss.modcluster.container.listeners.HttpSessionListener;

/**
 * Adapts {@link HttpSessionListener} to {@link javax.servlet.http.HttpSessionListener}.
 *
 * @author Radoslav Husar
 */
public class JavaxHttpSessionListener implements javax.servlet.http.HttpSessionListener {

    private final HttpSessionListener sessionListener;

    public JavaxHttpSessionListener(HttpSessionListener sessionListener) {
        this.sessionListener = sessionListener;
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        this.sessionListener.sessionCreated();
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        this.sessionListener.sessionDestroyed();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JavaxHttpSessionListener that = (JavaxHttpSessionListener) o;

        return sessionListener.equals(that.sessionListener);
    }

    @Override
    public int hashCode() {
        return this.sessionListener.hashCode();
    }
}