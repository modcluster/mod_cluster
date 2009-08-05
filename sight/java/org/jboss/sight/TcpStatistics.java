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
 * TCP statistics informations
 *
 * @author Mladen Turk
 *
 */

public class TcpStatistics extends NativeObject
{

    private static native void info0(TcpStatistics thiz, int iface, long pool)
                                throws OutOfMemoryError, OperatingSystemException;
    private static native long enum0(int iftype, long pool)
                                throws OutOfMemoryError, OperatingSystemException;
    private static native int  enum1(long handle);
    private static native void enum2(TcpConnection thiz, int idx, long handle)
                                throws OperatingSystemException;
    private static native void enum3(long handle);


    private NetworkAddressFamily family;

    public TcpStatistics(NetworkAddressFamily family)
        throws OutOfMemoryError, OperatingSystemException
    {
        super(0);
        this.family = family;
        info0(this, family.valueOf(), POOL);
    }

    /**
     * Refresh the TcpStatistics.
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
     * Retrieves a list of TCP endpoints available.
     */
    public TcpConnectionIterator getTcpConnections()
        throws OutOfMemoryError, OperatingSystemException
    {
        TcpConnection[] array;
        long handle = enum0(family.valueOf(), POOL);
        try {
            int cnt = enum1(handle);
            array = new TcpConnection[cnt];
            for (int i = 0; i < cnt; i++) {
                array[i] = new TcpConnection();
                enum2(array[i], i, handle);
            }
            return new TcpConnectionIterator(array);
        }
        finally {
            // Do not leak memory
            enum3(handle);
        }
    }


    /**
     * Specifies the minimum retransmission time-out (RTO)
     * value in milliseconds.
     */
    public int      RtoMin;

    /**
     * Specifies the maximum retransmission time-out (RTO)
     * value in milliseconds.
     */
    public int      RtoMax;

    /**
     * Specifies the maximum number of connections. If this member is
     * <code>-1</code>, the maximum number of connections is variable.
     */
    public int      MaxConn;

    /**
     * Specifies the number of active opens. In an active open,
     * the client is initiating a connection with the server.
     */
    public int      ActiveOpens;

    /**
     * Specifies the number of passive opens. In a passive open,
     * the server is listening for a connection request from a client.
     */
    public int      PassiveOpens;

    /**
     * Specifies the number of failed connection attempts.
     */
    public int      AttemptFails;

    /**
     * Specifies the number of established connections that
     * were reset.
     */
    public int      EstabResets;

    /**
     * Specifies the number of currently established connections.
     */
    public int      CurrEstab;

    /**
     * Specifies the number of segments received.
     */
    public int      InSegs;

    /**
     * Specifies the number of segments transmitted. This number does
     * not include retransmitted segments.
     */
    public int      OutSegs;

    /**
     * Specifies the number of segments retransmitted.
     */
    public int      RetransSegs;

    /**
     * Specifies the number of errors received.
     */
    public int      InErrs;

    /**
     * Specifies the number of segments transmitted with
     * the reset flag set.
     */
    public int      OutRsts;

    /**
     * Specifies the number of connections that are currently present
     * in the system. This total number includes connections in all
     * states except listening connections.
     */
    public int      NumConns;

}
