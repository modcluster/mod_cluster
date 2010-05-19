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
package org.jboss.modcluster;

import org.jboss.logging.Logger;

/**
 * Old location of catalina-based lifecycle listener.
 * To be removed in version 1.2/2.0.
 * @author Paul Ferraro
 */
@Deprecated
public class ModClusterListener extends org.jboss.modcluster.catalina.ModClusterListener
{
   private final Logger logger = Logger.getLogger(ModClusterListener.class);
   
   public ModClusterListener()
   {
      super();
      
      this.logger.warn(Strings.DEPRECATED.getString(ModClusterListener.class.getName(), org.jboss.modcluster.catalina.ModClusterListener.class.getName()));
   }
}
