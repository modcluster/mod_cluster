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
 * Pool Callback interface.
 * <BR/>
 * Cleanups are performed in the reverse order they were registered.  That is:
 * Last In, First Out.  A cleanup function can safely allocate memory from
 * the pool that is being cleaned up. It can also safely register additional
 * cleanups which will be run LIFO, directly after the current cleanup
 * terminates.  Cleanups have to take caution in calling functions that
 * create subpools. Subpools, created during cleanup will NOT automatically
 * be cleaned up.  In other words, cleanups are to clean up after themselves.
 *
 * @author Mladen Turk
 *
 */
public interface IPoolCallback
{
    /**
     * The function to call when the pool is cleared
     * or destroyed.
     * @return APR_SUCCESS in case of success.
     */
    public int cleanup();

}
