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

package org.jboss.modcluster.demo.client;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * @author Brian Stansberry
 * 
 */
public class ChartManager {
    private final Map<String, AtomicInteger> requestCounts;
    private final Map<String, AtomicInteger> sessionCounts;
    private final Map<String, Integer> lastRequestCounts = new HashMap<String, Integer>();
    private final Map<String, XYSeries> requestSeries = new HashMap<String, XYSeries>();
    private final Map<String, XYSeries> sessionSeries = new HashMap<String, XYSeries>();
    private final XYSeriesCollection requestSeriesCollection = new XYSeriesCollection();
    private final XYSeriesCollection sessionSeriesCollection = new XYSeriesCollection();
    private final JFreeChart requestBalancingChart;
    private final JFreeChart sessionBalancingChart;
    private long lastUpdateTime = 0;
    private int seriesCount;

    public ChartManager(Map<String, AtomicInteger> requestCounts, Map<String, AtomicInteger> sessionCounts) {
        this.requestCounts = requestCounts;
        this.sessionCounts = sessionCounts;

        requestBalancingChart = ChartFactory.createXYLineChart("Request Balancing", "Sample", "Requests / Second",
                requestSeriesCollection, PlotOrientation.VERTICAL, true, true, false);
        sessionBalancingChart = ChartFactory.createXYLineChart("Session Balancing", "Sample", "Session Count",
                sessionSeriesCollection, PlotOrientation.VERTICAL, true, true, false);

        // for (int i = 1; i < 9; i++)
        // {
        // String key = "cluster0" + i;
        // createRequestSeries(key);
        // createSessionSeries(key);
        // }
    }

    public JFreeChart getRequestBalancingChart() {
        return this.requestBalancingChart;
    }

    public JFreeChart getSessionBalancingChart() {
        return this.sessionBalancingChart;
    }

    public void start() {
        this.lastRequestCounts.clear();
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public void updateStats() {
        Integer xValue = new Integer(++seriesCount);

        long now = System.currentTimeMillis();
        long elapsed = (now - lastUpdateTime) / 1000l;
        // I once saw a DivideByZeroException below
        if (elapsed == 0) {
            seriesCount--;
            return;
        }

        this.lastUpdateTime = now;

        for (Map.Entry<String, AtomicInteger> entry : requestCounts.entrySet()) {
            String key = entry.getKey();
            Integer current = new Integer(entry.getValue().get());
            Integer last = lastRequestCounts.put(key, current);
            if (last == null) {
                last = new Integer(0);
            }

            int perSec = (int) ((current.intValue() - last.intValue()) / elapsed);

            XYSeries series = requestSeries.get(key);
            if (series == null) {
                series = createRequestSeries(key);
            }

            series.add(xValue, new Integer(perSec));
        }

        for (Map.Entry<String, AtomicInteger> entry : sessionCounts.entrySet()) {
            String key = entry.getKey();
            XYSeries series = sessionSeries.get(key);
            if (series == null) {
                series = createSessionSeries(key);
            }

            series.add(xValue, new Integer(entry.getValue().get()));
        }
    }

    private XYSeries createSessionSeries(String key) {
        XYSeries series = new XYSeries(key);
        series.setMaximumItemCount(20);
        sessionSeries.put(key, series);
        sessionSeriesCollection.addSeries(series);
        return series;
    }

    private XYSeries createRequestSeries(String key) {
        XYSeries series = new XYSeries(key);
        series.setMaximumItemCount(20);
        requestSeries.put(key, series);
        requestSeriesCollection.addSeries(series);
        return series;
    }

}
