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
package org.jboss.modcluster.advertise.impl;

import java.util.Date;
import java.util.HashMap;

/**
 * Advertised server instance
 *
 * @author Mladen Turk
 */
public class AdvertisedServer {
    /** Manager-Address header */
    public static final String MANAGER_ADDRESS = "X-Manager-Address";

    private String server;
    private Date date;
    private int status;
    private String status_desc;
    private HashMap<String, String> headers = new HashMap<String, String>();

    protected AdvertisedServer(String server) {
        this.server = server;
    }

    protected boolean setStatus(int status, String desc) {
        boolean rv = false;
        this.status_desc = desc;
        if (this.status == 0) {
            // First time
            this.status = status;
        } else if (this.status != status) {
            this.status = status;
            rv = true;
        }
        return rv;
    }

    /**
     * Set the Date of the last Advertise message
     */
    protected void setDate(Date date) {
        this.date = date;
    }

    /**
     * Set the Header
     */
    protected void setParameter(String name, String value) {
        this.headers.put(name, value);
    }

    /**
     * Get Date of the last Advertise message
     */
    public Date getDate() {
        return this.date;
    }

    /**
     * Get Status code of the last Advertise message
     */
    public int getStatusCode() {
        return this.status;
    }

    /**
     * Get Status description of the last Advertise message
     */
    public String getStatusDescription() {
        return this.status_desc;
    }

    /**
     * Get Advertise parameter
     */
    public String getParameter(String name) {
        return this.headers.get(name);
    }

    @Override
    public String toString() {
        return this.server;
    }
}
