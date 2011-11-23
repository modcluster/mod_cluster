package org.jboss.modcluster.container.jbossweb;

import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Host;
import org.jboss.modcluster.container.catalina.ContextFactory;

public class JBossWebContextFactory implements ContextFactory {
    @Override
    public Context createContext(org.apache.catalina.Context context, Host host) {
        return new JBossWebContext(context, host);
    }
}
