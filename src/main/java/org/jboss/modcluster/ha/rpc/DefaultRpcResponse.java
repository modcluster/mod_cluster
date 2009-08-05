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
package org.jboss.modcluster.ha.rpc;

import org.jboss.ha.framework.interfaces.ClusterNode;
import org.jboss.modcluster.Utils;

/**
 * @author Paul Ferraro
 *
 */
public class DefaultRpcResponse<T> implements RpcResponse<T>
{
   /** The serialVersionUID */
   private static final long serialVersionUID = -6410563421870835482L;
   
   private final ClusterNode sender;
   private Throwable exception;
   private T result;
   
   public DefaultRpcResponse(ClusterNode sender)
   {
      this.sender = sender;
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.ha.rpc.RpcResponse#getSender()
    */
   public ClusterNode getSender()
   {
      return this.sender;
   }

   public T getResult()
   {
      if (this.exception != null)
      {
         throw Utils.convertToUnchecked(this.exception);
      }
      
      return this.result;
   }
   
   public void setResult(T result)
   {
      this.result = result;  
   }
   
   public void setException(Throwable exception)
   {
      this.exception = exception;
   }
}
