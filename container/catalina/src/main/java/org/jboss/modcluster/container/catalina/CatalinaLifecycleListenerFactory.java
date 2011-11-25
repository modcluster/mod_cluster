package org.jboss.modcluster.container.catalina;

import org.apache.catalina.LifecycleListener;
import org.jboss.modcluster.container.ContainerEventHandler;

public class CatalinaLifecycleListenerFactory implements LifecycleListenerFactory {

    @Override
    public LifecycleListener createListener(ContainerEventHandler handler) {
        return new CatalinaEventHandlerAdapter(handler);
    }
}
