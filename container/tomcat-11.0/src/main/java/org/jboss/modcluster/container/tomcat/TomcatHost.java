/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.container.tomcat;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.catalina.Container;
import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Host;

/**
 * {@link Host} implementation that wraps a {@link org.apache.catalina.Host}.
 *
 * @author Paul Ferraro
 */
public class TomcatHost implements Host {
    protected final TomcatRegistry registry;
    protected final org.apache.catalina.Host host;
    protected final Engine engine;

    /**
     * Constructs a new CatalinaHost wrapping the specified catalina host.
     *
     * @param host a catalina host
     */
    public TomcatHost(TomcatRegistry registry, org.apache.catalina.Host host) {
        this.registry = registry;
        this.host = host;
        this.engine = new TomcatEngine(registry, (org.apache.catalina.Engine) host.getParent());
    }

    @Override
    public Set<String> getAliases() {
        String name = this.host.getName();
        String[] aliases = this.host.findAliases();

        if (aliases.length == 0) {
            return Collections.singleton(name);
        }

        Set<String> hosts = new LinkedHashSet<String>();

        hosts.add(name);

        hosts.addAll(Arrays.asList(aliases));

        return hosts;
    }

    @Override
    public Iterable<Context> getContexts() {
        final Iterator<Container> children = Arrays.asList(this.host.findChildren()).iterator();

        final Iterator<Context> contexts = new Iterator<Context>() {
            @Override
            public boolean hasNext() {
                return children.hasNext();
            }

            @Override
            public Context next() {
                return new TomcatContext(registry, (org.apache.catalina.Context) children.next());
            }

            @Override
            public void remove() {
                children.remove();
            }
        };

        return new Iterable<Context>() {
            @Override
            public Iterator<Context> iterator() {
                return contexts;
            }
        };
    }

    @Override
    public Engine getEngine() {
        return this.engine;
    }

    @Override
    public String getName() {
        return this.host.getName();
    }

    @Override
    public Context findContext(String path) {
        org.apache.catalina.Context context = (org.apache.catalina.Context) this.host.findChild(path);

        return (context != null) ? new TomcatContext(registry, context) : null;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof TomcatHost)) return false;

        TomcatHost host = (TomcatHost) object;

        return this.host == host.host;
    }

    @Override
    public int hashCode() {
        return this.host.hashCode();
    }

    @Override
    public String toString() {
        return this.host.getName();
    }
}
