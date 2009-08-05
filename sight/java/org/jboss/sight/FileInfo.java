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
 * File Information.
 * <br />
 * @author Mladen Turk
 *
 */
public final class FileInfo
{

    private static native int   stat0(Object thiz, String name, int wanted)
                                    throws NullPointerException, OperatingSystemException;

    private FileInfo()
    {
        // Nothing
    }
    private void setType(int ftype)
    {
        Type = FileType.valueOf(ftype);
    }

    private void setValid(int valid)
    {
        Valid = FileInfoFlags.valueOf(valid);
    }


    /** The access permissions of the file.  Mimics Unix access rights. */
    public int      Protection;
    /**
     * The type of file.
     * One of <code>APR_REG, APR_DIR, APR_CHR, APR_BLK, APR_PIPE,
     * APR_LNK or APR_SOCK</code>. If the type is undetermined, the value is
     * <code>APR_NOFILE</code>. If the type cannot be determined,
     * the value is <code>APR_UNKFILE</code>.
     */
    public FileType Type;
    /**
     * The user id that owns the file
     */
    public long     UserId;
    /**
     * The group id that owns the file
     */
    public long     GroupId;
    /**
     * The number of hard links to the file.
     */
    public int      NumLinks;
    /**
     * The size of the file
     */
    public long     Size;
    /**
     * The storage size consumed by the file
     */
    public long     StorageSize;
    /**
     * The time the file was last accessed
     */
    public long     LastAccessTime;
    /**
     * The time the file was last modified
     */
    public long     ModifiedTime;
    /**
     * The time the file was created, or the inode was last changed
     */
    public long     CreatedTime;
    /**
     * The pathname of the file (possibly unrooted)
     */
    public String   Name;
    /**
     * The file's name (no path) in filesystem case
     */
    public String   BaseName;
     /**
      * The inode of the file.
      */
    public long     InodeId;
    /**
     * The id of the device the file is on.
     */
    public long     DeviceId;

    /**
     * The bitmask describing valid fields of this FileInfo structure
     *  including all available 'wanted' fields and potentially more
     */
    public EnumSet<FileInfoFlags> Valid;

    public FileInfo(String fileName, EnumSet<FileInfoFlags> wanted)
        throws NullPointerException, OperatingSystemException
    {
        stat0(this, fileName, FileInfoFlags.bitmapOf(wanted));
    }

}
