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

/** Memory
 *
 * @author Mladen Turk
 *
 */

public class Memory extends NativeObject
{

    private static native int   alloc0(Object thiz, long instance, int pid);
    private int processID;

    public Memory()
    {
        super(0);
        processID = -1;
        alloc0(this, INSTANCE, this.processID);
    }

    public Memory(Pool pool)
    {
        super(pool.POOL);
        processID = -1;
        alloc0(this, INSTANCE, this.processID);
    }

    protected Memory(int processID)
    {
        super(0);
        this.processID = processID;
        alloc0(this, INSTANCE, this.processID);
    }

    protected Memory(long pool, int processID)
    {
        super(pool);
        this.processID = processID;
        alloc0(this, INSTANCE, this.processID);
    }

    protected void onDestroy()
    {
        // Nothing
    }

    /**
     * Refresh the CPU statistics.
     */
    public int refresh()
    {
        clear();
        return alloc0(this, INSTANCE, processID);
    }

    /**
     * Amount of used physical memory, in bytes
     */
    public long Physical;

    /**
     * Amount of available physical memory, in bytes
     */
    public long AvailPhysical;

    /**
     * Amount of used swap memory, in bytes
     */
    public long Swap;

    /**
     * Amount of available swap memory, in bytes
     */
    public long AvailSwap;

    /**
     * Amount of memory used by Kernel, in bytes
     */
    public long Kernel;

    /**
     * Total amount of system cache memory, in bytes
     */
    public long Cached;

    /**
     * Number between 0 and 100 that specifies the approximate
     * percentage of physical memory that is in use (0 indicates no
     * memory use and 100 indicates full memory use).
     */
    public int Load;

    /**
     * Memory page size, in bytes.
     */
    public int Pagesize;

    /**
     * Resident set size, in bytes.
     */
    public long RSS;

    /**
     * Shared memory size, in bytes.
     */
    public long Shared;

    /**
     * Number of page faults.
     */
    public long PageFaults;

    /**
     * Maximum Virtual memory size.
     * This is the maximum size the process can theoretically allocate.
     */
    public long MaxVirtual;

}
