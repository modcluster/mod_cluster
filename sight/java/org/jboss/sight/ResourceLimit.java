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
 * Resource Utilization limits when starting a new process.
 */
public enum ResourceLimit
{
    /**
     * The maximum amount of CPU time in seconds used by a
     * process. This is a soft limit only. The SIGXCPU
     * signal is sent to the process. If the process is holding
     * or ignoring SIGXCPU, the behavior is scheduling class defined.
     */
    CPU(    0),

    /**
     * The maximum size of a process's heap in bytes. The
     * memory allocation functions will fail with errno set to ENOMEM.
     */
    MEM(    1),

    /**
     * Number of child processes the process may create.
     */
    NPROC(  2),

    /**
     * One more than the maximum value that the system may
     * assign to a newly created descriptor. This limit constrains
     * the number of file descriptors that a process may create.
     */
    NOFILE( 3);


    private int value;
    private ResourceLimit(int v)
    {
        value = v;
    }

    public int valueOf()
    {
        return value;
    }

    public static ResourceLimit valueOf(int value)
    {
        for (ResourceLimit e : values()) {
            if (e.value == value)
                return e;
        }
        throw new IllegalArgumentException("Invalid initializer: " + value);
    }

}
