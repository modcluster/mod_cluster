/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.modcluster.container.tomcat;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.RequestGroupInfo;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.threads.ResizableExecutor;
import org.jboss.modcluster.container.catalina.CatalinaConnector;

/**
 * @author Paul Ferraro
 * @author Radoslav Husar
 * @version Mar 2016
 */
public class TomcatConnector extends CatalinaConnector {

    public TomcatConnector(Connector connector) {
        super(connector);
    }

    @Override
    public boolean isAvailable() {
        return LifecycleState.STARTED.equals(this.connector.getState());
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
