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

import java.util.Iterator;

/**
 * Module Information.
 * <br />
 * @author Mladen Turk
 *
 */
public final class Module
{

    private static native Module[]  enum0(int pid)
                                        throws OperatingSystemException;


    private Module(int pid, int id)
    {
        Id = id;
        ProcessId = pid;
    }

    /**
     * Module id
     */
    public int             Id;

    /**
     * Module Process id
     */
    public int             ProcessId;


    /**
     * Module name
     */
    public String           Name;

    /**
     * Module executable file
     */
    public String           BaseName;

    /**
     * Base address of the module in the context of the
     * owning process.
     */
    public long             BaseAddress;

    /**
     * Size of the module, in bytes
     */
    public long             Size;

    protected static Module[] getModules(int pid)
        throws OperatingSystemException
    {
        return enum0(pid);
    }



}
