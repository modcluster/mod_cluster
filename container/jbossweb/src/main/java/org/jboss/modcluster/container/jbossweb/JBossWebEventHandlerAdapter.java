package org.jboss.modcluster.container.jbossweb;

import java.lang.management.ManagementFactory;

import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.apache.catalina.Engine;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.jboss.modcluster.container.ContainerEventHandler;
import org.jboss.modcluster.container.catalina.AutoProxyConnectorProvider;
import org.jboss.modcluster.container.catalina.CatalinaEventHandlerAdapter;
import org.jboss.modcluster.container.catalina.CatalinaFactory;
import org.jboss.modcluster.container.catalina.JMXServerProvider;
import org.jboss.modcluster.container.catalina.ServerProvider;

public class JBossWebEventHandlerAdapter extends CatalinaEventHandlerAdapter implements NotificationListener {

    private volatile ObjectName serviceObjectName = toObjectName("jboss.web:service=WebServer");
    private volatile String connectorsStartedNotificationType = "jboss.tomcat.connectors.started";
    private volatile String connectorsStoppedNotificationType = "jboss.tomcat.connectors.stopped";
    private final MBeanServer server;

    public JBossWebEventHandlerAdapter(ContainerEventHandler eventHandler) {
        this(eventHandler, ManagementFactory.getPlatformMBeanServer());
    }
    
    public JBossWebEventHandlerAdapter(ContainerEventHandler eventHandler, MBeanServer server) {
        super(eventHandler, new JMXServerProvider(server, toObjectName("jboss.web:type=Server")), new AutoProxyConnectorProvider());
        this.server = server;
    }

    public JBossWebEventHandlerAdapter(ContainerEventHandler eventHandler, MBeanServer server, ServerProvider serverProvider, CatalinaFactory factory) {
        super(eventHandler, serverProvider, factory);
        this.server = server;
    }

    @Override
    protected void init(Server server) {
        super.init(server);
        
        // Register for mbean notifications if JBoss Web server mbean exists
        if (this.server.isRegistered(this.serviceObjectName)) {
            try {
                this.server.addNotificationListener(this.serviceObjectName, this, null, server);
            } catch (InstanceNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    protected void destroy(Server server) {
        // Unregister for mbean notifications if JBoss Web server mbean exists
        if (this.server.isRegistered(this.serviceObjectName)) {
            try {
                this.server.removeNotificationListener(this.serviceObjectName, this);
            } catch (InstanceNotFoundException e) {
                throw new IllegalStateException(e);
            } catch (ListenerNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
        
        super.destroy(server);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
     */
    @Override
    public void handleNotification(Notification notification, Object object) {
        String type = notification.getType();

        if (type != null) {
            if (type.equals(this.connectorsStartedNotificationType)) {
                // In JBoss AS, connectors are the last to start, so trigger a status event to reset the proxy
                if (this.start.get()) {
                    for (Service service: ((Server) object).findServices()) {
                        this.eventHandler.status(this.factory.createEngine((Engine) service.getContainer()));
                    }
                }
            } else if (type.equals(this.connectorsStoppedNotificationType)) {
                // In JBoss AS, connectors are the first to stop, so handle this notification as a server stop event.
                if (this.init.get() && this.start.compareAndSet(true, false)) {
                    this.eventHandler.stop(this.factory.createServer((Server) object));
                }
            }
        }
    }

    /**
     * @param serviceObjectName the name to serverObjectName
     */
    public void setServiceObjectName(ObjectName serviceObjectName) {
        this.serviceObjectName = serviceObjectName;
    }

    /**
     * @param notificationType the notificationType to set
     */
    public void setConnectorsStoppedNotificationType(String type) {
        this.connectorsStoppedNotificationType = type;
    }

    /**
     * @param connectorsStartedNotificationType the connectorsStartedNotificationType to set
     */
    public void setConnectorsStartedNotificationType(String type) {
        this.connectorsStartedNotificationType = type;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        // Hack to encourage this notification listener
        // to trigger *after* any listener with 0 hashCode.
        return 1;
    }

    @Override
    protected boolean isAfterInit(LifecycleEvent event) {
        return event.getType().equals(Lifecycle.INIT_EVENT);
    }

    @Override
    protected boolean isBeforeDestroy(LifecycleEvent event) {
        return event.getType().equals(Lifecycle.DESTROY_EVENT);
    }
}
