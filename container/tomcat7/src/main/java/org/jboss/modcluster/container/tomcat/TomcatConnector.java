package org.jboss.modcluster.container.tomcat;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.RequestGroupInfo;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.threads.ResizableExecutor;
import org.jboss.modcluster.container.catalina.CatalinaConnector;

public class TomcatConnector extends CatalinaConnector {

    public TomcatConnector(Connector connector) {
        super(connector);
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

    @Override
    protected RequestGroupInfo getRequestGroupInfo(Object connectionHandler) {
        AbstractEndpoint.Handler handler = (AbstractEndpoint.Handler) connectionHandler;
        return (RequestGroupInfo) handler.getGlobal();
    }
}
