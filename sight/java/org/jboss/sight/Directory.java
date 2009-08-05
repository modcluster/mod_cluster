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
 * Directory
 *
 * @author Mladen Turk
 *
 */

public class Directory extends NativeObject
{

    private static native int   open0(long instance, String name)
                                    throws NullPointerException, OperatingSystemException;
    private static native int   create0(String name, int perm, boolean recursive)
                                    throws NullPointerException, OperatingSystemException;
    private static native int   attach0(long instance, long dh)
                                    throws NullPointerException;
    private static native int   rewind0(long instance);
    private static native int   remove0(String name)
                                    throws NullPointerException, OperatingSystemException;
    private static native FileInfo read0(long instance, int wanted);

    private static native long   size0(long instance, boolean recursive, String path);

    protected void onDestroy()
    {
        // Nothing
    }

    private Directory()
    {
        super(0);
    }

    private Directory(Pool parent)
    {
        super(parent.POOL);
    }

    protected Directory(long pool, long dh)
    {
        super(pool);
        attach0(INSTANCE, dh);
    }

    private static String mkpath(String path)
    {
        String p = path.replace('\\', '/');
        if (p.charAt(p.length() - 1) == '/')
            return p.substring(0, p.length() - 1);
        else
            return p;
    }

    public String Path;

    /**
     * Open the specified directory.
     * @param path The full path to the directory (use / on all systems)
     */
    public Directory(String path)
        throws NullPointerException, OperatingSystemException
    {
        super(0);
        if (path == null || path.length() == 0)
            throw new NullPointerException();
        Path = mkpath(path);
        open0(INSTANCE, Path);
    }

    /**
     * Open the specified directory.
     * @param path The full path to the directory (use / on all systems)
     * @param parent The pool to use.
     */
    public  Directory(String path, Pool parent)
        throws NullPointerException, OperatingSystemException
    {
        super(parent.POOL);
        if (path == null || path.length() == 0)
            throw new NullPointerException();
        Path = mkpath(path);
        open0(INSTANCE, Path);
    }

    /**
     * Rewind the directory to the first entry.
     */
    public int rewind()
    {
        return rewind0(INSTANCE);
    }

    /**
     * Creates a new directory on the file system
     * @param path The path for the directory to be created.
                   (use / on all systems)
     */
    public static int make(String path,FileProtection perm)
        throws NullPointerException, OperatingSystemException
    {
        if (path == null)
            throw new NullPointerException();
        return create0(path, perm.valueOf(), false);
    }

    /**
     * Creates a new directory on the file system
     * @param path The path for the directory to be created.
                   (use / on all systems)
     */
    public static int make(String path, EnumSet<FileProtection> perm)
        throws NullPointerException, OperatingSystemException
    {
        if (path == null)
            throw new NullPointerException();
        return create0(path, FileProtection.bitmapOf(perm), false);
    }

    /**
     * Creates a new directory on the file system, but behaves like
     * <code>mkdir -p</code>. Creates intermediate directories as
     * @param path The path for the directory to be created.
                   (use / on all systems)
     */
    public static int makeRecursive(String path, FileProtection perm)
        throws NullPointerException, OperatingSystemException
    {
        if (path == null)
            throw new NullPointerException();
        return create0(path, perm.valueOf(), true);
    }

    /**
     * Creates a new directory on the file system, but behaves like
     * <code>mkdir -p</code>. Creates intermediate directories as
     * @param path The path for the directory to be created.
                   (use / on all systems)
     */
    public static int makeRecursive(String path, EnumSet<FileProtection> perm)
        throws NullPointerException, OperatingSystemException
    {
        if (path == null)
            throw new NullPointerException();
        return create0(path, FileProtection.bitmapOf(perm), true);
    }

    /**
     * Remove directory from the file system.
     * @param path the path for the directory to be removed.
     * (use / on all systems)
     */
    public static int remove(String path)
        throws NullPointerException, OperatingSystemException
    {
        if (path == null)
            throw new NullPointerException();
        return remove0(path);
    }

    private int cachedWanted = 0;

    /**
     * Read the next entry from the specified directory.
     * @param wanted The desired FileInfo fields, as a bit flag of
     *               FileInfoFlags values
     */
    protected FileInfo read(EnumSet<FileInfoFlags> wanted)
    {
        if (wanted != null) {
            cachedWanted = FileInfoFlags.bitmapOf(wanted);
        }
        return read0(INSTANCE, cachedWanted);
    }

    /**
     * Get the Directory iterator.
     * @param wanted The desired FileInfo fields, as a bit flag of
     *               FileInfoFlags values
     */
    public DirectoryIterator getContent(EnumSet<FileInfoFlags> wanted)
    {
        return new DirectoryIterator(this, wanted);
    }

    /**
     * Calculate the size consumed all files in directory
     * @param recursive Calculate the size of all subdirectories.
     * @return Directory size in bytes
     */
    public long calculateSize(boolean recursive)
    {
        return size0(INSTANCE, recursive, Path);
    }

    /**
     * Close the directory.
     * This is synonim for destroy method
     */
    public void close()
    {
        destroy();
    }

}
