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
 * ServiceState enumeration.
 */
public enum ServiceState
{

    /**
     * Unknown service state.
     */
    UNKNOWN(            0),
    /**
     * The service is not running.
     */
    STOPPED(            1),
    /**
     * The service is starting.
     */
    START_PENDING(      2),
    /**
     * The service is stopping.
     */
    STOP_PENDING(       3),
    /**
     * The service is running.
     */
    RUNNING(            4),
    /**
     * The service continue is pending.
     */
    CONTINUE_PENDING(   5),
    /**
     * The service pause is pending.
     */
    PAUSE_PENDING(      6),
    /**
     * The service is paused.
     */
    PAUSED(             7),
    /**
     * The service is disabled.
     */
    DISABLED(           8);

    private int value;
    private ServiceState(int v)
    {
        value = v;
    }

    public int valueOf()
    {
        return value;
    }

    public static ServiceState valueOf(int value)
    {
        for (ServiceState e : values()) {
            if (e.value == value)
                return e;
        }
        return UNKNOWN;
    }
}
