package org.jboss.modcluster.container.catalina;

import javax.servlet.ServletRequestListener;

import org.apache.catalina.Valve;

public interface RequestListenerValveFactory {
    Valve createValve(ServletRequestListener listener);
}
