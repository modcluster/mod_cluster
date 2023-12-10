/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
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
