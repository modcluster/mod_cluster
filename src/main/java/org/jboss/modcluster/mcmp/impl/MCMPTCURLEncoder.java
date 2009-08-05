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

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;

import org.apache.catalina.util.StringManager;
import org.apache.tomcat.util.buf.UEncoder;
import org.jboss.modcluster.Constants;
import org.jboss.modcluster.mcmp.MCMPURLEncoder;

/**
 * Allow to use TC and JBoss url converter.
 * 
 * @author Jean-Frederic Clere
 */
public class MCMPTCURLEncoder implements MCMPURLEncoder
{
   private static final Method urlEncodeMethod;

   static
   {
      try
      {
         urlEncodeMethod = UEncoder.class.getMethod("urlEncode", Writer.class, String.class);
      }
      catch (NoSuchMethodException e)
      {
         throw new IllegalStateException(e);
      }
   }

   private static final StringManager sm = StringManager.getManager(Constants.Package);
   
   private final CharArrayWriter writer;
   private final UEncoder encoder;

   public MCMPTCURLEncoder()
   {
      this.encoder = new UEncoder();
      this.writer = new CharArrayWriter();
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

      try
      {
         urlEncodeMethod.invoke(this.encoder, this.writer, key);
      }
      catch (Exception e)
      {
         this.writer.write(key);
      }
      
      this.writer.write('=');

      try
      {
         urlEncodeMethod.invoke(this.encoder, this.writer, value);
      }
      catch (Exception e)
      {
         this.writer.write(value);
      }

      if (hasNext)
      {
         this.writer.write('&');
      }
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.mcmp.MCMPURLEncoder#getLength()
    */
   public int getLength()
   {
      return this.writer.size();
   }

   /**
    * @{inheritDoc}
    * @see org.jboss.modcluster.mcmp.MCMPURLEncoder#getBuffer()
    */
   public char[] getBuffer()
   {
      return this.writer.toCharArray();
   }
}
