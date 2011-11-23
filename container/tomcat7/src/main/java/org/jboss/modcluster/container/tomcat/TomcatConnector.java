package org.jboss.modcluster.container.tomcat;

import java.net.InetAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.IntrospectionUtils;
import org.apache.tomcat.util.threads.ResizableExecutor;
import org.jboss.modcluster.container.catalina.CatalinaConnector;

public class TomcatConnector extends CatalinaConnector {

    public TomcatConnector(Connector connector) {
        super(connector);
    }

    @Override
    public void setAddress(InetAddress address) {
        IntrospectionUtils.setProperty(this.connector.getProtocolHandler(), "address", address.getHostAddress());
    }

    @Override
    public boolean isAvailable() {
        return this.connector.getState().isAvailable();
    }

    @Override
    public int getMaxThreads() {
        Executor executor = this.connector.getProtocolHandler().getExecutor();
        if (executor != null) {
            if (executor instanceof ThreadPoolExecutor) {
                return ((ThreadPoolExecutor) executor).getMaximumPoolSize();
            } else if (executor instanceof ResizableExecutor) {
                return ((ResizableExecutor) executor).getMaxThreads();
            }
        }
        return 0;
    }

    @Override
    public int getBusyThreads() {
        Executor executor = this.connector.getProtocolHandler().getExecutor();
        if (executor != null) {
            if (executor instanceof ThreadPoolExecutor) {
                return ((ThreadPoolExecutor) executor).getActiveCount();
            } else if (executor instanceof ResizableExecutor) {
                return ((ResizableExecutor) executor).getActiveCount();
            }
        }
        return 0;
    }
}
