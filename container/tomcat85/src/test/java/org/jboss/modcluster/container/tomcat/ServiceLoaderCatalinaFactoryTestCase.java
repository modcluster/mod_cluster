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

import org.jboss.modcluster.container.catalina.CatalinaConnectorFactory;
import org.jboss.modcluster.container.catalina.CatalinaEngineFactory;
import org.jboss.modcluster.container.catalina.CatalinaFactoryRegistry;
import org.jboss.modcluster.container.catalina.CatalinaHostFactory;

import static org.junit.Assert.assertSame;

/**
 * @author Paul Ferraro
 * @author Radoslav Husar
 * @version May 2016
 */
public class ServiceLoaderCatalinaFactoryTestCase extends org.jboss.modcluster.container.catalina.ServiceLoaderCatalinaFactoryTestCase {
    @Override
    protected void verifyCatalinaFactoryTypes(CatalinaFactoryRegistry registry) {
        assertSame(registry.getServerFactory().getClass(), TomcatServerFactory.class);
        assertSame(registry.getEngineFactory().getClass(), CatalinaEngineFactory.class);
        assertSame(registry.getHostFactory().getClass(), CatalinaHostFactory.class);
        assertSame(registry.getContextFactory().getClass(), TomcatContextFactory.class);
        assertSame(registry.getConnectorFactory().getClass(), CatalinaConnectorFactory.class);
    }
}
