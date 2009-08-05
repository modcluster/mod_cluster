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

package org.jboss.sight.platform.windows;

import org.jboss.sight.Error;
import org.jboss.sight.OS;
import org.jboss.sight.OperatingSystemException;
import org.jboss.sight.UnsupportedOperatingSystemException;

/**
 * Windows VARIANT support.
 * The VARIANT structure is a container for a large union that carries
 * many types of data.
 * @author Mladen Turk
 */

public class VARIANT {

    /* Native object pointer representing VARIANT* */
    protected long HANDLE;

    private static native long      alloc0();
    private static native void      free0(long handle);
    private static native int       type0(long handle);
    private static native String    getvs(long handle);
    private static native boolean   getvz(long handle);
    private static native long      getvj(long handle);
    private static native double    getvd(long handle);

    /**
     * Object finalize callback.
     */
    protected final void finalize()
    {
        clear();
    }

    public VARIANT()
        throws OperatingSystemException, UnsupportedOperatingSystemException
    {
        if (!OS.IS_WINDOWS)
            throw new UnsupportedOperatingSystemException();
        else
            HANDLE = alloc0();
    }

    protected VARIANT(long handle)
    {
        HANDLE = handle;
    }

    public VariantType getType()
    {
        return VariantType.valueOf(type0(HANDLE));
    }

    public void init()
    {
        if (HANDLE == 0)
            HANDLE = alloc0();
    }

    public void clear()
    {
        if (HANDLE != 0)
            free0(HANDLE);
        HANDLE = 0;
    }

    public String toString()
    {
        if (HANDLE != 0) {
            VariantType t = getType();
            if (t == VariantType.VT_BSTR) {
                return getvs(HANDLE);
            }
            else if (t == VariantType.VT_BOOL) {
                if (getvz(HANDLE))
                    return "true";
                else
                    return "false";
            }
        }
        return null;
    }
}
