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

/**
 * Drive type enumeration.
 * <BR/>
 * Determines whether a disk drive is a removable, fixed, CD-ROM,
 * RAM disk, or network drive.
 */
public enum DriveType
{

    /** The drive type cannot be determined. */
    UNKNOWN(       -1),
    /** The drive has no type. */
    NONE(           0),
    /**
     * The root path is invalid, for example,
     * no volume is mounted at the path.
     */
    INVALID(        1),
    /**
     * The drive is a type that has removable media, for example,
     * a floppy drive or removable hard disk.
     */
    REMOVABLE(      2),
    /**
     * The drive is a type that cannot be removed, for example,
     * a fixed hard drive.
     */
    FIXED(          3),
    /** The drive is a remote (network) drive. */
    REMOTE(         4),
    /** The drive is a CD-ROM drive. */
    CDROM(          5),
    /** The drive is a RAM disk. */
    RAMDISK(        6),
    /** The drive is SWAP. */
    SWAP(           7);


    private int value;
    private DriveType(int v)
    {
        value = v;
    }

    public int valueOf()
    {
        return value;
    }

    public static DriveType valueOf(int value)
    {
        for (DriveType e : values()) {
            if (e.value == value)
                return e;
        }
        return UNKNOWN;
    }

}
