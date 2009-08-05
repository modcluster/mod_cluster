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
 * File protection flags.
 */
public enum FileProtection
{
    /** Set user id */
    USETID(     0x8000),
    /** Read by user */
    UREAD(      0x0400),
    /** Write by user */
    UWRITE(     0x0200),
    /** Execute by user */
    UEXECUTE(   0x0100),

    /** Set group id */
    GSETID(     0x4000),
    /** Read by group */
    GREAD(      0x0040),
    /** Write by group */
    GWRITE(     0x0020),
    /** Execute by group */
    GEXECUTE(   0x0010),

    /** Sticky bit */
    WSTICKY(    0x2000),
    /** Read by others */
    WREAD(      0x0004),
    /** Write by others */
    WWRITE(     0x0002),
    /** Execute by others */
    WEXECUTE(   0x0001),

    /** Use OS's default permissions */
    OS_DEFAULT( 0x0FFF);

    private int value;
    private FileProtection(int v)
    {
        value = v;
    }

    public int valueOf()
    {
        return value;
    }

    public static int bitmapOf(EnumSet<FileProtection> set)
    {
        int bitmap = 0;
        if (set != null) {
            for (FileProtection p : set)
                bitmap += p.valueOf();
        }
        return bitmap;
    }

    public static EnumSet<FileProtection> valueOf(int value)
    {
        EnumSet<FileProtection> set = EnumSet.noneOf(FileProtection.class);
        for (FileProtection e : values()) {
            if ((e.value & value) == e.value)
                set.add(e);
        }
        return set;
    }
}
