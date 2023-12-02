/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.demo.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URI;

import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public class ActiveSessionsLoadServlet extends LoadServlet {

    private static final long serialVersionUID = -946741803216943778L;

    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(true);
        this.log("Handling session load request from: " + request.getRequestURL().toString() + ", using session id: " + session.getId());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int count = Integer.parseInt(this.getParameter(request, COUNT, "20"));

        URI uri = this.createLocalURI(request, null);

        this.log("Sending " + count + " requests to: " + uri);

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            for (int i = 0; i < count; ++i) {
                HttpClientUtils.closeQuietly(client.execute(new HttpHead(uri)));
            }
        }

        this.writeLocalName(request, response);
    }
}
