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

/** Pool
 *
 * @author Mladen Turk
 *
 */

public class Pool extends NativeObject
{

    private static native long  register0(long instance, IPoolCallback o)
                                    throws NullPointerException;
    private static native void  kill0(long instance, long data)
                                    throws NullPointerException;
    private static native void  notes0(long instance, long proc, int how);

    /**
     * Create new Pool with root as parent pool.
     */
    public Pool()
    {
        super(0, true);
    }

    /**
     * Create new NativeObject
     * @param parent The parent pool. If this is 0, the new pool is a root
     *        pool. If it is non-zero, the new pool will inherit all
     *        of its parent pool's attributes, except the apr_pool_t will
     *        be a sub-pool.
     */
    public Pool(Pool parent)
    {
        super(parent.POOL, true);
    }

    protected void onDestroy()
    {
        // Nothing
    }

    /**
     * Get native APR pool pointer.
     */
    public long getPoolHandle()
    {
        return POOL;
    }

    /**
     *  Create a new pool as sub-pool of this one.
     */
    public Pool create()
    {
        return new Pool(this);
    }

    /*
     * Cleanup
     *
     * Cleanups are performed in the reverse order they were registered.  That is:
     * Last In, First Out.  A cleanup function can safely allocate memory from
     * the pool that is being cleaned up. It can also safely register additional
     * cleanups which will be run LIFO, directly after the current cleanup
     * terminates.  Cleanups have to take caution in calling functions that
     * create subpools. Subpools, created during cleanup will NOT automatically
     * be cleaned up.  In other words, cleanups are to clean up after themselves.
     */

    /**
     * Register a function to be called when a pool is cleared or destroyed
     * @param callback The object to call when the pool is cleared
     *                 or destroyed.
     * @return The cleanup handler.
     */
    public long cleanupRegister(IPoolCallback callback)
        throws NullPointerException
    {
        return register0(INSTANCE, callback);
    }

    /**
     * Remove a previously registered cleanup function
     * @param data The cleanup handler to remove from cleanup
     */
    public void cleanupKill(long data)
        throws NullPointerException
    {
        kill0(INSTANCE, data);
    }

    /**
     * Register a process to be killed when a pool dies.
     * @param proc The Process to register
     * @param how How to kill the process, one of:
     * <PRE>
     *         KILL_NEVER         -- process is never sent any signals
     *         KILL_ALWAYS        -- process is sent SIGKILL on apr_pool_t cleanup
     *         KILL_AFTER_TIMEOUT -- SIGTERM, wait 3 seconds, SIGKILL
     *         JUST_WAIT          -- wait forever for the process to complete
     *         KILL_ONLY_ONCE     -- send SIGTERM and then wait
     * </PRE>
     */
    public void noteSubprocess(Process proc, KillConditions how)
    {
        notes0(INSTANCE, proc.INSTANCE, how.valueOf());
    }

}
