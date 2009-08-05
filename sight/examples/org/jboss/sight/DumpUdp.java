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
import java.util.Date;
/**
 * Dump Udp example
 *
 * @author Mladen Turk
 */

public class DumpUdp {


    public DumpUdp()
    {
        try {
            UdpStatistics udp = new UdpStatistics(NetworkAddressFamily.INET);
            System.out.println("IPV4 UdpStatistics");
            System.out.println("    InDatagrams  :" + udp.InDatagrams);
            System.out.println("    OutDatagrams :" + udp.OutDatagrams);
            System.out.println("    NumAddrs     :" + udp.NumAddrs);
            for (UdpConnection c : udp.getUdpConnections()) {
                System.out.println(new Date(c.CreateTimestamp) + " " + c.Pid + "  \t" +
                                   c.LocalAddr.Address + ":" + c.LocalAddr.Port + "  \t" +
                                   c.RemoteAddr.Address + ":" + c.RemoteAddr.Port);

            }

            udp = new UdpStatistics(NetworkAddressFamily.INET6);
            System.out.println();
            System.out.println("IPV6 UdpStatistics");
            System.out.println("    InDatagrams  :" + udp.InDatagrams);
            System.out.println("    OutDatagrams :" + udp.OutDatagrams);
            System.out.println("    NumAddrs     :" + udp.NumAddrs);
            for (UdpConnection c : udp.getUdpConnections()) {
                System.out.println("[" + c.LocalAddr.Address + "]:" + c.LocalAddr.Port);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String [] args) {
        try {
            Library.initialize("");
            DumpUdp dmp = new DumpUdp();
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
