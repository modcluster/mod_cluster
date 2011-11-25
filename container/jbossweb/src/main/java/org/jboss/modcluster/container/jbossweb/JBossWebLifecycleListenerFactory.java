package org.jboss.modcluster.container.jbossweb;

import org.apache.catalina.LifecycleListener;
import org.jboss.modcluster.container.ContainerEventHandler;
import org.jboss.modcluster.container.catalina.LifecycleListenerFactory;

public class JBossWebLifecycleListenerFactory implements LifecycleListenerFactory {

    @Override
    public LifecycleListener createListener(ContainerEventHandler handler) {
        return new JBossWebEventHandlerAdapter(handler);
    }
}
