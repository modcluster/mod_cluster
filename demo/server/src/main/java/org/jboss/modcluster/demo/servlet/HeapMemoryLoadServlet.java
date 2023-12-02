/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.demo.servlet;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Paul Ferraro
 */
public class HeapMemoryLoadServlet extends LoadServlet {
    /** The serialVersionUID */
    private static final long serialVersionUID = -8183119455180366670L;

    private static final String RATIO = "ratio";
    private static final String DEFAULT_RATIO = "0.9";

    private static final int MB = 1024 * 1024;

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int duration = Integer.parseInt(this.getParameter(request, DURATION, DEFAULT_DURATION)) * 1000;
        float ratio = Float.parseFloat(this.getParameter(request, RATIO, DEFAULT_RATIO));

        System.gc();

        MemoryUsage usage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();

        long max = usage.getMax();
        long total = (max >= 0) ? max : usage.getCommitted();
        long free = total - usage.getUsed();

        long reserve = (long) (free * ratio);

        this.log((total / MB) + "MB total memory");
        this.log((free / MB) + "MB free memory");
        this.log("Reserving " + (reserve / MB) + "MB (" + ((int) (ratio * 100)) + "%) of memory");

        List<Object> list = new ArrayList<Object>(2);

        if (free > Integer.MAX_VALUE) {
            list.add(new byte[(int) (reserve / Integer.MAX_VALUE)][Integer.MAX_VALUE]);
        }

        list.add(new byte[(int) (reserve % Integer.MAX_VALUE)]);

        try {

            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        this.log("Freeing " + (reserve / MB) + "MB of memory");

        list.clear();

        System.gc();

        this.writeLocalName(request, response);
    }
}
