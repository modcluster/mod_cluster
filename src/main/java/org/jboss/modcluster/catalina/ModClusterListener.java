/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.modcluster.catalina;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Server;
import org.apache.tomcat.util.IntrospectionUtils;
import org.apache.tomcat.util.modeler.Registry;
import org.jboss.logging.Logger;
import org.jboss.modcluster.ModClusterService;
import org.jboss.modcluster.ModClusterServiceMBean;
import org.jboss.modcluster.Strings;
import org.jboss.modcluster.config.LoadConfiguration;
import org.jboss.modcluster.config.ModClusterConfig;
import org.jboss.modcluster.load.LoadBalanceFactorProvider;
import org.jboss.modcluster.load.LoadBalanceFactorProviderFactory;
import org.jboss.modcluster.load.impl.DynamicLoadBalanceFactorProvider;
import org.jboss.modcluster.load.metric.LoadContext;
import org.jboss.modcluster.load.metric.LoadMetric;
import org.jboss.modcluster.load.metric.impl.BusyConnectorsLoadMetric;

/**
 * Non-clustered mod_cluster lifecycle listener for use in JBoss Web standalone.
 * @author Paul Ferraro
 */
public class ModClusterListener extends ModClusterConfig
   implements LifecycleListener, LoadConfiguration, LoadBalanceFactorProviderFactory, ModClusterServiceMBean
{
   static
   {
      // We expect to run in a JBoss Web/Tomcat environment where
      // server logging is done via java.util.logging. So, try to 
      // initialize JBoss Logging to use the JDK logging plugin.
      // But only if it isn't already initialized to something else!
      if (Logger.getPluginClassName() == null)
      {
         String pluginClass = "org.jboss.logging.jdk.JDK14LoggerPlugin";
         
         try
         {
            ModClusterListener.class.getClassLoader().loadClass(pluginClass);
            // We can load it, so let's use it
            Logger.setPluginClassName(pluginClass);
         }
         catch (Throwable t)
         {
            // Cannot load JDK14LoggerPlugin; just fall through to defaults
         }
      }
   }
   
   private static final Logger log = Logger.getLogger(ModClusterListener.class);

   private final ModClusterServiceMBean service;
   private final LifecycleListener listener;
   
   private Class<? extends LoadMetric<? extends LoadContext>> loadMetricClass = BusyConnectorsLoadMetric.class;
   private int decayFactor = DynamicLoadBalanceFactorProvider.DEFAULT_DECAY_FACTOR;
   private int history = DynamicLoadBalanceFactorProvider.DEFAULT_HISTORY;
   private double capacity = LoadMetric.DEFAULT_CAPACITY;

   public ModClusterListener()
   {
      ModClusterService service = new ModClusterService(this, this);
      
      this.service = service;
      this.listener = new CatalinaEventHandlerAdapter(service);
   }
   
   protected ModClusterListener(ModClusterServiceMBean mbean, LifecycleListener listener)
   {
      this.service = mbean;
      this.listener = listener;
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.load.LoadBalanceFactorProviderFactory#createLoadBalanceFactorProvider()
    */
   public LoadBalanceFactorProvider createLoadBalanceFactorProvider()
   {
      try
      {
         @SuppressWarnings("unchecked")
         LoadMetric<LoadContext> metric = (LoadMetric<LoadContext>) this.loadMetricClass.newInstance();
         
         metric.setCapacity(this.capacity);
         
         DynamicLoadBalanceFactorProvider provider = new DynamicLoadBalanceFactorProvider(Collections.singleton(metric));
         
         provider.setDecayFactor(this.decayFactor);
         provider.setHistory(this.history);
         
         return provider;
      }
      catch (IllegalAccessException e)
      {
         throw new IllegalArgumentException(e);
      }
      catch (InstantiationException e)
      {
         throw new IllegalArgumentException(e);
      }
   }

   /**
    * {@inheritDoc}
    * @see org.apache.catalina.LifecycleListener#lifecycleEvent(org.apache.catalina.LifecycleEvent)
    */
   @Override
   public void lifecycleEvent(LifecycleEvent event)
   {
      this.listener.lifecycleEvent(event);
      
      Lifecycle source = event.getLifecycle();
      
      if (source instanceof Server)
      {
         Server server = (Server) source;
         String type = event.getType();
         
         // Register/unregister ModClusterListener mbean on server start/stop
         if (Lifecycle.AFTER_START_EVENT.equals(type))
         {
            try
            {
               ObjectName name = this.getObjectName(server);
               
               Registry.getRegistry(null, null).registerComponent(this, name, null);
            }
            catch (Exception e)
            {
               log.error(Strings.ERROR_JMX_REGISTER.getString(), e);
            }
         }
         else if (Lifecycle.STOP_EVENT.equals(type))
         {
            try
            {
               ObjectName name = this.getObjectName(server);
               
               Registry.getRegistry(null, null).unregisterComponent(name);
            }
            catch (Exception e)
            {
               log.error(Strings.ERROR_JMX_UNREGISTER.getString(), e);
            }
         }
      }
   }

   private ObjectName getObjectName(Server server) throws MalformedObjectNameException
   {
      String domain = (String) IntrospectionUtils.getProperty(server, "domain");
      return ObjectName.getInstance(domain , "type", "ModClusterListener");
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.config.LoadConfiguration#getLoadMetricClass()
    */
   public Class<? extends LoadMetric<? extends LoadContext>> getLoadMetricClass()
   {
      return this.loadMetricClass;
   }

   public void setLoadMetricClass(Class<? extends LoadMetric<? extends LoadContext>> loadMetricClass)
   {
      this.loadMetricClass = loadMetricClass;
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.config.LoadBalanceFactorProviderConfiguration#getDecayFactor()
    */
   public int getLoadDecayFactor()
   {
      return this.decayFactor;
   }

   public void setLoadDecayFactor(int decayFactor)
   {
      this.decayFactor = decayFactor;
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.config.LoadBalanceFactorProviderConfiguration#getHistory()
    */
   public int getLoadHistory()
   {
      return this.history;
   }

   public void setLoadHistory(int history)
   {
      this.history = history;
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.config.LoadMetricConfiguration#getLoadMetricCapacity()
    */
   public double getLoadMetricCapacity()
   {
      return this.capacity;
   }

   public void setLoadMetricCapacity(double capacity)
   {
      this.capacity = capacity;
   }

   @Override
   public void addProxy(String host, int port)
   {
      this.service.addProxy(host, port);
   }

   @Override
   public boolean disable()
   {
      return this.service.disable();
   }

   @Override
   public boolean disable(String hostName, String contextPath)
   {
      return this.service.disable(hostName, contextPath);
   }

   @Override
   public Map<InetSocketAddress, String> ping(String jvmRoute)
   {
      return this.service.ping(jvmRoute);
   }

   @Override
   public boolean enable()
   {
      return this.service.enable();
   }

   @Override
   public boolean enable(String hostName, String contextPath)
   {
      return this.service.enable(hostName, contextPath);
   }

   @Override
   public Map<InetSocketAddress, String> getProxyConfiguration()
   {
      return this.service.getProxyConfiguration();
   }

   @Override
   public Map<InetSocketAddress, String> getProxyInfo()
   {
      return this.service.getProxyInfo();
   }

   @Override
   public void refresh()
   {
      this.service.refresh();
   }

   @Override
   public void removeProxy(String host, int port)
   {
      this.service.removeProxy(host, port);
   }

   @Override
   public void reset()
   {
      this.service.reset();
   }

   @Override
   public boolean stop(long timeout, TimeUnit unit)
   {
      return this.service.stop(timeout, unit);
   }

   @Override
   public boolean stop(String host, String path, long timeout, TimeUnit unit)
   {
      return this.service.stop(host, path, timeout, unit);
   }
}
