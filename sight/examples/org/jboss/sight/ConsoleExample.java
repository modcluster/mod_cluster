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
 * Console example
 *
 * @author Mladen Turk
 */

public class ConsoleExample {


    private class ConsoleWorker extends Thread
    {

        private Console  con;
        private String   prefix;

        public ConsoleWorker(String prefix)
        {
            this.prefix = prefix;
        }

        public void run() {
            try {
                con = Console.acquire(0);
                con.setEcho(true);
                con.setTitle("SIGHT Console example");
                System.out.print(prefix + "with echo: ");
                String s1 = con.readln();
                con.println(prefix + "readed (" + s1 + ")");
                con.setEcho(false);
                con.print(prefix + "without echo: ");
                String s2 = con.readln();
                // New line because we were without echo
                con.println();
                con.println(prefix + "readed (" + s2 + ")");
                System.out.println(prefix + "done");

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    con.release();
                } catch (Exception x) {
                    // Skip
                }
            }

        }


    }

    public ConsoleExample()
        throws InterruptedException
    {
        ConsoleWorker w1 = new ConsoleWorker("Service 1 ");
        ConsoleWorker w2 = new ConsoleWorker("Service 2 ");
        try {
            Console.ATTACH_TO_DESKTOP = true;

            Console  con = Console.acquire();
            w1.start();
            w2.start();
            con.dumpAllStacks();
            con.release();
            w1.join();
            w2.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String [] args) {
        try {
            Library.initialize("");

            ConsoleExample wc = new ConsoleExample();
            System.out.print("Shutting down the Sight library ... ");
            Library.shutdown();
            System.out.println("OK");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
 }
