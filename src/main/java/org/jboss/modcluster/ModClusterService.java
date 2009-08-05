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

import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Server;
import org.jboss.modcluster.load.LoadBalanceFactorProvider;
import org.jboss.modcluster.mcmp.MCMPHandler;
import org.jboss.modcluster.mcmp.MCMPRequestFactory;

/**
 * Non-clustered mod_cluster lifecycle listener for use in JBoss AS.
 * @author Paul Ferraro
 */
public class ModClusterService extends AbstractModClusterService
{
   private final LoadBalanceFactorProvider lbfProvider;
   
   public ModClusterService(LoadBalanceFactorProvider lbfProvider)
   {
      super();
      
      this.lbfProvider = lbfProvider;
   }
   
   protected ModClusterService(MCMPHandler mcmpHandler, MCMPRequestFactory requestFactory, ServerProvider<Server> serverProvider, LifecycleListener lifecycleListener, LoadBalanceFactorProvider lbfProvider)
   {
      super(mcmpHandler, requestFactory, serverProvider, lifecycleListener);
      
      this.lbfProvider = lbfProvider;
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.load.LoadBalanceFactorProviderFactory#createLoadBalanceFactorProvider()
    */
   public LoadBalanceFactorProvider createLoadBalanceFactorProvider()
   {
      return this.lbfProvider;
   }
}
