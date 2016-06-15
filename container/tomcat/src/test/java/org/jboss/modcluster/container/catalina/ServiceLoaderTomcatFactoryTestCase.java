package org.jboss.modcluster.container.catalina;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Host;
import org.jboss.modcluster.container.Server;
import org.jboss.modcluster.container.tomcat.TomcatConnectorFactory;
import org.jboss.modcluster.container.tomcat.TomcatContextFactory;
import org.jboss.modcluster.container.tomcat.TomcatEngineFactory;
import org.jboss.modcluster.container.tomcat.TomcatFactoryRegistry;
import org.jboss.modcluster.container.tomcat.TomcatHostFactory;
import org.jboss.modcluster.container.tomcat.TomcatServerFactory;
import org.jboss.modcluster.container.tomcat.ConnectorFactory;
import org.jboss.modcluster.container.tomcat.ContextFactory;
import org.jboss.modcluster.container.tomcat.EngineFactory;
import org.jboss.modcluster.container.tomcat.HostFactory;
import org.jboss.modcluster.container.tomcat.ProxyConnectorProvider;
import org.jboss.modcluster.container.tomcat.ServerFactory;
import org.jboss.modcluster.container.tomcat.ServiceLoaderTomcatFactory;
import org.junit.Test;

public class ServiceLoaderTomcatFactoryTestCase {

    private final ServerFactory serverFactory = mock(ServerFactory.class);
    private final EngineFactory engineFactory = mock(EngineFactory.class);
    private final HostFactory hostFactory = mock(HostFactory.class);
    private final ContextFactory contextFactory = mock(ContextFactory.class);
    private final ConnectorFactory connectorFactory = mock(ConnectorFactory.class);
    private final ProxyConnectorProvider provider = mock(ProxyConnectorProvider.class);
    
    @Test
    public void testCatalinaFactoryRegistry() {
        TomcatFactoryRegistry registry = new ServiceLoaderTomcatFactory(this.serverFactory, this.engineFactory, this.hostFactory, this.contextFactory, this.connectorFactory, this.provider);
        
        assertSame(this.serverFactory, registry.getServerFactory());
        assertSame(this.engineFactory, registry.getEngineFactory());
        assertSame(this.hostFactory, registry.getHostFactory());
        assertSame(this.contextFactory, registry.getContextFactory());
        assertSame(this.connectorFactory, registry.getConnectorFactory());
        assertSame(this.provider, registry.getProxyConnectorProvider());
    }
    
    @Test
    public void testCatalinaFactories() throws Exception {
        ServiceLoaderTomcatFactory factory = new ServiceLoaderTomcatFactory(this.serverFactory, this.engineFactory, this.hostFactory, this.contextFactory, this.connectorFactory, this.provider);
        
        org.apache.catalina.Server catalinaServer = mock(org.apache.catalina.Server.class);
        Server server = mock(Server.class);
        
        when(this.serverFactory.createServer(same(factory), same(catalinaServer))).thenReturn(server);
        
        assertSame(server, factory.createServer(catalinaServer));
        
        org.apache.catalina.Service service = mock(org.apache.catalina.Service.class);
        org.apache.catalina.Engine catalinaEngine = mock(org.apache.catalina.Engine.class);
        Engine engine = mock(Engine.class);
        
        when(catalinaEngine.getService()).thenReturn(service);
        when(service.getServer()).thenReturn(catalinaServer);
        when(this.engineFactory.createEngine(same(factory), same(catalinaEngine), same(server))).thenReturn(engine);
        
        assertSame(engine, factory.createEngine(catalinaEngine));
        
        org.apache.catalina.Host catalinaHost = mock(org.apache.catalina.Host.class);
        Host host = mock(Host.class);
        
        when(catalinaHost.getParent()).thenReturn(catalinaEngine);
        when(this.hostFactory.createHost(same(factory), same(catalinaHost), same(engine))).thenReturn(host);
        
        assertSame(host, factory.createHost(catalinaHost));
        
        org.apache.catalina.Context catalinaContext = mock(org.apache.catalina.Context.class);
        Context context = mock(Context.class);
        
        when(catalinaContext.getParent()).thenReturn(catalinaHost);
        when(this.contextFactory.createContext(same(catalinaContext), same(host))).thenReturn(context);
        
        assertSame(context, factory.createContext(catalinaContext));
    }
    
    @Test
    public void testServiceLoader() {
        this.verifyCatalinaFactoryTypes(new ServiceLoaderTomcatFactory(this.provider));
    }
    
    protected void verifyCatalinaFactoryTypes(TomcatFactoryRegistry registry) {
        assertSame(TomcatServerFactory.class, registry.getServerFactory().getClass());
        assertSame(TomcatEngineFactory.class, registry.getEngineFactory().getClass());
        assertSame(TomcatHostFactory.class, registry.getHostFactory().getClass());
        assertSame(TomcatContextFactory.class, registry.getContextFactory().getClass());
        assertSame(TomcatConnectorFactory.class, registry.getConnectorFactory().getClass());
        assertSame(this.provider, registry.getProxyConnectorProvider());
    }
}
