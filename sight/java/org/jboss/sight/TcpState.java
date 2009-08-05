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
 * Determines the state of TCP connection
 */
public enum TcpState
{

    UNKNOWN(        0),
    CLOSED(         1),
    LISTENING(      2),
    SYN_SENT(       3),
    SYN_RCVD(       4),
    ESTABLISHED(    5),
    FIN_WAIT1(      6),
    FIN_WAIT2(      7),
    CLOSE_WAIT(     8),
    CLOSING(        9),
    LAST_ACK(      10),
    TIME_WAIT(     11),
    DELETE_TCB(    12);

    private int value;
    private TcpState(int v)
    {
        value = v;
    }

    public int valueOf()
    {
        return value;
    }

    public static TcpState valueOf(int value)
    {
        for (TcpState e : values()) {
            if (e.value == value)
                return e;
        }
        return UNKNOWN;
    }

}
