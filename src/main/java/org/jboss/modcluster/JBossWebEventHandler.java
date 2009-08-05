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

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Server;

/**
 * Handles events notifications coming from JBoss Web via a cluster listener.
 * 
 * @author Brian Stansberry
 */
public interface JBossWebEventHandler
{
   void init();
   void shutdown();
   
   void addContext(Context context);

   void startContext(Context context);

   void stopContext(Context context);

   void removeContext(Context context);
   
   void status(Engine engine);

   void startServer(Server server);
   
   void stopServer(Server server);
}