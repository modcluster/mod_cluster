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

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * @author Paul Ferraro
 */
public class ReceiveTrafficLoadServlet extends LoadServlet {

    private static final long serialVersionUID = 2344830128026153418L;
    private static final String SIZE = "size";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int duration = Integer.parseInt(this.getParameter(request, DURATION, DEFAULT_DURATION));

        int size = Integer.parseInt(this.getParameter(request, SIZE, "100")) * 1024;

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpEntity entity = new ByteArrayEntity(new byte[size]);

            URI uri = this.createLocalURI(request, null);

            for (int i = 0; i < duration; ++i) {
                long start = System.currentTimeMillis();

                this.log("Sending " + (size / 1024) + "KB packet to: " + uri);

                HttpPut put = new HttpPut(uri);
                put.setEntity(entity);

                HttpClientUtils.closeQuietly(client.execute(put));

                long ms = 1000 - (System.currentTimeMillis() - start);

                if (ms > 0) {
                    try {
                        Thread.sleep(ms);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        this.writeLocalName(request, response);
    }
}
