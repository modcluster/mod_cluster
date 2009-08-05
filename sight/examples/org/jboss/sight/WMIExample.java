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

import org.jboss.sight.platform.windows.*;

/**
 * WMI example
 *
 * @author Mladen Turk
 */

public class WMIExample {


    private class Worker extends Thread
    {

        private WMIQuery wmi;
        private String   prefix;
        public Worker(String prefix)
        {
            this.prefix = prefix;
            try {
                wmi = new WMIQuery();

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        public void run() {
            try {

                wmi.ExecQuery("WQL", "SELECT * FROM Win32_Service");
                while (wmi.Next()) {
                    VARIANT name = wmi.Get("Name");
                    VARIANT started = wmi.Get("Started");
                    System.out.println(prefix + name + " (" + started + ")");

                }
                wmi.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }

    public WMIExample()
    {
        Worker w1 = new Worker("Service : ");
        w1.start();
    }

    public static void main(String [] args) {
        try {
            Library.initialize("");

            WMIExample wc = new WMIExample();
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
