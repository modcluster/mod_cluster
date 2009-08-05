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
 * TcpConnection information
 *
 * @author Mladen Turk
 *
 */

public final class TcpConnection
{

    protected TcpConnection()
    {
        // Nothing
    }

    private void setState(int state)
    {
        State = TcpState.valueOf(state);
    }

    /**
     * Specifies the address for the connection on the local computer.
     * A value of zero indicates the listener can accept a connection
     * on any interface.
     */
    public NetworkAddress       LocalAddr;

    /**
     * Specifies the address for the connection on the remote computer.
     */
    public NetworkAddress       RemoteAddr;

    /**
     * Specifies the state of the TCP connection
     */
    public TcpState             State;

    /**
     * The PID of the process that issued a context bind for this
     * TCP link.
     */
    public int                  Pid;

    /**
     * System time value which indicates when the context
     * bind operation that created this endpoint occurred.
     */
    public long                 CreateTimestamp;

    /**
     * Connection timeout value in miliseconds for the
     * current connection State. 
     */
    public int                  Timeout;

}
