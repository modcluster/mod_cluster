package org.jboss.modcluster.load.metric.impl;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.jboss.modcluster.container.Engine;

public abstract class AbstractMBeanLoadMetric extends AbstractLoadMetric {
    private MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    private ObjectName pattern;

    public void setPattern(ObjectName pattern) {
        this.pattern = pattern;
    }

    public void setPattern(String pattern) throws MalformedObjectNameException {
        this.setPattern(ObjectName.getInstance(pattern));
    }

    public void setMBeanServer(MBeanServer server) {
        this.server = server;
    }

    protected <T> List<T> getAttributes(String attribute, Class<T> targetClass) throws JMException {
        Set<ObjectName> names = this.server.queryNames(this.pattern, null);
        List<T> list = new ArrayList<T>(names.size());
        for (ObjectName name : names) {
            list.add(targetClass.cast(this.server.getAttribute(name, attribute)));
        }
        return list;
    }

    @Override
    public double getLoad(Engine engine) throws JMException {
        return (this.pattern != null) ? this.getLoad() : 0;
    }

    protected abstract double getLoad() throws JMException;
}
