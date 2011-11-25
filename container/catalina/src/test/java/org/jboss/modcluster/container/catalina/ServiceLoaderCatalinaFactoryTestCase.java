package org.jboss.modcluster.container.catalina;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Host;
import org.jboss.modcluster.container.Server;
import org.junit.Test;

public class ServiceLoaderCatalinaFactoryTestCase {

    private final ServerFactory serverFactory = mock(ServerFactory.class);
    private final EngineFactory engineFactory = mock(EngineFactory.class);
    private final HostFactory hostFactory = mock(HostFactory.class);
    private final ContextFactory contextFactory = mock(ContextFactory.class);
    private final ConnectorFactory connectorFactory = mock(ConnectorFactory.class);
    
    @Test
    public void testCatalinaFactoryRegistry() {
        CatalinaFactoryRegistry registry = new ServiceLoaderCatalinaFactory(this.serverFactory, this.engineFactory, this.hostFactory, this.contextFactory, this.connectorFactory);
        
        assertSame(this.serverFactory, registry.getServerFactory());
        assertSame(this.engineFactory, registry.getEngineFactory());
        assertSame(this.hostFactory, registry.getHostFactory());
        assertSame(this.contextFactory, registry.getContextFactory());
        assertSame(this.connectorFactory, registry.getConnectorFactory());
    }
    
    @Test
    public void testCatalinaFactories() throws Exception {
        ServiceLoaderCatalinaFactory factory = new ServiceLoaderCatalinaFactory(this.serverFactory, this.engineFactory, this.hostFactory, this.contextFactory, this.connectorFactory);
        
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
        this.verifyCatalinaFactoryTypes(new ServiceLoaderCatalinaFactory());
    }
    
    protected void verifyCatalinaFactoryTypes(CatalinaFactoryRegistry registry) {
        assertSame(registry.getServerFactory().getClass(), CatalinaServerFactory.class);
        assertSame(registry.getEngineFactory().getClass(), CatalinaEngineFactory.class);
        assertSame(registry.getHostFactory().getClass(), CatalinaHostFactory.class);
        assertSame(registry.getContextFactory().getClass(), CatalinaContextFactory.class);
        assertSame(registry.getConnectorFactory().getClass(), CatalinaConnectorFactory.class);
    }
}
