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
 * Mutex
 * APR Process Locking Routines
 *
 * @author Mladen Turk
 *
 */

public class Mutex extends NativeObject
{

    private static native int       create0(long instance, String fname, int mech, boolean global)
                                        throws NullPointerException, OperatingSystemException;
    private static native int       child0(long instance, String fname, boolean global)
                                        throws NullPointerException, OperatingSystemException;
    private static native int       lock0(long instance);
    private static native int       trylock0(long instance);
    private static native int       unlock0(long instance);
    private static native String    name0(long instance);
    private static native String    fname0(long instance);

    /**
     * Does the proc mutex lock threads too.
     */
    public static boolean PROC_MUTEX_IS_GLOBAL = false;
    protected boolean global;

    /**
     * Display the name of the default mutex: APR_LOCK_DEFAULT
     */
    public static native String getDefaultName();

    /**
     * Create new Mutex with root as parent pool.
     */
    public Mutex()
    {
        super(0);
        global = false;
    }

    /**
     * Create new Mutex
     * @param parent The parent pool. If this is 0, the new pool is a root
     *        pool. If it is non-zero, the new pool will inherit all
     *        of its parent pool's attributes, except the apr_pool_t will
     *        be a sub-pool.
     */
    public Mutex(Pool parent)
    {
        super(parent.POOL);
        global = false;
    }

    /**
     * Create new Mutex
     * @param fname A file name to use if the lock mechanism requires one.  This
     *        argument should always be provided.  The lock code itself will
     *        determine if it should be used.
     * @param mech The mechanism to use for the interprocess lock, if any; one of
     * <PRE>
     *            APR_LOCK_FCNTL
     *            APR_LOCK_FLOCK
     *            APR_LOCK_SYSVSEM
     *            APR_LOCK_POSIXSEM
     *            APR_LOCK_PROC_PTHREAD
     *            APR_LOCK_DEFAULT     pick the default mechanism for the platform
     * </PRE>
     */
    public Mutex(String fname, MutexType mech)
        throws NullPointerException, OperatingSystemException
    {
        super(0);
        global = false;
        create(fname, mech);
    }

    /**
     * Create new Mutex
     * @param fname A file name to use if the lock mechanism requires one.  This
     *        argument should always be provided.  The lock code itself will
     *        determine if it should be used.
     * @param mech The mechanism to use for the interprocess lock, if any; one of
     * <PRE>
     *            APR_LOCK_FCNTL
     *            APR_LOCK_FLOCK
     *            APR_LOCK_SYSVSEM
     *            APR_LOCK_POSIXSEM
     *            APR_LOCK_PROC_PTHREAD
     *            APR_LOCK_DEFAULT     pick the default mechanism for the platform
     * </PRE>
     * @param parent The parent pool. If this is 0, the new pool is a root
     *        pool. If it is non-zero, the new pool will inherit all
     *        of its parent pool's attributes, except the apr_pool_t will
     *        be a sub-pool.
     */
    public Mutex(String fname, MutexType mech, Pool parent)
        throws NullPointerException, OperatingSystemException
    {
        super(parent.POOL);
        global = false;
        create(fname, mech);
    }

    /**
     * Display the name of the mutex, as it relates to the actual method used.
     * This matches the valid options for Apache's AcceptMutex directive
     */
    public String getName()
    {
        return name0(INSTANCE);
    }

    /**
     * Return the name of the lockfile for the mutex, or NULL
     * if the mutex doesn't use a lock file
     */
    public String getLockFileName()
    {
        return fname0(INSTANCE);
    }

    protected void onDestroy()
    {
        // Nothing
    }

    /**
     * Create and initialize a mutex that can be used to synchronize processes.
     * <br /><b>Warning :</b> Check APR_HAS_foo_SERIALIZE defines to see if the platform supports
     *          APR_LOCK_foo.  Only APR_LOCK_DEFAULT is portable.
     * @param fname A file name to use if the lock mechanism requires one.  This
     *        argument should always be provided.  The lock code itself will
     *        determine if it should be used.
     * @param mech The mechanism to use for the interprocess lock, if any; one of
     * <PRE>
     *            APR_LOCK_FCNTL
     *            APR_LOCK_FLOCK
     *            APR_LOCK_SYSVSEM
     *            APR_LOCK_POSIXSEM
     *            APR_LOCK_PROC_PTHREAD
     *            APR_LOCK_DEFAULT     pick the default mechanism for the platform
     * </PRE>
     * @return APR status code.
     */
    public int create(String fname, MutexType mech)
        throws NullPointerException, OperatingSystemException
    {
        return create0(INSTANCE, fname, mech.valueOf(), global);
    }

    /**
     * Re-open a mutex in a child process.
     * This function must be called to maintain portability, even
     * if the underlying lock mechanism does not require it.
     * @param fname A file name to use if the mutex mechanism requires one.  This
     *              argument should always be provided.  The mutex code itself will
     *              determine if it should be used.  This filename should be the
     *              same one that was passed to apr_proc_mutex_create().
     * @return APR status code.
     */
    public int childInit(String fname)
        throws NullPointerException, OperatingSystemException
    {
        return child0(INSTANCE, fname, global);
    }
    /**
     * Acquire the lock for the given mutex. If the mutex is already locked,
     * the current thread will be put to sleep until the lock becomes available.
     */
    public int lock()
    {
        return lock0(INSTANCE);
    }

    /**
     * Attempt to acquire the lock for the given mutex. If the mutex has already
     * been acquired, the call returns immediately with APR_EBUSY. Note: it
     * is important that the APR_STATUS_IS_EBUSY(s) macro be used to determine
     * if the return value was APR_EBUSY, for portability reasons.
     */
    public int tryLock()
    {
        return trylock0(INSTANCE);
    }

    /**
     * Release the lock for the given mutex.
     */
    public int unlock()
    {
        return unlock0(INSTANCE);
    }

    /**
     * Does this mutex lock threads too.
     */
    public boolean isGlobal()
    {
        if (PROC_MUTEX_IS_GLOBAL)
            return true;
        else
            return global;
    }
}
