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
 * File system flags.
 */
public enum FileSystemFlags
{
    NONE(                   0x00000000),

    /** The file system supports case-sensitive file names. */
    CASE_SENSITIVE(         0x00000001),

    /**
     * The file system preserves the case of file names when it places
     * a name on disk.
     */
    CASE_PRESERVED_NAMES(   0x00000002),

    /** The file system supports file-based compression. */
    FILE_COMPRESSION(       0x00000010),

    /** The specified volume is read-only.  */
    READ_ONLY_VOLUME(       0x00080000),

    /** The file system supports encryption */
    SUPPORTS_ENCRYPTION(    0x00020000),

    /** Support for volume mount points (re-parse points) */
    SUPPORTS_MOUNT_POINTS(  0x00000080),

    /** The file system supports sparse files */
    SUPPORTS_SPARSE_FILES(  0x00000040),

    /**
     * The file system supports Unicode in file names as they
     * appear on disk.
     */
    UNICODE(                0x00000004),

    /**
     * The specified volume is a compressed volume,
     * for example, a DoubleSpace volume.
     */
    VOLUME_IS_COMPRESSED(   0x00008000),

    /** The file system supports disk quotas. */
    VOLUME_QUOTAS(          0x00000020),

    /** Read/Write is supported. */
    READ_WRITE_VOLUME(      0x01000000),

    /** Set Uid allowed */
    SUPPORTS_SUID(          0x02000000);

    private int value;
    private FileSystemFlags(int v)
    {
        value = v;
    }

    public int valueOf()
    {
        return value;
    }

    public static int bitmapOf(EnumSet<FileSystemFlags> set)
    {
        int bitmap = 0;
        if (set != null) {
            for (FileSystemFlags f : set)
                bitmap += f.valueOf();
        }
        return bitmap;
    }

    public static EnumSet<FileSystemFlags> valueOf(int value)
    {
        EnumSet<FileSystemFlags> set = EnumSet.noneOf(FileSystemFlags.class);
        for (FileSystemFlags e : values()) {
            if (e == NONE)
                continue;
            if ((e.value & value) == e.value)
                set.add(e);
        }
        return set;
    }
}
