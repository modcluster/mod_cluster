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
 * ServiceErrorControl enumeration.
 * Represents severity of the error, and action taken, if this
 * service fails to start.
 */
public enum ServiceErrorControl
{

    /**
     * The startup (boot) program logs the error but continues the
     * startup operation.
     */
    IGNORE(     0),
    /**
     * The startup program logs the error and displays a message
     * box pop-up but continues the startup operation.
     */
    NORMAL(     1),
    /**
     * The startup program logs the error. If the last-known good
     * configuration is being started, the startup operation continues.
     * Otherwise, the system is restarted with the last-known-good
     * configuration.
     */
    SEVERE(     2),
    /**
     * The startup program logs the error, if possible. If the last-known
     * good configuration is being started, the startup operation fails.
     * Otherwise, the system is restarted with the last-known good
     * configuration.
     */
    CRITICAL(   3);

    private int value;
    private ServiceErrorControl(int v)
    {
        value = v;
    }

    public int valueOf()
    {
        return value;
    }

    public static ServiceErrorControl valueOf(int value)
    {
        for (ServiceErrorControl e : values()) {
            if (e.value == value)
                return e;
        }
        return IGNORE;
    }
}
