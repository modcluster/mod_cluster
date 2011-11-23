package org.jboss.modcluster.container.catalina;

import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Host;

public interface ContextFactory {
    Context createContext(org.apache.catalina.Context context, Host host);
}
