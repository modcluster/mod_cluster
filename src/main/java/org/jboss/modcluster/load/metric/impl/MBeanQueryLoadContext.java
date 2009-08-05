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
package org.jboss.modcluster.load.metric.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.modcluster.load.metric.LoadContext;

/**
 * @author Paul Ferraro
 *
 */
public class MBeanQueryLoadContext implements LoadContext
{
   private final MBeanServer server;
   private final Set<ObjectName> names;
   
   @SuppressWarnings("unchecked")
   public MBeanQueryLoadContext(MBeanServer server, ObjectName pattern)
   {
      this.server = server;
      this.names = pattern.isPattern() ? server.queryNames(pattern, null) : Collections.singleton(pattern);
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.load.metric.LoadContext#close()
    */
   public void close()
   {
      // nothing to close
   }

   /**
    * Collects the attribute values for each mbean matching the object name pattern
    * @param <T> the type of the attribute
    * @param attribute the mbean attribute name
    * @param targetClass the type of the attribute
    * @return a list of attribute values for each mbean
    * @throws JMException
    */
   public <T> List<T> getAttributes(String attribute, Class<T> targetClass) throws JMException
   {
      List<T> list = new ArrayList<T>(this.names.size());
      
      for (ObjectName name: this.names)
      {
         list.add(targetClass.cast(this.server.getAttribute(name, attribute)));
      }
      
      return list;
   }
}
