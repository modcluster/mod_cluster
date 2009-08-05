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
 * Network adater information
 *
 * @author Mladen Turk
 *
 */

public class NetworkAdapter extends NativeObject
{

    private static native long enum0(long pool)
                                throws OutOfMemoryError, OperatingSystemException;
    private static native int  enum1(long handle);
    private static native void enum2(NetworkAdapter thiz, long handle)
                                throws OperatingSystemException;
    private static native void enum3(long handle);

    private NetworkAdapter()
    {
        super(0);
    }

    protected void onDestroy()
    {
        // Nothing
    }

    private void setType(int type)
    {
        Type = NetworkAdapterType.valueOf(type);
    }

    private void setStat(int stat)
    {
        OperationalStatus = IfOperStatus.valueOf(stat);
    }

    /**
     * Returns the array of all enabled Network Adapters
     */
    public static NetworkAdapterIterator getAdapters()
        throws OutOfMemoryError, OperatingSystemException
    {
        NetworkAdapter[] array;
        long handle = enum0(0);
        try {
            int cnt = enum1(handle);
            array = new NetworkAdapter[cnt];
            for (int i = 0; i < cnt; i++) {
                array[i] =new NetworkAdapter();
                enum2(array[i], handle);
            }
            return new NetworkAdapterIterator(array);
        }
        finally {
            // Do not leak memory
            enum3(handle);
        }
    }

    /**
     * Name of the adapter.
     */
    public String               Name;

    /**
     * Description for the adapter.
     */
    public String               Description;

    /**
     * User-friendly name for the adapter. For example:
     * "Local Area Connection 1." This name appears in contexts such
     * as the ipconfig command line program and the Connection folder.
     */
    public String               FriendlyName;

    /**
     * Media Access Control (MAC) of Hardware address for the adapter.
     * This can be null if adapter has no hardware address specified.
     */
    public NetworkAddress       MacAddress;

    /**
     * Type of interface using the values defined by the
     * Internet Assigned Numbers Authority (IANA).
     */
    public NetworkAdapterType   Type;

    /**
     * Operational status of the interface using the values
     * defined in RFC 2863.
     */
    public IfOperStatus         OperationalStatus;

    /**
     * Specifies whether dynamic host configuration protocol (DHCP)
     * is enabled for this adapter.
     */
    public boolean              DhcpEnabled;

    /**
     * Specifies the DHCP server address.
     */
    public NetworkAddress       DhcpServer;

    /**
     * Maximum transmission unit (MTU), in bytes.
     */
    public int                  Mtu;

    /**
     * Array of all configured addresses.
     * All addreses are shown, including duplicate addresses.
     * Such duplicate address entries can occur when addresses are
     * configured statically.
     */
    public NetworkAddress[]     UnicastAddresses;

    /**
     * Array of all configured multicast addresses.
     */
    public NetworkAddress[]     MulticastAddresses;

    /**
     * Array of all configured DNS server addresses.
     */
    public NetworkAddress[]     DnsServerAddresses;

}
