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
 * Windows WMI support
 *
 * @author Mladen Turk
 */

public class WMI {

    /* Native object pointer representing wmi_data_t */
    protected long INSTANCE;

    private static native long      create0(String namespace);
    private static native void      term0(long instance);

    /**
     * Object finalize callback.
     */
    protected final void finalize()
    {
        close();
    }

    public WMI()
        throws OperatingSystemException, UnsupportedOperatingSystemException
    {
        if (!OS.IS_WINDOWS)
            throw new UnsupportedOperatingSystemException();
        else
            INSTANCE = create0(null);
    }

    public WMI(String nameSpace)
        throws OperatingSystemException, UnsupportedOperatingSystemException
    {
        if (!OS.IS_WINDOWS)
            throw new UnsupportedOperatingSystemException();
        else
            INSTANCE = create0(nameSpace);
    }

    public void close()
    {
        if (INSTANCE != 0)
            term0(INSTANCE);
        INSTANCE = 0;
    }

}
