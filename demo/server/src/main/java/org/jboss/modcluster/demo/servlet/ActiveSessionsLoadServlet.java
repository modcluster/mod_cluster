/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.modcluster.demo.servlet;

import java.io.IOException;
import java.net.URI;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * @author Paul Ferraro
 *
 */
public class ActiveSessionsLoadServlet extends LoadServlet {
    /** The serialVersionUID */
    private static final long serialVersionUID = -946741803216943778L;

    /**
     * @{inheritDoc
     * @see javax.servlet.http.HttpServlet#doHead(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(true);
        this.log("Handling session load request from: " + request.getRequestURL().toString() + ", using session id: " + session.getId());
    }

    /**
     * @{inheritDoc
     * @see javax.servlet.http.HttpServlet#service(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int count = Integer.parseInt(this.getParameter(request, COUNT, "20"));

        URI uri = this.createLocalURI(request, null);

        this.log("Sending " + count + " requests to: " + uri);

        HttpClient client = new DefaultHttpClient();
        try {
            for (int i = 0; i < count; ++i) {
                HttpClientUtils.closeQuietly(client.execute(new HttpHead(uri)));
            }
        } finally {
            HttpClientUtils.closeQuietly(client);
        }

        this.writeLocalName(request, response);
    }
}
