/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.modcluster;

import java.util.Collections;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Server;
import org.apache.catalina.util.StringManager;
import org.apache.tomcat.util.IntrospectionUtils;
import org.apache.tomcat.util.modeler.Registry;
import org.jboss.logging.Logger;
import org.jboss.modcluster.load.LoadBalanceFactorProvider;
import org.jboss.modcluster.load.impl.DynamicLoadBalanceFactorProvider;
import org.jboss.modcluster.load.metric.LoadContext;
import org.jboss.modcluster.load.metric.LoadMetric;
import org.jboss.modcluster.load.metric.impl.BusyConnectorsLoadMetric;
import org.jboss.modcluster.mcmp.MCMPHandler;
import org.jboss.modcluster.mcmp.MCMPRequestFactory;

/**
 * Non-clustered mod_cluster lifecycle listener for use in JBoss Web standalone.
 * @author Paul Ferraro
 */
public class ModClusterListener extends AbstractModClusterService
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

   private final StringManager sm = StringManager.getManager(Constants.Package);

   @SuppressWarnings("rawtypes")
   private Class<? extends LoadMetric> loadMetricClass = BusyConnectorsLoadMetric.class;
   private int decayFactor = DynamicLoadBalanceFactorProvider.DEFAULT_DECAY_FACTOR;
   private int history = DynamicLoadBalanceFactorProvider.DEFAULT_HISTORY;
   private double capacity = LoadMetric.DEFAULT_CAPACITY;
   
   public ModClusterListener()
   {
      super();
   }
   
   protected ModClusterListener(MCMPHandler mcmpHandler, MCMPRequestFactory requestFactory, ServerProvider<Server> serverProvider, LifecycleListener lifecycleListener)
   {
      super(mcmpHandler, requestFactory, serverProvider, lifecycleListener);
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.load.LoadBalanceFactorProviderFactory#createLoadBalanceFactorProvider()
    */
   @SuppressWarnings("unchecked")
   public LoadBalanceFactorProvider createLoadBalanceFactorProvider()
   {
      try
      {
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
    * @{inheritDoc}
    * @see org.jboss.modcluster.AbstractModClusterService#lifecycleEvent(org.apache.catalina.LifecycleEvent)
    */
   @Override
   public void lifecycleEvent(LifecycleEvent event)
   {
      super.lifecycleEvent(event);
      
      Lifecycle source = event.getLifecycle();
      
      if (source instanceof Server)
      {
         Server server = (Server) source;
         String type = event.getType();
         
         // Register/unregister ClusterListener mbean on server start/stop
         if (Lifecycle.AFTER_START_EVENT.equals(type))
         {
            try
            {
               ObjectName name = this.getObjectName(server);
               
               Registry.getRegistry(null, null).registerComponent(this, name, null);
            }
            catch (Exception e)
            {
               log.error(this.sm.getString("modcluster.error.jmxRregister"), e);
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
               log.error(this.sm.getString("modcluster.error.jmxUnregister"), e);
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
    * Returns the class name of the configured load metric.
    *  @return the name of a class implementing {@link LoadMetric}
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

   public void setLoadMetricCapacity(String capacity)
   {
      this.capacity = Double.parseDouble(capacity);
   }
}
