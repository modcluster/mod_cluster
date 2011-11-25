package org.jboss.modcluster.container.tomcat;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.Server;
import org.jboss.modcluster.container.ContainerEventHandler;
import org.jboss.modcluster.container.catalina.CatalinaEventHandlerAdapter;
import org.jboss.modcluster.container.catalina.CatalinaFactory;
import org.jboss.modcluster.container.catalina.ServerProvider;

public class TomcatEventHandlerAdapter extends CatalinaEventHandlerAdapter {

    public TomcatEventHandlerAdapter(ContainerEventHandler eventHandler) {
        super(eventHandler);
    }

    public TomcatEventHandlerAdapter(ContainerEventHandler eventHandler, Server server) {
        super(eventHandler, server);
    }

    public TomcatEventHandlerAdapter(ContainerEventHandler eventHandler, ServerProvider provider) {
        super(eventHandler, provider);
    }

    public TomcatEventHandlerAdapter(ContainerEventHandler eventHandler, ServerProvider provider, CatalinaFactory factory) {
        super(eventHandler, provider, factory);
    }

    @Override
    protected boolean isAfterInit(LifecycleEvent event) {
        return event.getType().equals(Lifecycle.AFTER_INIT_EVENT);
    }
}
