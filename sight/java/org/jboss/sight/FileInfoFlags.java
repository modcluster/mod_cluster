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
 * FileInfo flags.
 * Those are basically APR_FINFO_foo.
 */
public enum FileInfoFlags
{
    NONE(    0x00000000),
    /** Stat the link not the file itself if it is a link */
    LINK(    0x00000001),
    /** Modification Time */
    MTIME(   0x00000010),
    /** Creation or inode-changed time */
    CTIME(   0x00000020),
    /** Access Time */
    ATIME(   0x00000040),
    /** Size of the file */
    SIZE(    0x00000100),
    /** Storage size consumed by the file */
    CSIZE(   0x00000200),
    /** Device */
    DEV(     0x00001000),
    /** Inode */
    INODE(   0x00002000),
    /** Number of links */
    NLINK(   0x00004000),
    /** Type */
    TYPE(    0x00008000),
    /** User */
    USER(    0x00010000),
    /** Group */
    GROUP(   0x00020000),
    /** User protection bits */
    UPROT(   0x00100000),
    /** Group protection bits */
    GPROT(   0x00200000),
    /** World protection bits */
    WPROT(   0x00400000),
    /** if dev is case insensitive */
    ICASE(   0x01000000),
    /** ->name in proper case */
    NAME(    0x02000000),

    /** type, mtime, ctime, atime, size */
    MIN(     0x00008170),
    /** dev and inode */
    IDENT(   0x00003000),
    /** user and group */
    OWNER(   0x00030000),
    /**  all protections */
    PROT(    0x00700000),
    /**  an atomic unix apr_stat() */
    NORM(    0x0073b170),
    /**  an atomic unix apr_dir_read() */
    DIRENT(  0x02000000);

    private int value;
    private FileInfoFlags(int v)
    {
        value = v;
    }

    public int valueOf()
    {
        return value;
    }

    public static int bitmapOf(EnumSet<FileInfoFlags> set)
    {
        int bitmap = 0;
        if (set != null) {
            for (FileInfoFlags f : set)
                bitmap += f.valueOf();
        }
        return bitmap;
    }

    public static EnumSet<FileInfoFlags> valueOf(int value)
    {
        EnumSet<FileInfoFlags> set = EnumSet.noneOf(FileInfoFlags.class);
        for (FileInfoFlags e : values()) {
            if (e == NONE)
                continue;
            if ((e.value & value) == e.value)
                set.add(e);
        }
        return set;
    }
}
