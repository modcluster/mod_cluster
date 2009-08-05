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
package org.jboss.modcluster.test;

import java.text.MessageFormat;
import java.util.Hashtable;

import javax.management.MBeanServerConnection;
import javax.naming.Context;
import javax.naming.InitialContext;

/**
 * @author Paul Ferraro
 *
 */
public class MBeanServerConnector
{
   private final MBeanServerConnection server;
   
   @SuppressWarnings("boxing")
   public MBeanServerConnector(int node) throws Exception
   {
      this(System.getProperty(MessageFormat.format("jbosstest.cluster.node{0}.jndi.url", node), "jnp://localhost:1099"));
   }
   
   public MBeanServerConnector(String providerUrl) throws Exception
   {
       Hashtable<String, String> env = new Hashtable<String, String>();
       env.put(Context.PROVIDER_URL, providerUrl);
       env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
       env.put(Context.URL_PKG_PREFIXES, "org.jnp.interfaces");
       
       this.server = (MBeanServerConnection) new InitialContext(env).lookup("jmx/invoker/RMIAdaptor");
   }
   
   public MBeanServerConnection getServer()
   {
      return this.server;
   }
}
