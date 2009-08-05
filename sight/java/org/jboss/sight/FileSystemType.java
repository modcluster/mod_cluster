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

/* FileSystem enumeration.
 */
public enum FileSystemType
{
    /** No file type determined */
    UNKNOWN(        0),
     /** MS-DOS FAT */
    MSDOS(          1),
    /** Windows FAT32 or VFAT */
    VFAT(           2),
    /** NT File System */
    NTFS(           3),
    /** CD-ROM File System */
    ISO9660(        4),
    /** EXT2 File System */
    EXT2(           5),
    /** EXT3 File System */
    EXT3(           6),
    /** Extended File System (IRIX) */
    XFS(            7),
    /** Replacement for old Minix filesystem in Linux */
    XIAFS(          8),
    /** High Performance File System */
    HPFS(           9),
    /** Macintosh Hierarchical Filesystem */
    HFS(           10),
    /** Journaled filesystem (HP-UX, AIX, OS/2 5, Linux) */
    JFS(           11),
    /** Rom filesystem */
    ROMFS(         12),
    /** Universal Disk Format (DVD-ROM filesystem) */
    UDF(           13),
    /**
     * Native filesystem for most BSD unixes
     * (FreeBSD, NetBSD, OpenBSD, Sun Solaris, ...).
     */
    FFS(           14),
    /**
     * Secure File System. FSF filesystem type is a variation
     * of the FFS filesystem type.
     */
    SFS(           15),
    /** Network filesystem */
    NFS(           16),
    /** RAM filesystem */
    RAMFS(         17),
    /** Raiser filesystem */
    RAISERFS(      18),
    /** Device filesystem */
    DEV(           19),
    /** Proc filesystem */
    PROC(          20),
    /** Sys filesystem */
    SYSFS(         21),
    /** Temp filesystem */
    TMPFS(         22),
    /** RPC filesystem */
    RPC(           23),
    /** USB filesystem */
    USBFS(         24),
    /** VMware guest filesystem */
    VMHGFS(        25),
    /** VMware VMCI filesystem */
    VMBLOCK(       26),
    /** Swap filesystem */
    SWAP(          27),

    /** File system is mounted to nothing */
    NONE(          99);

    private int value;
    private FileSystemType(int v)
    {
        value = v;
    }

    public int valueOf()
    {
        return value;
    }

    public static FileSystemType valueOf(int value)
    {
        for (FileSystemType e : values()) {
            if (e.value == value)
                return e;
        }
        return UNKNOWN;
    }

}
