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

/**
 * ServiceStartType enumeration.
 * Determines when to start the service.
 */
public enum ServiceStartType
{

    /**
     * A device driver started by the system loader.
     * This value is valid only for driver services.
     */
    BOOT(       0),
    /**
     * A device driver started by the IoInitSystem function.
     * This value is valid only for driver services.
     */
    SYSTEM(     1),
    /**
     * A service started automatically by the service control manager
     * during system startup.
     */
    AUTO(       2),
    /**
     * A service started by the service control manager when a process
     * calls the StartService function.
     */
    DEMAND(     3),
    /**
     * A service that cannot be started. Attempts to start the service
     * result in the error code ERROR_SERVICE_DISABLED.
     */
    DISABLED(   4);

    private int value;
    private ServiceStartType(int v)
    {
        value = v;
    }

    public int valueOf()
    {
        return value;
    }

    public static ServiceStartType valueOf(int value)
    {
        for (ServiceStartType e : values()) {
            if (e.value == value)
                return e;
        }
        return BOOT;
    }
}
