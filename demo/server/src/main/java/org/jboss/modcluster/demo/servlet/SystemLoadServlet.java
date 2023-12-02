/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.demo.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Paul Ferraro
 */
public class SystemLoadServlet extends LoadServlet {
    /** The serialVersionUID */
    private static final long serialVersionUID = 5665079393261425098L;

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        long start = System.currentTimeMillis();

        int duration = Integer.parseInt(this.getParameter(request, DURATION, "15")) * 1000;

        this.log("Begin handling system load request");

        // Naughty loop
        while (System.currentTimeMillis() - start < duration) {
            if ((System.currentTimeMillis() % 10) == 0) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        this.log("End handling system load request");

        this.writeLocalName(request, response);
    }
}