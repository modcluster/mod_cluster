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

import java.io.File;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import javax.naming.Context;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.deploy.NamingResourcesImpl;
import org.apache.catalina.startup.Catalina;

/**
 * A {@link Server} delegate which filters found services to return only one configured {@link Service}.
 *
 * @author Radoslav Husar
 */
public class SingleServiceServer implements Server {

    private final Service service;

    public SingleServiceServer(Service service) {
        super();

        this.service = service;
    }

    // Filtering methods

    @Override
    public Service findService(String name) {
        return this.service.getName().equals(name) ? this.service : null;
    }

    @Override
    public Service[] findServices() {
        return new Service[] { this.service };
    }

    // Delegating methods

    @Override
    public NamingResourcesImpl getGlobalNamingResources() {
        return this.service.getServer().getGlobalNamingResources();
    }

    @Override
    public void setGlobalNamingResources(NamingResourcesImpl globalNamingResources) {
        throw new IllegalStateException();
    }

    @Override
    public Context getGlobalNamingContext() {
        return this.service.getServer().getGlobalNamingContext();
    }

    @Override
    public int getPort() {
        return this.service.getServer().getPort();
    }

    @Override
    public void setPort(int port) {
        throw new IllegalStateException();
    }

    @Override
    public int getPortOffset() {
        return this.service.getServer().getPortOffset();
    }

    @Override
    public void setPortOffset(int portOffset) {
        throw new IllegalStateException();
    }

    @Override
    public int getPortWithOffset() {
        return this.service.getServer().getPortWithOffset();
    }

    @Override
    public String getAddress() {
        return this.service.getServer().getAddress();
    }

    @Override
    public void setAddress(String address) {
        throw new IllegalStateException();
    }

    @Override
    public String getShutdown() {
        return this.service.getServer().getShutdown();
    }

    @Override
    public void setShutdown(String shutdown) {
        throw new IllegalStateException();
    }

    @Override
    public ClassLoader getParentClassLoader() {
        return this.service.getServer().getParentClassLoader();
    }

    @Override
    public void setParentClassLoader(ClassLoader parent) {
        throw new IllegalStateException();
    }

    @Override
    public Catalina getCatalina() {
        return this.service.getServer().getCatalina();
    }

    @Override
    public void setCatalina(Catalina catalina) {
        throw new IllegalStateException();
    }

    @Override
    public File getCatalinaBase() {
        return this.service.getServer().getCatalinaBase();
    }

    @Override
    public void setCatalinaBase(File catalinaBase) {
        throw new IllegalStateException();
    }

    @Override
    public File getCatalinaHome() {
        return this.service.getServer().getCatalinaHome();
    }

    @Override
    public void setCatalinaHome(File catalinaHome) {
        throw new IllegalStateException();
    }

    @Override
    public int getUtilityThreads() {
        return this.service.getServer().getUtilityThreads();
    }

    @Override
    public void setUtilityThreads(int utilityThreads) {
        throw new IllegalStateException();
    }

    @Override
    public void addService(Service service) {
        throw new IllegalStateException();
    }

    @Override
    public void await() {
        throw new IllegalStateException();
    }


    @Override
    public void removeService(Service service) {
        throw new IllegalStateException();
    }

    @Override
    public Object getNamingToken() {
        return this.service.getServer().getNamingToken();
    }

    @Override
    public ScheduledExecutorService getUtilityExecutor() {
        return this.service.getServer().getUtilityExecutor();
    }

    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        throw new IllegalStateException();
    }

    @Override
    public LifecycleListener[] findLifecycleListeners() {
        return this.service.findLifecycleListeners();
    }

    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        throw new IllegalStateException();
    }

    @Override
    public void init() throws LifecycleException {
        throw new IllegalStateException();
    }

    @Override
    public void start() throws LifecycleException {
        throw new IllegalStateException();
    }

    @Override
    public void stop() throws LifecycleException {
        throw new IllegalStateException();
    }

    @Override
    public void destroy() throws LifecycleException {
        throw new IllegalStateException();
    }

    @Override
    public LifecycleState getState() {
        return this.service.getServer().getState();
    }

    @Override
    public String getStateName() {
        return this.service.getServer().getStateName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SingleServiceServer that = (SingleServiceServer) o;

        return Objects.equals(service, that.service);
    }

    @Override
    public int hashCode() {
        return service != null ? service.hashCode() : 0;
    }
}
