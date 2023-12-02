/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.demo.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author Paul Ferraro
 */
public class RecordServlet extends LoadServlet {
    /** The serialVersionUID */
    private static final long serialVersionUID = -4143320241936636855L;

    private static final String DESTROY = "destroy";
    private static final String TIMEOUT = "timeout";

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(true);

        boolean destroy = Boolean.valueOf(request.getParameter(DESTROY));

        if (destroy) {
            session.invalidate();
        } else {
            String timeout = request.getParameter(TIMEOUT);

            if (timeout != null) {
                session.setMaxInactiveInterval(Integer.valueOf(timeout));
            }
        }

        if (!request.getAttributeNames().hasMoreElements()) {
            System.out.println("No request attributes");
        }
        for (String attribute: java.util.Collections.list(request.getAttributeNames())) {
            System.out.println(attribute + " = " + request.getAttribute(attribute));
        }
        response.setHeader("X-ClusterNode", this.getJvmRoute());

        this.writeLocalName(request, response);
    }
}
