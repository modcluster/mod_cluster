package org.jboss.modcluster.container.catalina;

import org.apache.catalina.LifecycleListener;
import org.jboss.modcluster.container.ContainerEventHandler;

public interface LifecycleListenerFactory {
    LifecycleListener createListener(ContainerEventHandler handler);
}
