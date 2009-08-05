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

/** Cpu
 *
 * @author Mladen Turk
 *
 */

public class Cpu extends NativeObject
{

    private static native int   alloc0(Object thiz, long instance);
    private static native int   stats0(Object thiz, long instance, int id);

    private int processorNumber;

    public Cpu()
    {
        super(0);
        this.processorNumber = 0;
        alloc0(this, INSTANCE);
        stats0(this, INSTANCE, this.processorNumber);
    }

    public Cpu(Pool pool)
    {
        super(pool.POOL);
        this.processorNumber = 0;
        alloc0(this, INSTANCE);
        stats0(this, INSTANCE, this.processorNumber);
    }

    public Cpu(int processorNumber)
    {
        super(0);
        this.processorNumber = processorNumber + 1;
        alloc0(this, INSTANCE);
        stats0(this, INSTANCE, this.processorNumber);
    }

    public Cpu(Pool pool, int processorNumber)
    {
        super(pool.POOL);
        this.processorNumber = processorNumber + 1;
        alloc0(this, INSTANCE);
        stats0(this, INSTANCE, this.processorNumber);
    }

    /**
     * Refresh the CPU statistics.
     */
    public int refresh()
    {
        long it  = IdleTime;
        long st  = SystemTime;
        long ut  = UserTime;
        long tt  = UserTime + NicedTime + SystemTime + IdleTime + WaitTime + IrqTime + SoftirqTime;
        clear();
        int rv   = stats0(this, INSTANCE, processorNumber);
        long dt  = UserTime + NicedTime + SystemTime + IdleTime + WaitTime + IrqTime + SoftirqTime - tt;
        IdleLoad   = (int)((IdleTime   - it) * 100L / dt);
        SystemLoad = (int)((SystemTime - st) * 100L / dt);
        UserLoad   = (int)((UserTime   - ut) * 100L / dt);
        return rv;
    }

    protected void onDestroy()
    {
        // Nothing
    }

    /**
     * The number between 0 and 100 showing how much the
     * CPU was idle between two <code>refresh()</code> calls.
     */
    public int      IdleLoad;

    /**
     * The number between 0 and 100 showing how much the
     * CPU spend in System mode between two <code>refresh()</code> calls.
     */
    public int      SystemLoad;

    /**
     * The number between 0 and 100 showing how much the
     * CPU spend in User mode between two <code>refresh()</code> calls.
     */
    public int      UserLoad;

    /**
     * True if the CPU is Big endian.
     */
    public boolean  IsBigEndian;

    /**
     * Number of processors in the system.
     */
    public int      NumberOfProcessors;

    /**
     * Processors number in the system.
     */
    public int      Processor;

    /**
     * System's architecture-dependent processor level.
     * It should be used only for display purposes.
     */
    public String   Family;

    /**
     * Processor model.
     */
    public String   Model;

    /**
     * Processor stepping number.
     */
    public String   Stepping;

    /**
     * Processor vendor.
     */
    public String   Vendor;

    /**
     * Processor display name.
     */
    public String   Name;

    /**
     * Processor speed in MHz.
     */
    public double   MHz;

    /**
     * Processor Bogomips value.
     * Available only on {@link OS#IS_LINUX OS.IS_LINUX}
     */
    public double   Bogomips;


    /**
     * Normal processes executing time in user mode.
     */
    public long     UserTime;

    /**
     * Niced processes executing time in user mode.
     */
    public long     NicedTime;

    /**
     * Time processes executing in kernel mode.
     */
    public long     SystemTime;

    /**
     * Idle executing time.
     */
    public long     IdleTime;

    /**
     * Time spent waiting for I/O to complete.
     */
    public long     WaitTime;

    /**
     * Time spent servicing interrupts.
     */
    public long     IrqTime;

    /**
     * Time spent servicing softirqs.
     */
    public long     SoftirqTime;

    /**
     * Total number of context switches across all CPUs.
     */
    public long     ContextSwitched;

    /**
     * Gives the time at which the system booted.
     */
    public long     BootTime;

    /**
     * Number of processes and threads created, which includes
     * (but is not limited to) those created by calls to the fork()
     * and clone() system calls.
     */
    public int      Processes;

    /**
     * Number of processes currently running on CPUs.
     */
    public int      RunningProcesses;

    /**
     * Number of processes currently blocked, waiting for I/O to complete.
     */
    public int      BlockedProcesses;

}
