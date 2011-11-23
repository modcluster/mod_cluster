package org.jboss.modcluster.container.catalina;

import java.beans.PropertyChangeListener;

import javax.management.NotificationListener;

import org.apache.catalina.ContainerListener;
import org.apache.catalina.LifecycleListener;

public interface CatalinaEventHandler extends LifecycleListener, ContainerListener, NotificationListener, PropertyChangeListener {

    void start();
    
    void stop();
}
