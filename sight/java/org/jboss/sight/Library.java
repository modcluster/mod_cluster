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

/** Library
 *
 * @author Mladen Turk
 *
 */

public final class Library {

    /* Default library names */
    private static String [] NAMES = {"sight-1", "libsight-1"};

    /*
     * A handle to the unique Library singleton instance.
     */
    static private Library _instance = null;

    private Library()
        throws Exception
    {
        boolean loaded = false;
        String err = "";
        for (int i = 0; i < NAMES.length; i++) {
            try {
                System.loadLibrary(NAMES[i]);
                loaded = true;
            }
            catch (Throwable e) {
                if ( i > 0)
                    err += ", ";
                err +=  e.getMessage();
            }
            if (loaded)
                break;
        }
        if (!loaded) {
            err += "(";
            err += System.getProperty("java.library.path");
            err += ")";
            throw new UnsatisfiedLinkError(err);
        }
    }

    private Library(String rootPath)
        throws Exception
    {
        LibraryLoader.load(rootPath);
    }

    /* create global SIGHT APR pool
     * This has to be the first call to SIGHT library.
     */
    private static native boolean initialize0();

    /* destroy global SIGHT APR pool
     * This has to be the last call to SIGHT library.
     */
    private static native void terminate0();

    /* Internal function for loading versions */
    private static native int version(int what);

    /* SIGHT_MAJOR_VERSION */
    public static int SIGHT_MAJOR_VERSION  = 0;
    /* SIGHT_MINOR_VERSION */
    public static int SIGHT_MINOR_VERSION  = 0;
    /* SIGHT_PATCH_VERSION */
    public static int SIGHT_PATCH_VERSION  = 0;
    /* SIGHT_IS_DEV_VERSION */
    public static int SIGHT_IS_DEV_VERSION = 0;
    /* APR_MAJOR_VERSION */
    public static int APR_MAJOR_VERSION    = 0;
    /* APR_MINOR_VERSION */
    public static int APR_MINOR_VERSION    = 0;
    /* APR_PATCH_VERSION */
    public static int APR_PATCH_VERSION    = 0;
    /* APR_IS_DEV_VERSION */
    public static int APR_IS_DEV_VERSION   = 0;

    /** Return SIGHT_VERSION_STRING */
    public static native String getVersionString();
    /** Return APR_VERSION_STRING */
    public static native String getAprVersionString();

    /**
     * Setup any APR internal data structures.  This MUST be the first function
     * called for any APR library.
     * @param libraryName the name of the library to load
     */
    static public boolean initialize(String libraryName)
        throws Exception, UnsatisfiedLinkError
    {
        synchronized(Library.class) {
            if (_instance == null) {
                if (libraryName == null)
                    _instance = new Library();
                else
                    _instance = new Library(libraryName);
                SIGHT_MAJOR_VERSION  = version(0x01);
                SIGHT_MINOR_VERSION  = version(0x02);
                SIGHT_PATCH_VERSION  = version(0x03);
                SIGHT_IS_DEV_VERSION = version(0x04);
                APR_MAJOR_VERSION    = version(0x11);
                APR_MINOR_VERSION    = version(0x12);
                APR_PATCH_VERSION    = version(0x13);
                APR_IS_DEV_VERSION   = version(0x14);

                if (APR_MAJOR_VERSION < 1) {
                    throw new UnsatisfiedLinkError("Unsupported APR Version (" +
                                                   getAprVersionString() + ")");
                }
                if (initialize0()) {
                    /* Initialize Runtime objects */
                    File.initializeStdFiles();
                    Service.loadResources();
                    return true;
                }
                return false;
            }
            else
                return true;
        }
    }

    /**
     * Clean up APR internal data structures.  This MUST be the last function
     * called for any APR library.
     */
    static public void shutdown()
        throws Exception, UnsatisfiedLinkError
    {
        synchronized(Library.class) {
            try {
                if (_instance != null) {
                    terminate0();
                }
                else {
                    throw new UnsatisfiedLinkError("Library was not initialized");
                }
            }
            finally {
                _instance = null;
            }
        }
    }

    /**
     * Clean up APR internal data structures.  This MUST be the last function
     * called for any APR library.
     */
    static public void terminate()
        throws Exception, UnsatisfiedLinkError
    {
        shutdown();
    }

    static public native void clear0();

    /**
     * Clear the global APR pool and
     * close all the native objects.
     */
    static public void clear()
        throws Exception, UnsatisfiedLinkError
    {
        synchronized(Library.class) {
            if (_instance != null) {
                clear0();
                /* Reinitialize Runtime objects */
                File.initializeStdFiles();
            }
            else {
                throw new UnsatisfiedLinkError("Library was not initialized");
            }
        }
    }

}
