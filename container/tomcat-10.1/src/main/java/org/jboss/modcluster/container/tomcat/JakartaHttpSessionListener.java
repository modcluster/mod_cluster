/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.container.tomcat;

import jakarta.servlet.http.HttpSessionEvent;

import org.jboss.modcluster.container.listeners.HttpSessionListener;

/**
 * Adapts {@link HttpSessionListener} to {@link jakarta.servlet.http.HttpSessionListener}.
 *
 * @author Radoslav Husar
 */
public class JakartaHttpSessionListener implements jakarta.servlet.http.HttpSessionListener {

    private final HttpSessionListener sessionListener;

    public JakartaHttpSessionListener(HttpSessionListener sessionListener) {
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

        JakartaHttpSessionListener that = (JakartaHttpSessionListener) o;

        return sessionListener.equals(that.sessionListener);
    }

    @Override
    public int hashCode() {
        return this.sessionListener.hashCode();
    }
}