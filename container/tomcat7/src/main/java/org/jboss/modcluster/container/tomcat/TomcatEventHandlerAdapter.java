package org.jboss.modcluster.container.tomcat;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.jboss.modcluster.container.ContainerEventHandler;
import org.jboss.modcluster.container.catalina.CatalinaEventHandlerAdapter;
import org.jboss.modcluster.container.catalina.CatalinaFactory;
import org.jboss.modcluster.container.catalina.ServerProvider;

public class TomcatEventHandlerAdapter extends CatalinaEventHandlerAdapter {

    public TomcatEventHandlerAdapter(ContainerEventHandler eventHandler) {
        super(eventHandler);
    }

    public TomcatEventHandlerAdapter(ContainerEventHandler eventHandler, ServerProvider serverProvider, CatalinaFactory factory) {
        super(eventHandler, serverProvider, factory);
    }

    @Override
    protected boolean isAfterInit(LifecycleEvent event) {
        return event.getType().equals(Lifecycle.AFTER_INIT_EVENT);
    }

    @Override
    protected boolean isBeforeDestroy(LifecycleEvent event) {
        return event.getType().equals(Lifecycle.BEFORE_DESTROY_EVENT);
    }
}
