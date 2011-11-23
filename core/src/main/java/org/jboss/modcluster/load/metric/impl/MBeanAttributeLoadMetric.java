/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.modcluster.load.metric.impl;

import java.util.List;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.jboss.modcluster.container.Engine;

/**
 * Generic {@link LoadMetric} whose load is the aggregated value of an mbean attribute.
 * 
 * @author Paul Ferraro
 */
public class MBeanAttributeLoadMetric extends AbstractLoadMetric {
    private MBeanQuerySupport querySupport;
    private String attribute;

    public void setPattern(ObjectName pattern) {
        this.querySupport = new MBeanQuerySupport(pattern);
    }

    public void setPattern(String pattern) throws MalformedObjectNameException {
        this.querySupport = new MBeanQuerySupport(pattern);
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    @Override
    public double getLoad(Engine engine) throws Exception {
        double load = 0;
        List<Number> results = this.querySupport.getAttributes(engine.getServer().getMBeanServer(), this.attribute, Number.class);
        for (Number result : results) {
            load += result.doubleValue();
        }
        return load;
    }
}
