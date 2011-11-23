package org.jboss.modcluster.container.tomcat;

import javax.management.MBeanServer;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.Server;
import org.jboss.modcluster.container.ContainerEventHandler;
import org.jboss.modcluster.container.catalina.CatalinaEventHandlerAdapter;

public class TomcatEventHandlerAdapter extends CatalinaEventHandlerAdapter {

    public TomcatEventHandlerAdapter(ContainerEventHandler eventHandler, MBeanServer mbeanServer) {
        super(eventHandler, mbeanServer);
    }

    public TomcatEventHandlerAdapter(ContainerEventHandler eventHandler, Server server, MBeanServer mbeanServer) {
        super(eventHandler, server, mbeanServer);
    }

    public TomcatEventHandlerAdapter(ContainerEventHandler eventHandler, Server server) {
        super(eventHandler, server);
    }

    public TomcatEventHandlerAdapter(ContainerEventHandler eventHandler) {
        super(eventHandler);
    }

    @Override
    protected boolean isAfterInit(LifecycleEvent event) {
        return event.getType().equals(Lifecycle.AFTER_INIT_EVENT);
    }
}
