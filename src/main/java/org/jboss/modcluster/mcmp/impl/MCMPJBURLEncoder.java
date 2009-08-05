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

package org.jboss.modcluster.mcmp.impl;

import java.io.IOException;

import org.apache.catalina.util.StringManager;
import org.apache.tomcat.util.buf.CharChunk;
import org.apache.tomcat.util.buf.UEncoder;
import org.jboss.modcluster.Constants;
import org.jboss.modcluster.mcmp.MCMPURLEncoder;

/**
 * Allow to use TC and JBoss url converter.
 * 
 * @author Jean-Frederic Clere
 */
public class MCMPJBURLEncoder implements MCMPURLEncoder
{
   private static StringManager sm = StringManager.getManager(Constants.Package);
   
   private final CharChunk body;
   private final UEncoder encoder;

   public MCMPJBURLEncoder()
   {
      this.encoder = new UEncoder();

      try
      {
         this.body = this.encoder.encodeURL("", 0, 0);
         this.body.recycle();
      }
      catch (IOException e)
      {
         // Cannot happen
         throw new RuntimeException(e);
      }
   }
   
   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.mcmp.MCMPURLEncoder#encodeParameter(org.apache.catalina.util.StringManager, java.lang.String, java.lang.String, boolean)
    */
   public void encodeParameter(String key, String value, boolean hasNext) throws IOException
   {
      if (value == null)
      {
         throw new IllegalArgumentException(sm.getString("modcluster.error.nullAttribute", key));
      }

      this.encoder.encodeURL(key, 0, key.length()).append('=');

      CharChunk body = this.encoder.encodeURL(value, 0, value.length());

      if (hasNext)
      {
         body.append('&');
      }
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.mcmp.MCMPURLEncoder#getLength()
    */
   public int getLength()
   {
      return this.body.getLength();
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.mcmp.MCMPURLEncoder#getBuffer()
    */
   public char[] getBuffer()
   {
      return this.body.getBuffer();
   }
}
