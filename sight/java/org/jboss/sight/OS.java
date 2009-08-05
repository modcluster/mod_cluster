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

/** OS
 *
 * @author Mladen Turk
 *
 */

public final class OS {

    /* OS Enums */
    private static final int UNIX           =  1;
    private static final int WINDOWS        =  2;
    private static final int WIN64          =  3;
    private static final int WOW64          =  4;
    private static final int LINUX          =  5;
    private static final int SOLARIS        =  6;
    private static final int BSD            =  7;

    /**
     * Check for OS type.
     * @param type OS type to test.
     */
    private static native boolean is(int type);

    public static final boolean IS_UNIX    = is(UNIX);
    public static final boolean IS_WINDOWS = is(WINDOWS);
    public static final boolean IS_WIN64   = is(WIN64);
    public static final boolean IS_WOW64   = is(WOW64);
    public static final boolean IS_LINUX   = is(LINUX);
    public static final boolean IS_SOLARIS = is(SOLARIS);
    public static final boolean IS_BSD     = is(BSD);

    /**
     * Name of the operating system implementation.
     */
    public static native String getSysname();

    /**
     * Network name of this machine.
     */
    public static native String getRelease();

    /**
     * Release level of the operating system.
     */
    public static native String getMachine();

    /**
     * Version level of the operating system.
     */
    public static native String getVersion();

    /**
     * Machine hardware platform.
     */
    public static native String getNodename();

}
