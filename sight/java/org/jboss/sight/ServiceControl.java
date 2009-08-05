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
 * ServiceControl types.
 */
public enum ServiceControl
{

    UNKNOWN(       -1),
    /**
     * Notifies a service that it should start.
     */
    START(          0),
    /**
     * Notifies a service that it should stop.<BR/>
     * After sending the stop request to a service, you should not send
     * other controls to the service.
     */
    STOP(           1),
    /**
     * Notifies a service that it should pause.
     */
    PAUSE(          2),
    /**
     * Notifies a paused service that it should resume.
     */
    CONTINUE(       3),
    /**
     * Notifies a service that it should report its current status
     * information to the service control manager.
     */
    INTERROGATE(    4),
    /**
     * Notifies a service when system shutdown occurs.
     * <BR/>
     * Note that ControlService cannot
     * send this notification; only the system can send it.
     */
    SHUTDOWN(       5),
    /**
     * Notifies a service that its startup parameters have changed.
     */
    PARAMCHANGE(    6),
    /**
     * The service defines the action associated with the control code.
     * Range 128 to 255.
     */
    USER(         128);

    private int value;
    private ServiceControl(int v)
    {
        value = v;
    }

    public int valueOf()
    {
        return value;
    }

    public static ServiceControl valueOf(int value)
    {
        for (ServiceControl e : values()) {
            if (e.value == value)
                return e;
        }
        if (value > USER.value)
            return USER;
        else
            return UNKNOWN;
    }
}
