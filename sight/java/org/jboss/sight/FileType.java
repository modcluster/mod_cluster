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

/* FileType values for the filetype member of the
 * apr_file_info_t structure
 * <br /><b>Warning :</b>: Not all of the filetypes below can be determined.
 * For example, a given platform might not correctly report
 * a socket descriptor as APR_SOCK if that type isn't
 * well-identified on that platform.  In such cases where
 * a filetype exists but cannot be described by the recognized
 * flags below, the filetype will be APR_UNKFILE.  If the
 * filetype member is not determined, the type will be APR_NOFILE.
 */
public enum FileType
{
    /** No file type determined */
    NOFILE(     0),
     /** A regular file */
    REG(        1),
    /** A directory */
    DIR(        2),
    /** A character device */
    CHR(        3),
    /** A block device */
    BLK(        4),
    /** A FIFO / pipe */
    PIPE(       5),
    /** A symbolic link */
    LNK(        6),
    /** A [unix domain] socket */
    SOCK(       7),
    /** A file of some other unknown type */
    UNKFILE(  127);


    private int value;
    private FileType(int v)
    {
        value = v;
    }

    public int valueOf()
    {
        return value;
    }

    public static FileType valueOf(int value)
    {
        for (FileType e : values()) {
            if (e.value == value)
                return e;
        }
        return UNKFILE;
    }

}
