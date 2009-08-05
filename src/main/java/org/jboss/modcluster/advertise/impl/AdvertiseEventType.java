/*
 *
 *  Copyright(c) 2008 Red Hat Middleware, LLC,
 *  and individual contributors as indicated by the @authors tag.
 *  See the copyright.txt in the distribution for a
 *  full listing of individual contributors.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library in the file COPYING.LIB;
 *  if not, write to the Free Software Foundation, Inc.,
 *  59 Temple Place - Suite 330, Boston, MA 02111-1307, USA
 *
 */

package org.jboss.modcluster.advertise.impl;

/**
 * Set what type of event the AdvertiseEvent signals.
 * @param type The type of event.  One of:
 * <PRE>
 * ON_NEW_SERVER     --  New AdvertisedServer detected
 * ON_STATUS_CHANGE  --  AdvertisedServer server changed status
 * </PRE>
 */
public enum AdvertiseEventType
{
   /** New AdvertisedServer detected */
   ON_NEW_SERVER(0),
   /** AdvertisedServer server changed status */
   ON_STATUS_CHANGE(1);

   private int value;

   private AdvertiseEventType(int v)
   {
      this.value = v;
   }

   public int valueOf()
   {
      return this.value;
   }

   public static AdvertiseEventType valueOf(int value)
   {
      for (AdvertiseEventType e : values())
      {
         if (e.value == value) return e;
      }
      
      throw new IllegalArgumentException("Invalid initializer: " + value);
   }
}
