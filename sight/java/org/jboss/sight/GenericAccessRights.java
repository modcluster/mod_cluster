/*
 *  SIGHT - System information gathering hybrid tool
 *
 *  Copyright(c) 2007 Red Hat Middleware, LLC,
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

package org.jboss.sight;

import java.util.EnumSet;

/**
 * Generic Access Rights.
 * Securable objects use an access mask format in which the four high-order
 * bits specify generic access rights. Each type of securable object maps
 * these bits to a set of its standard and object-specific access rights.
 */
public enum GenericAccessRights
{

    /**
     * Access is not specified.
     */
    NONE(   0x00000000),

    /**
     * Read Access.
     */
    READ(   0x80000000),

    /**
     * Write Access.
     */
    WRITE(  0x40000000),

    /**
     * Execute Access.
     */
    EXECUTE(0x20000000),

    /**
     * Read, write, and execute access
     */
    ALL(    0x10000000);

    private int value;
    private GenericAccessRights(int v)
    {
        value = v;
    }

    public int valueOf()
    {
        return value;
    }

    public static int bitmapOf(EnumSet<GenericAccessRights> set)
    {
        int bitmap = 0;
        if (set != null) {
            for (GenericAccessRights a : set)
                bitmap += a.valueOf();
        }
        return bitmap;
    }

    public static EnumSet<GenericAccessRights> valueOf(int value)
    {
        EnumSet<GenericAccessRights> set = EnumSet.noneOf(GenericAccessRights.class);
        for (GenericAccessRights e : values()) {
            if ((e.value & value) == e.value)
                set.add(e);
        }
        return set;
    }
}
