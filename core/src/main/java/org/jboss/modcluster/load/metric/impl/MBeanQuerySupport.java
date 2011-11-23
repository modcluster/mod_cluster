package org.jboss.modcluster.load.metric.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public class MBeanQuerySupport {
    private final ObjectName pattern;

    public MBeanQuerySupport(String pattern) throws MalformedObjectNameException {
        this(ObjectName.getInstance(pattern));
    }

    public MBeanQuerySupport(ObjectName pattern) {
        this.pattern = pattern;
    }

    /**
     * Collects the attribute values for each mbean matching the object name pattern
     * 
     * @param <T> the type of the attribute
     * @param attribute the mbean attribute name
     * @param targetClass the type of the attribute
     * @return a list of attribute values for each mbean
     * @throws JMException
     */
    public <T> List<T> getAttributes(MBeanServer server, String attribute, Class<T> targetClass) throws JMException {
        Set<ObjectName> names = server.queryNames(this.pattern, null);
        List<T> list = new ArrayList<T>(names.size());
        for (ObjectName name : names) {
            list.add(targetClass.cast(this.getAttribute(server, name, attribute)));
        }
        return list;
    }

    private Object getAttribute(MBeanServer server, ObjectName name, String attribute) throws JMException {
        try {
            return server.getAttribute(name, attribute);
        } catch (AttributeNotFoundException e) {
            // MODCLUSTER-29
            // Try again, reversing case of the first letter of the attribute
            StringBuilder builder = new StringBuilder(attribute.length());
            char first = attribute.charAt(0);
            builder.append(Character.isLowerCase(first) ? Character.toUpperCase(first) : Character.toLowerCase(first));
            builder.append(attribute.substring(1));

            try {
                return server.getAttribute(name, builder.toString());
            } catch (AttributeNotFoundException e2) {
                // Throw original exception
                throw e;
            }
        }
    }
}
