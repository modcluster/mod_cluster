package org.jboss.modcluster.container.catalina;

import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Host;

public class CatalinaContextFactory implements ContextFactory {
    @Override
    public Context createContext(org.apache.catalina.Context context, Host host) {
        return new CatalinaContext(context, host, null);
    }
}
