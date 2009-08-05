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
 * Network address information
 *
 * @author Mladen Turk
 *
 */

public final class NetworkAddress
{

    private NetworkAddress()
    {
        // Nothing
    }

    private void setFamily(int family)
    {
        Family = NetworkAddressFamily.valueOf(family);
    }

    /**
     * The hostname.
     */
    public String               HostName;

    /**
     * Either a string of the port number or the service name
     * for the port.
     */
    public String               ServiceName;

    /**
     * Specifies the string representation of
     * the network address.
     */
    public String               Address;

    /**
     * IP address mask.
     */
    public String               Mask;

    /**
     * Lease lifetime for the address, in seconds.
     */
    public long                 LeaseLifetime;

    /**
     * Port
     */
    public int                  Port;

    /**
     * Is this IPV6 Address
     */
    public NetworkAddressFamily Family;


}
