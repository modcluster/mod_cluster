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
 * ServiceControlsAccepted flags.
 * Control codes the service accepts and processes in its handler function.
 */
public enum ServiceControlsAccepted
{

    /**
     * By default, all services accept the
     * {@link ServiceControl#INTERROGATE ServiceControl.INTERROGATE} value.
     */
    DEFAULT(        0x00000000),

    /**
     * The service can be stopped.
     * This control code allows the service to receive
     * {@link ServiceControl#STOP ServiceControl.STOP} notifications.
     */
    STOP(           0x00000001),

    /**
     * The service can be paused and continued.
     * This control code allows the service to receive
     * {@link ServiceControl#PAUSE ServiceControl.PAUSE} and
     * {@link ServiceControl#CONTINUE ServiceControl.CONTINUE}
     * notifications.
     */
    PAUSE_CONTINUE( 0x00000002),

    /**
     * The service is notified when system shutdown occurs.
     * This control code allows the service to receive
     * {@link ServiceControl#SHUTDOWN ServiceControl.SHUTDOWN} notifications.
     * <BR/>
     * Note that ControlService cannot
     * send this notification; only the system can send it.
     */
    SHUTDOWN(       0x00000004);

    private int value;
    private ServiceControlsAccepted(int v)
    {
        value = v;
    }

    public int valueOf()
    {
        return value;
    }

    public static int bitmapOf(EnumSet<ServiceControlsAccepted> set)
    {
        int bitmap = 0;
        if (set != null) {
            for (ServiceControlsAccepted a : set)
                bitmap += a.valueOf();
        }
        return bitmap;
    }

    public static EnumSet<ServiceControlsAccepted> valueOf(int value)
    {
        EnumSet<ServiceControlsAccepted> set = EnumSet.noneOf(ServiceControlsAccepted.class);
        if (value == 0) {
            set.add(DEFAULT);
            return set;
        }
        for (ServiceControlsAccepted e : values()) {
            if (e == DEFAULT)
                continue;
            if ((e.value & value) == e.value)
                set.add(e);
        }
        return set;
    }
}
