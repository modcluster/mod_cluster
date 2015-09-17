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
package org.jboss.modcluster.container;

import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionListener;

/**
 * SPI for a web application context.
 * 
 * @author Paul Ferraro
 */
public interface Context {
    /**
     * @return
     */
    Host getHost();

    /**
     * @return
     */
    String getPath();

    /**
     * @return
     */
    boolean isStarted();

    /**
     * @param listener
     */
    void addRequestListener(ServletRequestListener listener);

    /**
     * @param listener
     */
    void removeRequestListener(ServletRequestListener listener);

    /**
     * Adds the specified session listener to this context.
     * 
     * @param listener a session listener
     */
    void addSessionListener(HttpSessionListener listener);

    /**
     * Removes the specified session listener to this context.
     * 
     * @param listener a session listener
     */
    void removeSessionListener(HttpSessionListener listener);

    /**
     * Returns the number of active sessions for this context.
     * 
     * @return the active session count
     */
    int getActiveSessionCount();

    /**
     * Indicates whether this context is distributable.
     * 
     * @return true, if this context distributes sessions, false otherwise
     */
    boolean isDistributable();
    
    /**
     * Configure the jvmRoute for this context.
     * 
     * @param jvmRoute value of the jvmRoute
     */
    void configureJvmRoute(String jvmRoute);    	
}
