package org.jboss.modcluster.container.jbossweb;

import static org.junit.Assert.assertSame;

import org.jboss.modcluster.container.catalina.CatalinaEngineFactory;
import org.jboss.modcluster.container.catalina.CatalinaFactoryRegistry;
import org.jboss.modcluster.container.catalina.CatalinaHostFactory;
import org.jboss.modcluster.container.catalina.CatalinaServerFactory;

public class ServiceLoaderCatalinaFactoryTestCase extends org.jboss.modcluster.container.catalina.ServiceLoaderCatalinaFactoryTestCase {
    @Override
    protected void verifyCatalinaFactoryTypes(CatalinaFactoryRegistry registry) {
        assertSame(registry.getServerFactory().getClass(), CatalinaServerFactory.class);
        assertSame(registry.getEngineFactory().getClass(), JBossWebEngineFactory.class);
        assertSame(registry.getHostFactory().getClass(), CatalinaHostFactory.class);
        assertSame(registry.getContextFactory().getClass(), JBossWebContextFactory.class);
        assertSame(registry.getConnectorFactory().getClass(), JBossWebConnectorFactory.class);
    }
}
