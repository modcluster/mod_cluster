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
 * Enumerated Network address family types.
 */
public enum NetworkAddressFamily
{
    
    /**
     * Let the system decide which address family to use
     */
    UNSPEC(      0),

    /**
     * IPv4 Internet protocols
     */
    INET(        1),

    /**
     * IPv6 Internet protocols
     */
    INET6(       2),

    /**
     * Local communication (pipes, unix domain sockets)
     */
    LOCAL(       3),

    /**
     * Hardware address (MAC).
     * This is Sight specific type.
     */
    HARDWARE(    4);

    private int value;
    private NetworkAddressFamily(int v)
    {
        value = v;
    }

    public int valueOf()
    {
        return value;
    }

    public static NetworkAddressFamily valueOf(int value)
    {
        for (NetworkAddressFamily e : values()) {
            if (e.value == value)
                return e;
        }
        return UNSPEC;
    }

}
