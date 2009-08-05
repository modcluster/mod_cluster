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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.EnumSet;

/**
 * File
 *
 * @author Mladen Turk
 *
 */

public class File extends NativeObject
{

    /** A handle to the standard input stream. */
    public static File in  = null;
    /** A handle to the standard output stream. */
    public static File out = null;
    /** A handle to the standard error stream. */
    public static File err = null;

    private static native int   create0(long instance, String name, int flag, int perm)
                                    throws NullPointerException, OperatingSystemException;
    private static native int   attach0(long instance, long fh)
                                    throws NullPointerException;
    private static native int   getstd0(long instance, int which)
                                    throws NullPointerException, OperatingSystemException;
    private static native int   mktemp0(long instance, String template, int flag)
                                    throws NullPointerException, OperatingSystemException;
    private static native String  name0(long instance);
    private static native int   lock0(long instance, int type);
    private static native int   unlock0(long instance);

    private static native int   flush0(long instance)
                                    throws IOException;
    private static native int   putc0(long instance, int ch)
                                    throws IOException;
    private static native int   write0(long instance, byte[] b, int off, int len)
                                    throws IOException;
    private static native int   getc0(long instance)
                                    throws IOException;
    private static native int   read0(long instance, byte[] b, int off, int len)
                                    throws IOException;
    private static native long  seek0(long instance, int where, long offset)
                                    throws IOException;
    private static native int   trunc0(long instance, long offset);
    private static native int   dup0(long target, long instance)
                                    throws NullPointerException, OperatingSystemException;
    private static native int   dup2(long target, long instance)
                                    throws NullPointerException, OperatingSystemException;
    private static native int   attrs0(String name, int attr, int mask)
                                    throws NullPointerException, OperatingSystemException;
    private static native int   perms0(String name, int attr);

    private static native FileInfo finfo0(long instance, int vanted)
                                    throws NullPointerException, OperatingSystemException;


    private FileInputStream     iStream = null;
    private FileOutputStream    oStream = null;

    private File()
    {
        super(0);
    }

    private File(Pool parent)
    {
        super(parent.POOL);
    }

    protected File(long pool, long fh)
    {
        super(pool);
        attach0(INSTANCE, fh);
    }

    /**
     * Open the specified file.
     * @param name The full path to the file (using / on all systems)
     * @param flags Or'ed value of:
     * <PRE>
     * APR_FOPEN_READ              open for reading
     * APR_FOPEN_WRITE             open for writing
     * APR_FOPEN_CREATE            create the file if not there
     * APR_FOPEN_APPEND            file ptr is set to end prior to all writes
     * APR_FOPEN_TRUNCATE          set length to zero if file exists
     * APR_FOPEN_BINARY            not a text file (This flag is ignored on
     *                             UNIX because it has no meaning)
     * APR_FOPEN_BUFFERED          buffer the data.  Default is non-buffered
     * APR_FOPEN_EXCL              return error if APR_FOPEN_CREATE and file exists
     * APR_FOPEN_DELONCLOSE        delete the file after closing.
     * APR_FOPEN_XTHREAD           Platform dependent tag to open the file
     *                             for use across multiple threads
     * APR_FOPEN_SHARELOCK         Platform dependent support for higher
     *                             level locked read/write access to support
     *                             writes across process/machines
     * APR_FOPEN_SENDFILE_ENABLED  Open with appropriate platform semantics
     *                             for sendfile operations.  Advisory only,
     *                             apr_socket_sendfile does not check this flag.
     * </PRE>
     * @param perm Access permissions for file.
     * <P>
     * If perm is OS_DEFAULT and the file is being created,
     * appropriate default permissions will be used.
     * </P>
     */
    public File(String name, EnumSet<FileMode> flags,
                EnumSet<FileProtection> perm)
        throws NullPointerException, OperatingSystemException
    {
        super(0);
        open(name, flags, perm);
    }

    /**
     * Open the specified file.
     * @param name The full path to the file (using / on all systems)
     * @param flags Or'ed value of:
     * <PRE>
     * APR_FOPEN_READ              open for reading
     * APR_FOPEN_WRITE             open for writing
     * APR_FOPEN_CREATE            create the file if not there
     * APR_FOPEN_APPEND            file ptr is set to end prior to all writes
     * APR_FOPEN_TRUNCATE          set length to zero if file exists
     * APR_FOPEN_BINARY            not a text file (This flag is ignored on
     *                             UNIX because it has no meaning)
     * APR_FOPEN_BUFFERED          buffer the data.  Default is non-buffered
     * APR_FOPEN_EXCL              return error if APR_FOPEN_CREATE and file exists
     * APR_FOPEN_DELONCLOSE        delete the file after closing.
     * APR_FOPEN_XTHREAD           Platform dependent tag to open the file
     *                             for use across multiple threads
     * APR_FOPEN_SHARELOCK         Platform dependent support for higher
     *                             level locked read/write access to support
     *                             writes across process/machines
     * APR_FOPEN_SENDFILE_ENABLED  Open with appropriate platform semantics
     *                             for sendfile operations.  Advisory only,
     *                             apr_socket_sendfile does not check this flag.
     * </PRE>
     * @param perm Access permissions for file.
     * <P>
     * If perm is OS_DEFAULT and the file is being created,
     * appropriate default permissions will be used.
     * </P>
     * @param parent The pool to use.
     */
    public File(String name, EnumSet<FileMode> flags,
                EnumSet<FileProtection> perm, Pool parent)
        throws NullPointerException, OperatingSystemException
    {
        super(parent.POOL);
        open(name, flags, perm);
    }

    /**
     * Open the specified file.
     * @param name The full path to the file (using / on all systems)
     * @param flags Or'ed value of:
     * <PRE>
     * APR_FOPEN_READ              open for reading
     * APR_FOPEN_WRITE             open for writing
     * APR_FOPEN_CREATE            create the file if not there
     * APR_FOPEN_APPEND            file ptr is set to end prior to all writes
     * APR_FOPEN_TRUNCATE          set length to zero if file exists
     * APR_FOPEN_BINARY            not a text file (This flag is ignored on
     *                             UNIX because it has no meaning)
     * APR_FOPEN_BUFFERED          buffer the data.  Default is non-buffered
     * APR_FOPEN_EXCL              return error if APR_FOPEN_CREATE and file exists
     * APR_FOPEN_DELONCLOSE        delete the file after closing.
     * APR_FOPEN_XTHREAD           Platform dependent tag to open the file
     *                             for use across multiple threads
     * APR_FOPEN_SHARELOCK         Platform dependent support for higher
     *                             level locked read/write access to support
     *                             writes across process/machines
     * APR_FOPEN_SENDFILE_ENABLED  Open with appropriate platform semantics
     *                             for sendfile operations.  Advisory only,
     *                             apr_socket_sendfile does not check this flag.
     * </PRE>
     * @param perm Access permissions for file.
     * <P>
     * If perm is OS_DEFAULT and the file is being created,
     * appropriate default permissions will be used.
     * </P>
     */
    public void open(String name, EnumSet<FileMode> flags,
                     EnumSet<FileProtection> perm)
        throws NullPointerException, OperatingSystemException
    {
        create0(INSTANCE, name, FileMode.bitmapOf(flags),
                FileProtection.bitmapOf(perm));
    }

    /**
     * Close the File.
     * This is synonim for destroy method
     */
    public void close()
    {
        destroy();
    }

    /**
     * Create a temporary file
     * @param template The template to use when creating a temp file.
     * @param flags The flags to open the file with. If this is zero,
     *              the file is opened with:
     * <code>FileMode.CREATE | FileMode.READ |
     * FileMode.WRITE | FileMode.EXCL |
     * FileMode.DELONCLOSE</code>
     * @return The File to use as a temporary file.
     * <P>
     * This function  generates  a unique temporary file name from template.
     * The last six characters of template must be <code>XXXXXX</code>
     * and these are replaced with a string that makes the filename unique.
     * </P>
     */
    public static File createTemp(String template, EnumSet<FileMode> flags)
        throws NullPointerException, OperatingSystemException
    {
        File tmp = new File();
        mktemp0(tmp.INSTANCE, template, FileMode.bitmapOf(flags));
        return tmp;
    }

    /**
     * Create a temporary file
     * @param template The template to use when creating a temp file.
     * @param flags The flags to open the file with. If this is zero,
     *              the file is opened with:
     * <code>FileMode.CREATE | FileMode.READ |
     * FileMode.WRITE | FileMode.EXCL |
     * FileMode.DELONCLOSE</code>
     * @param parent The pool to allocate the file out of.
     * @return The File to use as a temporary file.
     * <P>
     * This function  generates  a unique temporary file name from template.
     * The last six characters of template must be <code>XXXXXX</code>
     * and these are replaced with a string that makes the filename unique.
     * </P>
     */
    public static File createTemp(String template, EnumSet<FileMode> flags,
                                  Pool parent)
        throws NullPointerException, OperatingSystemException
    {
        File tmp = new File(parent);
        mktemp0(tmp.INSTANCE, template, FileMode.bitmapOf(flags));
        return tmp;
    }

    /**
     * Initialize console stream Files
     */
    protected static void initializeStdFiles()
    {
        try {
            in = new File();
            getstd0(in.INSTANCE, 0);
        }
        catch (Exception e) {
            // Nothing
        }
        try {
            out = new File();
            getstd0(out.INSTANCE, 1);
        }
        catch (Exception e) {
            // Nothing
        }
        try {
            err = new File();
            getstd0(err.INSTANCE, 2);
        }
        catch (Exception e) {
            // Nothing
        }
    }

    protected void onDestroy()
    {
        // Nothing
    }

    /**
     * Get the file name of the current file.
     */
    public String getName()
    {
        return name0(INSTANCE);
    }

    /**
     * Establish a lock on the specified, open file. The lock may be advisory
     * or mandatory, at the discretion of the platform. The lock applies to
     * the file as a whole, rather than a specific range. Locks are established
     * on a per-thread/process basis; a second lock by the same thread will not
     * block.
     * @param type The type of lock to establish on the file. It can be one of:
     * <PRE>
     *  EXCLUSIVE  -- Exclusive lock. Only one process may hold an
     *                exclusive lock at any given time. This is
     *                analogous to a "write lock".
     *  TYPEMASK   -- Mask to extract lock type
     *  NONBLOCK   -- Do not block while acquiring the file lock
     * </PRE>
     * @return {@link Error#APR_SUCCESS Error.APR_SUCCESS} in case the operation was successful.
     */
    public int lock(EnumSet<FileLockType> type)
    {
        return lock0(INSTANCE, FileLockType.bitmapOf(type));
    }

    /**
     * Remove any outstanding locks on the file.
     * @return {@link Error#APR_SUCCESS Error.APR_SUCCESS} in case the operation was successful.
     */
    public int unlock()
    {
        return unlock0(INSTANCE);
    }

    /**
     * Get the specified file's stats.
     * @param wanted The desired apr_finfo_t fields, as a bit flag of
     *               <code>APR_FINFO_*</code> values
     * @return FileInfo containing wanted file information.
     */
    public FileInfo getFileInfo(EnumSet<FileInfoFlags> wanted)
        throws NullPointerException, OperatingSystemException
    {
        return finfo0(INSTANCE, FileInfoFlags.bitmapOf(wanted));
    }

    /**
     * Flush the file's buffer.
     */
    public int flush()
        throws IOException
    {
        return flush0(INSTANCE);
    }

    /**
     * Write a character into the specified file.
     * @param ch The character to write.
     */
    public int write(int ch)
        throws IOException
    {
        return putc0(INSTANCE, ch);
    }

    /**
     * Write data to the specified file.
     *
     * Write will write up to the specified number of
     * bytes, but never more.  If the OS cannot write that many bytes, it
     * will write as many as it can.  The third argument is modified to
     * reflect the * number of bytes written.
     *
     * It is possible for both bytes to be written and an error to
     * be returned. {@link Error#EINTR Error.EINTR} is never returned.
     * @param b The buffer which contains the data.
     * @param off Start offset in buf
     * @param len The number of bytes to write; (-1) for full array.
     * @return The number of bytes written.
     */
    public int write(byte[] b, int off, int len)
        throws IOException
    {
        return write0(INSTANCE, b, off, len);
    }


    /**
     * Read a character from the specified file.
     * @return The readed character or -1 in case of
     *         {@link Error#APR_EOF Error.APR_EOF}.
     */
    public int read()
        throws IOException
    {
        return getc0(INSTANCE);
    }

    /**
     * Read data from the specified file.
     *
     * apr_file_read will read up to the specified number of
     * bytes, but never more.  If there isn't enough data to fill that
     * number of bytes, all of the available data is read.  The third
     * argument is modified to reflect the number of bytes read.  If a
     * char was put back into the stream via ungetc, it will be the first
     * character returned.
     *
     * It is not possible for both bytes to be read and an
     * {@link Error#APR_EOF Error.APR_EOF} or other error to be returned.
     * {@link Error#EINTR Error.EINTR} is never returned.
     * @param b The buffer to store the data to.
     * @param off Start offset in buf
     * @param len The number of bytes to read (-1) for full array.
     * @return The number of bytes read or -1 in case of
     *         {@link Error#APR_EOF Error.APR_EOF}
     */
    public int read(byte[] b, int off, int len)
        throws IOException
    {
        return read0(INSTANCE, b, off, len);
    }

    /**
     * Move the read/write file offset to a specified byte within a file.
     * @param where How to move the pointer, one of:
     * <PRE>
     * SET  --  set the offset to offset
     * CUR  --  add the offset to the current position
     * END  --  add the offset to the current file size
     * </PRE>
     * @param offset The offset to move the pointer to.
     * @return Offset the pointer was actually moved to.
     */
    public long seek(FileSeek where, long offset)
        throws IOException
    {
        return seek0(INSTANCE, where.valueOf(), offset);
    }

    /**
     * Truncate the file's length to the specified offset
     * @param offset The offset to truncate to.
     */
    public int truncate(long offset)
    {
        return trunc0(INSTANCE, offset);
    }

    /**
     * Duplicate the specified file descriptor.
     * @return Duplicated File.
     */
    public File dup()
        throws NullPointerException, OperatingSystemException
    {
        File d = new File();
        dup0(d.INSTANCE, INSTANCE);
        return d;
    }

    /**
     * Duplicate the specified file descriptor and close the original.
     * This file will be closed and reused so it must point
     * at a valid apr_file_t. It cannot be NULL.
     * @param oldFile The file to duplicate.
     * @return Status code.
     */
    public int dup2(File oldFile)
        throws NullPointerException, OperatingSystemException
    {
        return dup2(INSTANCE, oldFile.INSTANCE);
    }

    /**
     * Returns an input stream for this file.
     */
    public InputStream getInputStream()
    {
        if (iStream == null)
            iStream = new FileInputStream(this);
        return iStream;
    }

    /**
     * Returns an output stream for this file.
     */
    public OutputStream getOutputStream()
    {
        if (oStream == null)
            oStream = new FileOutputStream(this);
        return oStream;
    }

    /**
     * Set attributes of the specified file.
     * This function should be used in preference to explict manipulation
     * of the file permissions, because the operations to provide these
     * attributes are platform specific and may involve more than simply
     * setting permission bits.
     * <BR/>
     * <B>Warning:</B> Platforms which do not implement this feature will
     * return APR_ENOTIMPL.
     *
     * @param fileName The full path to the file (using / on all systems)
     * @param attributes Or'd combination of
     * <PRE>
     *  READONLY   - make the file readonly
     *  EXECUTABLE - make the file executable
     *  HIDDEN     - make the file hidden
     * </PRE>
     * @param mask Mask of valid bits in attributes.
     */
    public static int setAttributes(String fileName,
                                    EnumSet<FileAttributes> attributes,
                                    EnumSet<FileAttributes> mask)
        throws NullPointerException, OperatingSystemException
    {
        return attrs0(fileName,
                      FileAttributes.bitmapOf(attributes),
                      FileAttributes.bitmapOf(mask));
    }

    /**
     * Set the specified file's permission bits.
     * <BR/>
     * <B>Warning:</B> Some platforms may not be able to apply all of the
     * available permission bits; APR_INCOMPLETE will be returned if some
     * permissions are specified which could not be set.
     * <BR/>
     * <B>Warning:</B> Platforms which do not implement this feature will
     * return APR_ENOTIMPL.
     * @param fileName The file (name) to apply the permissions to.
     * @param perms The permission bits to apply to the file.
     */
    public static int setPermission(String fileName,
                                    EnumSet<FileProtection> perms)
    {
        return perms0(fileName, FileProtection.bitmapOf(perms));
    }

}
