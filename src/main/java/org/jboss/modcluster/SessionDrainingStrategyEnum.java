/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

/**
 * @author Paul Ferraro
 */
public enum SessionDrainingStrategyEnum implements SessionDrainingStrategy
{
   /** Drain sessions only if the target context is non-distributable, i.e. its sessions are not replicated */
   DEFAULT(null),
   /** Always drain sessions */
   ALWAYS(Boolean.TRUE),
   /** Never drain sessions */
   NEVER(Boolean.FALSE);
   
   private final Boolean drainSessions;
   
   private SessionDrainingStrategyEnum(Boolean drainSessions)
   {
      this.drainSessions = drainSessions;
   }
   
   /**
    * {@inheritDoc}
    * @see org.jboss.modcluster.SessionDrainingStrategy#isEnabled(org.jboss.modcluster.Context)
    */
   public boolean isEnabled(Context context)
   {
      return (this.drainSessions != null) ? this.drainSessions.booleanValue() : !context.isDistributable();
   }
}
