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
 * UDP statistics informations
 *
 * @author Mladen Turk
 *
 */

public class UdpStatistics extends NativeObject
{

    private static native void info0(UdpStatistics thiz, int iface, long pool)
                                throws OutOfMemoryError, OperatingSystemException;
    private static native long enum0(int iftype, long pool)
                                throws OutOfMemoryError, OperatingSystemException;
    private static native int  enum1(long handle);
    private static native void enum2(UdpConnection thiz, int idx, long handle)
                                throws OperatingSystemException;
    private static native void enum3(long handle);

    private NetworkAddressFamily family;

    public UdpStatistics(NetworkAddressFamily family)
        throws OutOfMemoryError, OperatingSystemException
    {
        super(0);
        this.family = family;
        info0(this, family.valueOf(), POOL);
    }

    /**
     * Refresh the UdpStatistics.
     */
    public void refresh()
        throws OutOfMemoryError, OperatingSystemException
    {
        clear();
        info0(this, family.valueOf(), POOL);
    }

    protected void onDestroy()
    {
        // Nothing
    }

    /**
     * Retrieves a list of UDP endpoints available.
     */
    public UdpConnectionIterator getUdpConnections()
        throws OutOfMemoryError, OperatingSystemException
    {
        UdpConnection[] array;
        long handle = enum0(family.valueOf(), POOL);
        try {
            int cnt = enum1(handle);
            array = new UdpConnection[cnt];
            for (int i = 0; i < cnt; i++) {
                array[i] = new UdpConnection();
                enum2(array[i], i, handle);
            }
            return new UdpConnectionIterator(array);
        }
        finally {
            // Do not leak memory
            enum3(handle);
        }
    }

    /**
     * Specifies the number of datagrams received.
     */
    public int      InDatagrams;

    /**
     * Specifies the number of datagrams received that were discarded
     * because the port specified was invalid.
     */
    public int      NoPorts;

    /**
     * Specifies the number of erroneous datagrams received.
     * This number does not include the value contained by
     * the NoPorts member.
     */
    public int      InErrors;

    /**
     * Specifies the number of datagrams transmitted.
     */
    public int      OutDatagrams;

    /**
     * Specifies the number of entries in the UDP listener table.
     */
    public int      NumAddrs;

}
