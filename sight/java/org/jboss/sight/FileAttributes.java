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
 * File attributes flag flags.
 */
public enum FileAttributes
{
    /** File is read-only */
    READ(       0x01),
    /** File is executable */
    EXCUTABLE(  0x02),
    /** File is hidden */
    HIDDEN(     0x04);

    private int value;
    private FileAttributes(int v)
    {
        value = v;
    }

    public int valueOf()
    {
        return value;
    }

    public static int bitmapOf(EnumSet<FileAttributes> set)
    {
        int bitmap = 0;
        if (set != null) {
            for (FileAttributes a : set)
                bitmap += a.valueOf();
        }
        return bitmap;
    }

    public static EnumSet<FileAttributes> valueOf(int value)
    {
        EnumSet<FileAttributes> set = EnumSet.noneOf(FileAttributes.class);
        for (FileAttributes e : values()) {
            if ((e.value & value) == e.value)
                set.add(e);
        }
        return set;
    }
}
