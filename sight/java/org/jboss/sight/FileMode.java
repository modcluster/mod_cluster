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
 * File open mode flags.
 */
public enum FileMode
{
    /** Open the file for reading */
    READ(               0x00001),
    /** Open the file for writing */
    WRITE(              0x00002),
    /** Create the file if not there */
    CREATE(             0x00004),
    /** Append to the end of the file */
    APPEND(             0x00008),
    /** Open the file and truncate to 0 length */
    TRUNCATE(           0x00010),
    /** Open the file in binary mode */
    BINARY(             0x00020),
    /** Open should fail if APR_CREATE and file exists. */
    EXCL(               0x00040),
    /** Open the file for buffered I/O */
    BUFFERED(           0x00080),
    /** Delete the file after close */
    DELONCLOSE(         0x00100),

    /** Platform dependent tag to open the file for
     * use across multiple threads
     */
    XTHREAD(            0x00200),

    /** Platform dependent support for higher level locked read/write
     * access to support writes across process/machines
     */
    SHARELOCK(          0x00400),

    /** Advisory flag that this file should support
     * apr_socket_sendfile operation
     */
    SENDFILE_ENABLED(   0x01000),

    /** Platform dependent flag to enable large file support;
     * <br /><b>Warning :</b><br />The <code>APR_LARGEFILE</code>
     * flag only has effect
     * on some platforms where <code>sizeof(apr_off_t) == 4</code>.
     * Where implemented, it allows opening
     * and writing to a file which exceeds the size which can be
     * represented by apr_off_t (2 gigabytes).
     * <br />When a file's size does
     * exceed 2Gb, apr_file_info_get() will fail with an error on the
     * descriptor, likewise apr_stat()/apr_lstat() will fail on the
     * filename.  apr_dir_read() will fail with <code>APR_INCOMPLETE</code>
     * on a directory entry for a large file depending on the particular
     * <code>APR_FINFO_*</code> flags.<br />
     * Generally, it is not recommended to use this flag.
     */
    LARGEFILE(          0x04000);

    private int value;
    private FileMode(int v)
    {
        value = v;
    }

    public int valueOf()
    {
        return value;
    }

    public static int bitmapOf(EnumSet<FileMode> set)
    {
        int bitmap = 0;
        if (set != null) {
            for (FileMode m : set)
                bitmap += m.valueOf();
        }
        return bitmap;
    }

    public static EnumSet<FileMode> valueOf(int value)
    {
        EnumSet<FileMode> set = EnumSet.noneOf(FileMode.class);
        for (FileMode e : values()) {
            if ((e.value & value) == e.value)
                set.add(e);
        }
        return set;
    }
}
