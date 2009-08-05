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
 * File lock types/flags.
 */
public enum FileLockType
{
    /** Shared lock. More than one process or thread can hold a shared lock
     * at any given time. Essentially, this is a "read lock", preventing
     * writers from establishing an exclusive lock.
     */
    SHARED(   1),

    /** Exclusive lock. Only one process may hold an exclusive lock at any
     * given time. This is analogous to a "write lock".
     */
    EXCLUSIVE(2),

    /** Do not block while acquiring the file lock */
    NONBLOCK( 0x0010);

    private int value;
    private FileLockType(int v)
    {
        value = v;
    }

    public int valueOf()
    {
        return value;
    }

    public static int bitmapOf(EnumSet<FileLockType> set)
    {
        int bitmap = 0;
        if (set != null) {
            for (FileLockType t : set)
                bitmap += t.valueOf();
        }
        return bitmap;
    }

    public static EnumSet<FileLockType> valueOf(int value)
    {
        EnumSet<FileLockType> set = EnumSet.noneOf(FileLockType.class);
        for (FileLockType e : values()) {
            if ((e.value & value) == e.value)
                set.add(e);
        }
        return set;
    }
}
