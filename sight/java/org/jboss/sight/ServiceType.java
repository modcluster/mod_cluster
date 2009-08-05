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
 * Type of service.
 */
public enum ServiceType
{

    /** Default system service type.  */
    DEFAULT(            0x00000000),
    /** The service is a device driver.  */
    FILE_SYSTEM_DRIVER( 0x00000002),
     /** The service is a device driver */
    KERNEL_DRIVER(      0x00000001),
    /** The service runs in its own process. */
    OWN_PROCESS(        0x00000010),
    /** The service shares a process with other services. */
    SHARE_PROCESS(      0x00000020),
    /** The service can interact with the desktop. */
    INTERACTIVE_PROCESS(0x00000100);


    private int value;
    private ServiceType(int v)
    {
        value = v;
    }

    public int valueOf()
    {
        return value;
    }

    public static int bitmapOf(EnumSet<ServiceType> set)
    {
        int bitmap = 0;
        if (set != null) {
            for (ServiceType a : set)
                bitmap += a.valueOf();
        }
        return bitmap;
    }

    public static EnumSet<ServiceType> valueOf(int value)
    {
        EnumSet<ServiceType> set = EnumSet.noneOf(ServiceType.class);
        if (value == 0) {
            set.add(DEFAULT);
            return set;
        }
        for (ServiceType e : values()) {
            if (e == DEFAULT)
                continue;
            if ((e.value & value) == e.value)
                set.add(e);
        }
        return set;
    }

}
