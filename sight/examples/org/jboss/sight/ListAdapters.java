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
 * List users example
 *
 * @author Mladen Turk
 */

public class ListAdapters {


    public ListAdapters()
    {
        try {
            Network net = new Network();
            System.out.println("Server : " + net.HostName);
            System.out.println("         " + net.DomainName);
            System.out.println("         DNS Servers:");
            if (net.DnsServerAddresses != null) {
                for (int i = 0; i <  net.DnsServerAddresses.length; i++) {
                    System.out.println("             " + net.DnsServerAddresses[i].Address);
                }
            }
            System.out.println("Adapters");
            for (NetworkAdapter a : NetworkAdapter.getAdapters()) {
                System.out.println("Adapter: " + a.FriendlyName);
                System.out.println("         " + a.Name);
                System.out.println("         " + a.Description);
                System.out.println("         " + a.Type);
                System.out.println("         " + a.OperationalStatus);
                if (a.MacAddress != null)
                System.out.println("         " + a.MacAddress.Address);
                System.out.println("         Dhcp Enabled: " + a.DhcpEnabled);
                if (a.DhcpServer != null)
                System.out.println("         Dhcp Server : " + a.DhcpServer.Address);
                System.out.println("         Unicast Addresses:");
                if (a.UnicastAddresses != null) {
                    for (int i = 0; i <  a.UnicastAddresses.length; i++) {
                        if (a.UnicastAddresses[i].Family == NetworkAddressFamily.INET6)
                            System.out.println("             " + a.UnicastAddresses[i].Address +
                                     " (" + a.UnicastAddresses[i].LeaseLifetime + ")");
                        else
                            System.out.println("             " + a.UnicastAddresses[i].Address +
                                     " / " + a.UnicastAddresses[i].Mask +
                                     " (" + a.UnicastAddresses[i].LeaseLifetime + ")");
                    }
                }
                System.out.println("         Multicast Addresses:");
                if (a.MulticastAddresses != null) {
                    for (int i = 0; i <  a.MulticastAddresses.length; i++) {
                        System.out.println("             " + a.MulticastAddresses[i].Address);
                    }
                }
                System.out.println("         DNS Servers:");
                if (a.DnsServerAddresses != null) {
                    for (int i = 0; i <  a.DnsServerAddresses.length; i++) {
                        System.out.println("             " + a.DnsServerAddresses[i].Address);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String [] args) {
        try {
            Library.initialize("");
            ListAdapters ld = new ListAdapters();
            System.out.println("1");
            System.gc();
            Thread.sleep(1000);
            System.out.println("2");
            Library.shutdown();
            System.out.println("3");
            Thread.sleep(1000);
            System.out.println("4");
            System.gc();
            Thread.sleep(1000);
            System.out.println("5");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
 }
