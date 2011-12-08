package org.jboss.modcluster.container.tomcat;

import static org.junit.Assert.assertSame;

import org.jboss.modcluster.container.catalina.CatalinaConnectorFactory;
import org.jboss.modcluster.container.catalina.CatalinaEngineFactory;
import org.jboss.modcluster.container.catalina.CatalinaFactoryRegistry;
import org.jboss.modcluster.container.catalina.CatalinaHostFactory;
import org.jboss.modcluster.container.catalina.CatalinaServerFactory;

public class ServiceLoaderCatalinaFactoryTestCase extends org.jboss.modcluster.container.catalina.ServiceLoaderCatalinaFactoryTestCase {
    @Override
    protected void verifyCatalinaFactoryTypes(CatalinaFactoryRegistry registry) {
        assertSame(registry.getServerFactory().getClass(), CatalinaServerFactory.class);
        assertSame(registry.getEngineFactory().getClass(), CatalinaEngineFactory.class);
        assertSame(registry.getHostFactory().getClass(), CatalinaHostFactory.class);
        assertSame(registry.getContextFactory().getClass(), TomcatContextFactory.class);
        assertSame(registry.getConnectorFactory().getClass(), CatalinaConnectorFactory.class);
    }
}
