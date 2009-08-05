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
 * Enumerated potential types for APR process locking methods.
 * <br /><b>Warning :</b> Check APR_HAS_foo_SERIALIZE defines to see
 * if the platform supports APR_LOCK_foo.
 * Only DEFAULT is portable.
 */
public enum MutexType
{
    /** Use fcntl() for lockning */
    FCNTL(       0),

    /** Use flock() for lockingv */
    FLOCK(       1),

    /** System V Semaphores */
    SYSVSEM(     2),

    /** POSIX pthread process-based locking */
    PROC_PTHREAD(3),

    /** POSIX semaphore process-based locking */
    POSIXSEM(    4),

    /** Use the default process lock */
    DEFAULT(     5);

    private int value;
    private MutexType(int v)
    {
        value = v;
    }

    public int valueOf()
    {
        return value;
    }

    public static MutexType valueOf(int value)
    {
        for (MutexType e : values()) {
            if (e.value == value)
                return e;
        }
        throw new IllegalArgumentException("Invalid initializer: " + value);
    }

}
