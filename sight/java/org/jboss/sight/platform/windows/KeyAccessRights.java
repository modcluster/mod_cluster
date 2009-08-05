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

package org.jboss.sight.platform.windows;

import java.util.EnumSet;

/**
 * Key Access Rights.
 */
public enum KeyAccessRights
{

    /**
     * Access is not specified.
     */
    NONE(               0x0000),

    ALL_ACCESS(         0x0001),
    CREATE_LINK(        0x0002),
    CREATE_SUB_KEY(     0x0004),
    ENUMERATE_SUB_KEYS( 0x0008),
    EXECUTE(            0x0010),
    NOTIFY(             0x0020),
    QUERY_VALUE(        0x0040),
    READ(               0x0080),
    SET_VALUE(          0x0100),
    WOW64_64KEY(        0x0200),
    WOW64_32KEY(        0x0400),
    WRITE(              0x0800);

    private int value;
    private KeyAccessRights(int v)
    {
        value = v;
    }

    public int valueOf()
    {
        return value;
    }

    public static int bitmapOf(EnumSet<KeyAccessRights> set)
    {
        int bitmap = 0;
        if (set != null) {
            for (KeyAccessRights a : set)
                bitmap += a.valueOf();
        }
        return bitmap;
    }

    public static EnumSet<KeyAccessRights> valueOf(int value)
    {
        EnumSet<KeyAccessRights> set = EnumSet.noneOf(KeyAccessRights.class);
        for (KeyAccessRights e : values()) {
            if ((e.value & value) == e.value)
                set.add(e);
        }
        return set;
    }
}
