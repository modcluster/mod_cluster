package org.jboss.modcluster.container.catalina;

import java.beans.PropertyChangeListener;

import org.apache.catalina.ContainerListener;
import org.apache.catalina.LifecycleListener;

public interface CatalinaEventHandler extends LifecycleListener, ContainerListener, PropertyChangeListener {

    void start();
    
    void stop();
}
