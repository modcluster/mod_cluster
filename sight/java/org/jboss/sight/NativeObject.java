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
 * Native Object abstract class
 *
 * @author Mladen Turk
 *
 */

public abstract class NativeObject
{
    /* Native object sight_object_t pointer */
    protected long INSTANCE;
    /* Native object apr_pool_t pointer */
    protected long POOL;

    private static native long  alloc()
                                    throws OutOfMemoryError;
    private static native void  init0(NativeObject thiz,
                                      long instance, long parent,
                                      boolean lockable)
                                    throws OutOfMemoryError;
    private static native void  free0(long instance);
    private static native void  clear0(long instance);
    private static native void  cbset0(long instance, Object cb)
                                    throws OutOfMemoryError;
    private static native void  intr0(long instance);

    private static native void  sleep0(long instance, long step, long time);

    /**
     * Create new NativeObject without APR pool
     */
    public NativeObject()
        throws OutOfMemoryError
    {
        POOL     = 0;
        INSTANCE = alloc();
    }

    /**
     * Create new NativeObject
     * @param parent The parent pool. If this is 0, the new pool is a root
     *        pool. If it is non-zero, the new pool will inherit all
     *        of its parent pool's attributes, except the apr_pool_t will
     *        be a sub-pool.
     * @param lockable Defines if the created object can have child objects
     */
    protected NativeObject(long parent, boolean lockable)
        throws OutOfMemoryError
    {
        POOL     = 0;
        INSTANCE = alloc();
        init0(this, INSTANCE, parent, lockable);
    }

    /**
     * Create new NativeObject
     * @param parent The parent pool. If this is 0, the new pool is a root
     *        pool. If it is non-zero, the new pool will inherit all
     *        of its parent pool's attributes, except the apr_pool_t will
     *        be a sub-pool.
     */
    protected NativeObject(long parent)
        throws OutOfMemoryError
    {
        this(parent, false);
    }

    /**
     * Interrupt the pending native operation.
     */
    public void interrupt()
    {
        intr0(INSTANCE);
    }

    /**
     * Destroy the object.
     */
    public void destroy()
    {
        synchronized(this) {
            free0(INSTANCE);
            INSTANCE = 0;
            POOL     = 0;
        }
    }

    /**
     * Clear the object.
     */
    public void clear()
    {
        synchronized(this) {
            clear0(INSTANCE);
        }
    }

    /**
     * Object finalize callback.
     * Called by the garbage collector on an object when garbage
     * collection determines that there are no more references to the object.
     */
    protected final void finalize()
    {
        if (INSTANCE != 0)
            destroy();
    }

    /**
     * Set the callback method.
     */
    public void setCallback(ICallback callback)
        throws OutOfMemoryError
    {
        cbset0(INSTANCE, callback);
    }

    public void sleep(long step, long time)
    {
        sleep0(INSTANCE, step, time);
    }

    /**
     * Destroy callback method.
     * <BR/>
     * Invoked when the native object is destroyed
     * by its parent Pool destroy. Implementation
     * must not call any native function.
     * by its parent Pool.
     */
    protected abstract void onDestroy();

}
