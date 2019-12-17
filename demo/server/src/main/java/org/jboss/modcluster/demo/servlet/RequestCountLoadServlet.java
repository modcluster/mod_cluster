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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * @author Paul Ferraro
 */
public class RequestCountLoadServlet extends LoadServlet {

    private static final long serialVersionUID = -5001091954463802789L;

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int count = Integer.parseInt(this.getParameter(request, COUNT, "50"));

        if (count > 1) {
            URI uri = this.createLocalURI(request, Collections.singletonMap(COUNT, String.valueOf(count - 1)));

            this.log("Sending request count request to: " + uri);

            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                HttpClientUtils.closeQuietly(client.execute(new HttpGet(uri)));
            }
        }

        this.writeLocalName(request, response);
    }
}
