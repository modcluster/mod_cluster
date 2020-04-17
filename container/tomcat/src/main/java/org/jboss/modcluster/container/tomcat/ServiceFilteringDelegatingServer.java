/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.deploy.NamingResources;
import org.apache.catalina.startup.Catalina;

import javax.naming.Context;
import java.util.Objects;

/**
 * A {@link Server} delegate which filters found services to return only one configured {@link Service}.
 *
 * @author Radoslav Husar
 */
public class ServiceFilteringDelegatingServer implements Server {

    private final Server delegate;
    private final String serviceName;

    public ServiceFilteringDelegatingServer(Service service) {
        super();
        this.delegate = service.getServer();
        this.serviceName = service.getName();
    }

    @Override
    public Service[] findServices() {
        for (Service service : delegate.findServices()) {
            if (service.getName().equals(serviceName)) {
                return new Service[] { service };
            }
        }

        return new Service[] {};
    }

    // Delegating methods

    @Override
    public String getInfo() {
        return delegate.getInfo();
    }

    @Override
    public NamingResources getGlobalNamingResources() {
        return delegate.getGlobalNamingResources();
    }

    @Override
    public void setGlobalNamingResources(NamingResources globalNamingResources) {
        delegate.setGlobalNamingResources(globalNamingResources);
    }

    @Override
    public Context getGlobalNamingContext() {
        return delegate.getGlobalNamingContext();
    }

    @Override
    public int getPort() {
        return delegate.getPort();
    }

    @Override
    public void setPort(int port) {
        delegate.setPort(port);
    }

    @Override
    public String getAddress() {
        return delegate.getAddress();
    }

    @Override
    public void setAddress(String address) {
        delegate.setAddress(address);
    }

    @Override
    public String getShutdown() {
        return delegate.getShutdown();
    }

    @Override
    public void setShutdown(String shutdown) {
        delegate.setShutdown(shutdown);
    }

    @Override
    public ClassLoader getParentClassLoader() {
        return delegate.getParentClassLoader();
    }

    @Override
    public void setParentClassLoader(ClassLoader parent) {
        delegate.setParentClassLoader(parent);
    }

    @Override
    public Catalina getCatalina() {
        return delegate.getCatalina();
    }

    @Override
    public void setCatalina(Catalina catalina) {
        delegate.setCatalina(catalina);
    }

    @Override
    public void addService(Service service) {
        delegate.addService(service);
    }

    @Override
    public void await() {
        delegate.await();
    }

    @Override
    public Service findService(String name) {
        return delegate.findService(name);
    }

    @Override
    public void removeService(Service service) {
        delegate.removeService(service);
    }

    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        delegate.addLifecycleListener(listener);
    }

    @Override
    public LifecycleListener[] findLifecycleListeners() {
        return delegate.findLifecycleListeners();
    }

    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        delegate.removeLifecycleListener(listener);
    }

    @Override
    public void init() throws LifecycleException {
        delegate.init();
    }

    @Override
    public void start() throws LifecycleException {
        delegate.start();
    }

    @Override
    public void stop() throws LifecycleException {
        delegate.stop();
    }

    @Override
    public void destroy() throws LifecycleException {
        delegate.destroy();
    }

    @Override
    public LifecycleState getState() {
        return delegate.getState();
    }

    @Override
    public String getStateName() {
        return delegate.getStateName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceFilteringDelegatingServer that = (ServiceFilteringDelegatingServer) o;

        if (!Objects.equals(delegate, that.delegate)) return false;
        return Objects.equals(serviceName, that.serviceName);
    }

    @Override
    public int hashCode() {
        int result = delegate != null ? delegate.hashCode() : 0;
        result = 31 * result + (serviceName != null ? serviceName.hashCode() : 0);
        return result;
    }
}
