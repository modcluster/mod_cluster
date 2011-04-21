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
import org.jboss.logging.NullLoggerPlugin;
import org.jboss.modcluster.JvmRouteFactory;
import org.jboss.modcluster.ModClusterService;
import org.jboss.modcluster.ModClusterServiceMBean;
import org.jboss.modcluster.Strings;
import org.jboss.modcluster.config.ModClusterConfig;
import org.jboss.modcluster.load.LoadBalanceFactorProvider;
import org.jboss.modcluster.load.LoadBalanceFactorProviderFactory;
import org.jboss.modcluster.load.impl.DynamicLoadBalanceFactorProvider;
import org.jboss.modcluster.load.metric.LoadContext;
import org.jboss.modcluster.load.metric.LoadMetric;
import org.jboss.modcluster.load.metric.impl.BusyConnectorsLoadMetric;

/**
 * Non-clustered mod_cluster lifecycle listener for use in JBoss Web standalone and Tomcat.
 * @author Paul Ferraro
 */
public class ModClusterListener extends ModClusterConfig
   implements LifecycleListener, LoadBalanceFactorProviderFactory, ModClusterServiceMBean
{
   static
   {
      Logger logger = Logger.getLogger(ModClusterListener.class);
      // We expect to run in a JBoss Web/Tomcat environment where
      // server logging is done via java.util.logging. So, try to 
      // initialize JBoss Logging to use the JDK logging plugin.
      // But only if it isn't already initialized to something else!
      if (logger.getLoggerPlugin().getClass().equals(NullLoggerPlugin.class))
      {
         Logger.setPluginClassName("org.jboss.logging.jdk.JDK14LoggerPlugin");
         
         logger = Logger.getLogger(ModClusterListener.class);
      }
      
      log = logger;
   }
   
   private static final Logger log;

   private final ModClusterServiceMBean service;
   private final LifecycleListener listener;
   
   @SuppressWarnings("rawtypes")
   private Class<? extends LoadMetric> loadMetricClass = BusyConnectorsLoadMetric.class;
   private int decayFactor = DynamicLoadBalanceFactorProvider.DEFAULT_DECAY_FACTOR;
   private int history = DynamicLoadBalanceFactorProvider.DEFAULT_HISTORY;
   private double capacity = LoadMetric.DEFAULT_CAPACITY;

   /**
    * Constructs a new ModClusterListener
    */
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
         LoadMetric<LoadContext> metric = this.loadMetricClass.newInstance();
         
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

   public Class<? extends JvmRouteFactory> getJvmRouteFactoryClass()
   {
      return this.getJvmRouteFactory().getClass();
   }
   
   public void setJvmRouteFactoryClass(Class<? extends JvmRouteFactory> factoryClass) throws InstantiationException, IllegalAccessException
   {
      this.setJvmRouteFactory(factoryClass.newInstance());
   }
   
   /**
    * Returns the class name of the configured load metric.
    * @return the name of a class implementing {@link LoadMetric}
    */
   public String getLoadMetricClass()
   {
      return this.loadMetricClass.getName();
   }

   /**
    * Sets the class of the desired load metric
    * @param loadMetricClass a class implementing {@link LoadMetric}
    * @throws ClassNotFoundException 
    */
   public void setLoadMetricClass(String loadMetricClass) throws ClassNotFoundException
   {
      this.loadMetricClass = Class.forName(loadMetricClass).asSubclass(LoadMetric.class);
   }
   
   /**
    * Returns the factor by which the contribution of historical load values to the load factor calculation should exponentially decay.
    * @return the configured load decay factor
    */
   public int getLoadDecayFactor()
   {
      return this.decayFactor;
   }

   /**
    * Sets the factor by which the contribution of historical load values to the load factor calculation should exponentially decay.
    * @param decayFactor a positive number
    */
   public void setLoadDecayFactor(int decayFactor)
   {
      this.decayFactor = decayFactor;
   }
   
   /**
    * Returns the number of historic load values used when calculating the load factor.
    * @return the configured load history
    */
   public int getLoadHistory()
   {
      return this.history;
   }

   /**
    * Sets the number of historic load values used when calculating the load factor.
    * @param history
    */
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

   /**
    * Sets the capacity (i.e. maximum expected value) of the load values returned by the load metric specified by {@link #getLoadMetricClass()}
    * @param capacity a load capacity
    */
   public void setLoadMetricCapacity(String capacity)
   {
      this.capacity = Double.parseDouble(capacity);
   }

   /**
    * {@inhericDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#addProxy(java.lang.String, int)
    */
   public void addProxy(String host, int port)
   {
      this.service.addProxy(host, port);
   }

   /**
    * {@inhericDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#disable()
    */
   public boolean disable()
   {
      return this.service.disable();
   }

   /**
    * {@inhericDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#disableContext(java.lang.String, java.lang.String)
    */
   public boolean disableContext(String hostName, String contextPath)
   {
      return this.service.disableContext(hostName, contextPath);
   }

   /**
    * {@inhericDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#ping()
    */
   public Map<InetSocketAddress, String> ping()
   {
      return this.service.ping();
   }

   /**
    * {@inhericDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#ping(java.lang.String)
    */
   public Map<InetSocketAddress, String> ping(String jvmRoute)
   {
      return this.service.ping(jvmRoute);
   }

   /**
    * {@inhericDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#ping(java.lang.String, java.lang.String, int)
    */
   public Map<InetSocketAddress, String> ping(String scheme, String host, int port)
   {
      return this.service.ping(scheme, host, port);
   }

   /**
    * {@inhericDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#enable()
    */
   public boolean enable()
   {
      return this.service.enable();
   }

   /**
    * {@inhericDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#enableContext(java.lang.String, java.lang.String)
    */
   public boolean enableContext(String hostName, String contextPath)
   {
      return this.service.enableContext(hostName, contextPath);
   }

   /**
    * {@inhericDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#getProxyConfiguration()
    */
   public Map<InetSocketAddress, String> getProxyConfiguration()
   {
      return this.service.getProxyConfiguration();
   }

   /**
    * {@inhericDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#getProxyInfo()
    */
   public Map<InetSocketAddress, String> getProxyInfo()
   {
      return this.service.getProxyInfo();
   }

   /**
    * {@inhericDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#refresh()
    */
   public void refresh()
   {
      this.service.refresh();
   }

   /**
    * {@inhericDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#removeProxy(java.lang.String, int)
    */
   public void removeProxy(String host, int port)
   {
      this.service.removeProxy(host, port);
   }

   /**
    * {@inhericDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#reset()
    */
   public void reset()
   {
      this.service.reset();
   }

   /**
    * {@inhericDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#stop(long, java.util.concurrent.TimeUnit)
    */
   public boolean stop(long timeout, TimeUnit unit)
   {
      return this.service.stop(timeout, unit);
   }

   /**
    * {@inhericDoc}
    * @see org.jboss.modcluster.ModClusterServiceMBean#stopContext(java.lang.String, java.lang.String, long, java.util.concurrent.TimeUnit)
    */
   public boolean stopContext(String host, String path, long timeout, TimeUnit unit)
   {
      return this.service.stopContext(host, path, timeout, unit);
   }
}
