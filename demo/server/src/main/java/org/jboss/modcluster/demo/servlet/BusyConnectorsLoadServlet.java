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

import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * @author Paul Ferraro
 *
 */
public class BusyConnectorsLoadServlet extends LoadServlet {

    private static final long serialVersionUID = -946741803216943778L;

    private static final String END = "end";

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String parameter = request.getParameter(END);

        if (parameter == null) {
            int duration = Integer.parseInt(this.getParameter(request, DURATION, "15")) * 1000;

            long end = System.currentTimeMillis() + duration;

            URI uri = this.createLocalURI(request, Collections.singletonMap(END, String.valueOf(end)));
            Runnable task = new ExecuteMethodTask(uri);

            int count = Integer.parseInt(this.getParameter(request, COUNT, "50"));

            this.log("Sending " + count + " concurrent requests to: " + uri);

            Thread[] threads = new Thread[count];

            for (int i = 0; i < count; ++i) {
                threads[i] = new Thread(task);
            }

            for (int i = 0; i < count; ++i) {
                threads[i].start();
            }

            for (int i = 0; i < count; ++i) {
                try {
                    threads[i].join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            this.writeLocalName(request, response);
        } else {
            long end = Long.parseLong(parameter);

            if (end > System.currentTimeMillis()) {
                URI uri = this.createLocalURI(request, Collections.singletonMap(END, String.valueOf(end)));
                response.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
                response.setHeader("location", response.encodeRedirectURL(uri.toString()));
            }
        }
    }

    private class ExecuteMethodTask implements Runnable {
        private final URI uri;

        ExecuteMethodTask(URI uri) {
            this.uri = uri;
        }

        @Override
        public void run() {
            URI uri = this.uri;

            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                while (uri != null) {
                    // Disable auto redirect following, to allow circular redirect
                    RequestConfig requestConfig = RequestConfig.custom().setCircularRedirectsAllowed(true).build();

                    HttpHead head = new HttpHead(uri);
                    head.setConfig(requestConfig);

                    HttpResponse response = client.execute(head);
                    try {
                        int code = response.getStatusLine().getStatusCode();

                        uri = (code == HttpServletResponse.SC_TEMPORARY_REDIRECT) ? URI.create(response.getFirstHeader("location").getValue()) : null;
                    } finally {
                        HttpClientUtils.closeQuietly(response);
                    }
                }
            } catch (IOException e) {
                BusyConnectorsLoadServlet.this.log(e.getLocalizedMessage(), e);
            }
        }
    }
}
