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
 * Network informations
 *
 * @author Mladen Turk
 *
 */

public class Network extends NativeObject
{

    private static native void info0(Network thiz, long pool)
                                throws OutOfMemoryError, OperatingSystemException;

    public Network()
        throws OutOfMemoryError, OperatingSystemException
    {
        super(0);
        info0(this, POOL);
    }

    protected void onDestroy()
    {
        // Nothing
    }


    /**
     * This box host name.
     */
    public String               HostName;

    /**
     * Domain name.
     */
    public String               DomainName;

    /**
     * Array of all configured DNS server addresses.
     */
    public NetworkAddress[]     DnsServerAddresses;

}
