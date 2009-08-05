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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Shm
 *
 * @author Mladen Turk
 *
 */

public class Shm extends NativeObject
{

    private static native int           create0(long instance, String name, long size)
                                            throws NullPointerException;
    private static native int           attach0(long instance, String name)
                                            throws NullPointerException;
    private static native long          size0(long instance);
    private static native ByteBuffer    addr0(long instance, long size);
    private static native int           remove0(String name, long pool);

    private ByteBuffer bb = null;
    /**
     * Create new Shm with root as parent pool.
     */
    public Shm()
    {
        super(0);
    }


    /**
     * Create new Shm
     * @param parent The parent pool. If this is 0, the new pool is a root
     *        pool. If it is non-zero, the new pool will inherit all
     *        of its parent pool's attributes, except the apr_pool_t will
     *        be a sub-pool.
     */
    public Shm(Pool parent)
    {
        super(parent.POOL);
    }

    /**
     * Create and make accessable a shared memory segment.
     * <br />
     * A note about Anonymous vs. Named shared memory segments:<br />
     *         Not all plaforms support anonymous shared memory segments, but in
     *         some cases it is prefered over other types of shared memory
     *         implementations. Passing a NULL 'file' parameter to this function
     *         will cause the subsystem to use anonymous shared memory segments.
     *         If such a system is not available, APR_ENOTIMPL is returned.
     * <br />
     * A note about allocation sizes:<br />
     *         On some platforms it is necessary to store some metainformation
     *         about the segment within the actual segment. In order to supply
     *         the caller with the requested size it may be necessary for the
     *         implementation to request a slightly greater segment length
     *         from the subsystem. In all cases, the apr_shm_baseaddr_get()
     *         function will return the first usable byte of memory.
     * @param reqsize The desired size of the segment.
     * @param name The file to use for shared memory on platforms that
     *        require it.
     */
    public Shm(String name, long reqsize)
        throws NullPointerException, OperatingSystemException
    {
        super(0);
        int rv = create0(INSTANCE, name, reqsize);
        if (rv != Error.APR_SUCCESS) {
            throw new OperatingSystemException(Error.getError(rv));
        }
    }

    /**
     * Create and make accessable a shared memory segment.
     * <br />
     * A note about Anonymous vs. Named shared memory segments:<br />
     *         Not all plaforms support anonymous shared memory segments, but in
     *         some cases it is prefered over other types of shared memory
     *         implementations. Passing a NULL 'file' parameter to this function
     *         will cause the subsystem to use anonymous shared memory segments.
     *         If such a system is not available, APR_ENOTIMPL is returned.
     * <br />
     * A note about allocation sizes:<br />
     *         On some platforms it is necessary to store some metainformation
     *         about the segment within the actual segment. In order to supply
     *         the caller with the requested size it may be necessary for the
     *         implementation to request a slightly greater segment length
     *         from the subsystem. In all cases, the apr_shm_baseaddr_get()
     *         function will return the first usable byte of memory.
     * @param reqsize The desired size of the segment.
     * @param name The file to use for shared memory on platforms that
     *        require it.
     * @param parent The parent pool. If this is 0, the new pool is a root
     *        pool. If it is non-zero, the new pool will inherit all
     *        of its parent pool's attributes, except the apr_pool_t will
     *        be a sub-pool.
     */
    public Shm(String name, long reqsize, Pool parent)
        throws NullPointerException, OperatingSystemException
    {
        super(parent.POOL);
        int rv = create0(INSTANCE, name, reqsize);
        if (rv != Error.APR_SUCCESS) {
            throw new OperatingSystemException(Error.getError(rv));
        }
    }

    protected void onDestroy()
    {
        // Nothing
    }

    /**
     * Create and make accessable a shared memory segment.
     * <br />
     * A note about Anonymous vs. Named shared memory segments:<br />
     *         Not all plaforms support anonymous shared memory segments, but in
     *         some cases it is prefered over other types of shared memory
     *         implementations. Passing a NULL 'file' parameter to this function
     *         will cause the subsystem to use anonymous shared memory segments.
     *         If such a system is not available, APR_ENOTIMPL is returned.
     * <br />
     * A note about allocation sizes:<br />
     *         On some platforms it is necessary to store some metainformation
     *         about the segment within the actual segment. In order to supply
     *         the caller with the requested size it may be necessary for the
     *         implementation to request a slightly greater segment length
     *         from the subsystem. In all cases, the apr_shm_baseaddr_get()
     *         function will return the first usable byte of memory.
     * @param reqsize The desired size of the segment.
     * @param name The file to use for shared memory on platforms that
     *        require it.
     */
    public int create(String name, long reqsize)
        throws NullPointerException
    {
        return create0(INSTANCE, name, reqsize);
    }

    /**
     * Attach to a shared memory segment that was created
     * by another process.
     * @param name The file used to create the original segment.
     *        (This MUST match the original filename.)
     * @return APR status code.
     */
    public int attach(String name)
        throws NullPointerException
    {
        return attach0(INSTANCE, name);
    }

    /**
     * Retrieve the length of a shared memory segment in bytes.
     */
    public long size()
    {
        return size0(INSTANCE);
    }

    /**
     * Retrieve new ByteBuffer base address of the shared memory segment.
     * NOTE: This address is only usable within the callers address
     * space, since this API does not guarantee that other attaching
     * processes will maintain the same address mapping.
     * @return address, aligned by APR_ALIGN_DEFAULT.
     */
    public ByteBuffer getBytes()
    {
        if (bb == null) {
            bb = addr0(INSTANCE, -1);
            if (bb != null) {
                // Always use network byte order
                bb.order(ByteOrder.BIG_ENDIAN);
            }
        }
        return bb;
    }

    /**
     * Retrieve new ByteBuffer base address of the shared memory segment.
     * NOTE: This address is only usable within the callers address
     * space, since this API does not guarantee that other attaching
     * processes will maintain the same address mapping.
     * @param size Size to map.
     * @return address, aligned by APR_ALIGN_DEFAULT.
     */
    public ByteBuffer getBytes(long size)
    {
        if (bb == null) {
            bb = addr0(INSTANCE, size);
            if (bb != null) {
                // Always use network byte order
                bb.order(ByteOrder.BIG_ENDIAN);
            }
        }
        return bb;
    }

    /**
     * Remove shared memory segment associated with a filename.
     * <br />
     * This function is only supported on platforms which support
     * name-based shared memory segments, and will return APR_ENOTIMPL on
     * platforms without such support.
     * @param filename The filename associated with shared-memory segment which
     *        needs to be removed
     * @param pool The pool used for file operations
     * @return APR status code.
     */
    public static int remove(String filename, Pool pool)
    {
        return remove0(filename, pool.POOL);
    }
}
