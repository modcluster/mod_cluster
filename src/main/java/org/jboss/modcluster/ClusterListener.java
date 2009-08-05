/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.catalina.ContainerEvent;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Engine;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Server;
import org.apache.catalina.ServerFactory;
import org.apache.catalina.Service;
import org.apache.catalina.util.StringManager;
import org.apache.tomcat.util.IntrospectionUtils;
import org.apache.tomcat.util.modeler.Registry;
import org.jboss.logging.Logger;
import org.jboss.modcluster.config.ModClusterConfig;
import org.jboss.modcluster.load.LoadBalanceFactorProvider;
import org.jboss.modcluster.mcmp.MCMPHandler;
import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPUtils;
import org.jboss.modcluster.mcmp.impl.DefaultMCMPHandler;
import org.jboss.modcluster.mcmp.impl.ResetRequestSourceImpl;

/**
 * This listener communicates with a front end mod_cluster enabled proxy to
 * automatically maintain the node configuration according to what is 
 * deployed.
 */
public class ClusterListener extends ModClusterConfig
   implements LifecycleListener, ContainerListener
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
            ClusterListener.class.getClassLoader().loadClass(pluginClass);
            // We can load it, so let's use it
            Logger.setPluginClassName(pluginClass);
         }
         catch (Throwable t)
         {
            // Cannot load JDK14LoggerPlugin; just fall through to defaults
         }
      }
   }
   private static final Logger log = Logger.getLogger(ClusterListener.class);

   /** The string manager for this package. */
   private final StringManager sm = StringManager.getManager(Constants.Package);
   
   // ----------------------------------------------------------------- Fields    

   private final LifecycleListener lifecycleListener;
   private final ContainerListener containerListener;
   private final MCMPHandler mcmpHandler;

   // ----------------------------------------------------------- Constructors

   public ClusterListener()
   {
      this(null);
   }

   public ClusterListener(LoadBalanceFactorProvider loadBalanceFactorProvider)
   {
      this.mcmpHandler = new DefaultMCMPHandler(this, new ResetRequestSourceImpl(this, this));
      
      JBossWebEventHandler eventHandler = new DefaultJBossWebEventHandler(this, this, this, this.mcmpHandler, (loadBalanceFactorProvider != null) ? loadBalanceFactorProvider : this);
      
      BasicClusterListener listener = new BasicClusterListener(eventHandler);

      this.lifecycleListener = listener;
      this.containerListener = listener;
   }
   
   protected ClusterListener(MCMPHandler mcmpHandler, LifecycleListener lifecycleListener, ContainerListener containerListener)
   {
      this.mcmpHandler = mcmpHandler;
      this.lifecycleListener = lifecycleListener;
      this.containerListener = containerListener;
   }
   
   //----------------------------------------------------------------- Public   

   /**
    * Retrieves the full proxy configuration. To be used through JMX or similar.
    * 
    *         response: HTTP/1.1 200 OK
    *   response:
    *   node: [1:1] JVMRoute: node1 Domain: [bla] Host: 127.0.0.1 Port: 8009 Type: ajp
    *   host: 1 [] vhost: 1 node: 1
    *   context: 1 [/] vhost: 1 node: 1 status: 1
    *   context: 2 [/myapp] vhost: 1 node: 1 status: 1
    *   context: 3 [/host-manager] vhost: 1 node: 1 status: 1
    *   context: 4 [/docs] vhost: 1 node: 1 status: 1
    *   context: 5 [/manager] vhost: 1 node: 1 status: 1
    *
    * @return the proxy confguration
    */
   public String getProxyConfiguration()
   {
      return this.mcmpHandler.getProxyConfiguration();
   }
   
   /**
    * Retrieves the full proxy info message.
    *
    *
    * @return the proxy info confguration
    */
   public String getProxyInfo()
   {
      return this.mcmpHandler.getProxyInfo();
   }

   /**
    * Reset a DOWN connection to the proxy up to ERROR, where the configuration will
    * be refreshed. To be used through JMX or similar.
    */
   public void reset()
   {
      this.mcmpHandler.reset();
   }

   /**
    * Refresh configuration. To be used through JMX or similar.
    */
   public void refresh()
   {
      // Set as error, and the periodic event will refresh the configuration
      this.mcmpHandler.markProxiesInError();
   }

   /**
    * Disable all webapps for all engines. To be used through JMX or similar.
    */
   public boolean disable()
   {
      Service[] services = ServerFactory.getServer().findServices();
      for (Service service: services)
      {
         Engine engine = (Engine) service.getContainer();
         // Send DISABLE-APP * request
         MCMPRequest request = MCMPUtils.createDisableEngineRequest(engine);
         this.mcmpHandler.sendRequest(request);
      }
      return this.mcmpHandler.isProxyHealthOK();
   }

   /**
    * Enable all webapps for all engines. To be used through JMX or similar.
    */
   public boolean enable()
   {
      Service[] services = ServerFactory.getServer().findServices();
      for (Service service: services)
      {
         Engine engine = (Engine) service.getContainer();
         // Send ENABLE-APP * request
         MCMPRequest request = MCMPUtils.createEnableEngineRequest(engine);
         this.mcmpHandler.sendRequest(request);
      }
      return this.mcmpHandler.isProxyHealthOK();
   }
   
   /**
    * @{inheritDoc}
    * @see org.apache.catalina.ContainerListener#containerEvent(org.apache.catalina.ContainerEvent)
    */
   public void containerEvent(ContainerEvent event)
   {
      this.containerListener.containerEvent(event);
   }

   /**
    * @{inheritDoc}
    * @see org.apache.catalina.LifecycleListener#lifecycleEvent(org.apache.catalina.LifecycleEvent)
    */
   public void lifecycleEvent(LifecycleEvent event)
   {
      this.lifecycleListener.lifecycleEvent(event);
      
      Lifecycle source = event.getLifecycle();
      
      if (source instanceof Server)
      {
         Server server = (Server) source;
         
         // Register/unregister ClusterListener mbean on server start/stop
         if (Lifecycle.AFTER_START_EVENT.equals(event.getType()))
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
         else if (Lifecycle.STOP_EVENT.equals(event.getType()))
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
      return ObjectName.getInstance(domain , "type", "ClusterListener");
   }
}
