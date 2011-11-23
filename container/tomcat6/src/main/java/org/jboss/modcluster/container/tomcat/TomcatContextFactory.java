package org.jboss.modcluster.container.tomcat;

import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Host;
import org.jboss.modcluster.container.catalina.ContextFactory;

public class TomcatContextFactory implements ContextFactory {
    @Override
    public Context createContext(org.apache.catalina.Context context, Host host) {
        return new TomcatContext(context, host);
    }
}
