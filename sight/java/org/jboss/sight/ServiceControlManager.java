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

import java.util.EnumSet;

/**
 * ServiceControlManager
 * Manages service control manager database
 *
 * @author Mladen Turk
 *
 */

public class ServiceControlManager extends NativeObject
{

    private static native int       open0(long instance, String database, int mode);
    private static native int       close0(long instance);

    private static native String[]  enum0(long instance, int drivers, int what)
                                        throws OutOfMemoryError, OperatingSystemException;

    public ServiceControlManager()
    {
        super(0);
    }

    public ServiceControlManager(Pool parent)
    {
        super(parent.POOL);
    }

    protected void onDestroy()
    {
        // Nothing
    }

    /**
     * Establishes a connection to the service control manager on the
     * local computer.
     * @param database The name of the service control manager
     *        database to open.<BR/>
     *        Depending on the OS if this parameter is <code>null</code>
     *        it will default to the OS specific service database:
     *<PRE>
     *  Windows:    If it is <code>null</code>, the <code>ServicesActive</code>
     *              database is will be opened. The other valid datbase is
     *              <code>ServicesFailed</code>.
     *  Linux:      If it is <code>null</code>, the services from <code>/etc/rc.d/rc5.d</code>
     *              will be used.
     *</PRE>
     * @param access Access to the service control manager. For a list
     *               of access rights, see GenericAccessRights.
     */
    public void open(String database, EnumSet<GenericAccessRights> access)
        throws NullPointerException, OperatingSystemException
    {
        int  a = 0;
        for (GenericAccessRights i : access)
            a += i.valueOf();
        a = open0(INSTANCE, database, a);
        if (a != Error.APR_SUCCESS) {
            throw new OperatingSystemException(Error.getError(a));
        }
    }

    /*
     * if what is 1 then only active services are shown.
     * if what is 2 then only inactive services are shown.
     * in any other case all services are shown
     */
    private String[] getServiceNames(int what)
        throws OutOfMemoryError, OperatingSystemException
    {
        return enum0(INSTANCE, 0, what);
    }

    /**
     * Get the list of all services on the system
     * @return Service Iterator.
     */
    public ServiceIterator getServices()
        throws OutOfMemoryError, OperatingSystemException
    {
        return new ServiceIterator(this,
                                   enum0(INSTANCE, 0, 0));
    }

    /**
     * Get the list of all driver on the system
     * @return Service Iterator.
     */
    public ServiceIterator getDrivers()
        throws OutOfMemoryError, OperatingSystemException
    {
        return new ServiceIterator(this,
                                   enum0(INSTANCE, 1, 0));
    }


    /**
     * Closes a connection to the service control manager on the
     * local computer. The method will invalidate all opened
     * Services connected to this manager.
     */
    public void close()
        throws NullPointerException, OperatingSystemException
    {
        close0(INSTANCE);
    }

}
