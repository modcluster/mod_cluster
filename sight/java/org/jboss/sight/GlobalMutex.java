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
 * GlobalMutex
 * APR Process Locking Routines
 *
 * @author Mladen Turk
 *
 */

public class GlobalMutex extends Mutex
{

    /**
     * Create new Mutex with root as parent pool.
     */
    public GlobalMutex()
    {
        super();
        global = true;
    }

    /**
     * Create new Mutex
     * @param parent The parent pool. If this is 0, the new pool is a root
     *        pool. If it is non-zero, the new pool will inherit all
     *        of its parent pool's attributes, except the apr_pool_t will
     *        be a sub-pool.
     */
    public GlobalMutex(Pool parent)
    {
        super(parent);
        global = true;
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
    public GlobalMutex(String fname, MutexType mech)
        throws NullPointerException, OperatingSystemException
    {
        super();
        global = true;
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
    public GlobalMutex(String fname, MutexType mech, Pool parent)
        throws NullPointerException, OperatingSystemException
    {
        super(parent);
        global = true;
        create(fname, mech);
    }

}
