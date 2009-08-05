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

import junit.framework.Assert;
import junit.framework.TestCase;


/**
 * Memory Leak Test.
 *
 */
public class MemoryLeakTest extends TestCase
{

    private static long created   = 0;
    private static long destroyed = 0;

    class NativeObjectTestClass extends NativeObject
    {
        NativeObjectTestClass()
        {
            super(0);
            created++;
        }

        // Called from the native when the
        // object is destroyed.
        protected void onDestroy()
        {
            destroyed++;
        }

    }

    class NativeObjectRunner extends Thread
    {
        NativeObjectTestClass no;

        NativeObjectRunner(NativeObjectTestClass no)
        {
            this.no = no;
        }

        public void run() {
            try {
                // Sleep 100 * 10 ms
                no.sleep(100, 10000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class NativeObjectCleaner extends Thread
    {
        NativeObjectTestClass[] no;

        NativeObjectCleaner(NativeObjectTestClass[] no)
        {
            this.no = no;
        }

        public void run() {
            try {
                for (int i = 0; i < no.length; i++) {
                    if (no[i] != null)
                        no[i].clear();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(MemoryLeakTest.class);
    }

    protected void setUp()
        throws Exception
    {
        Library.initialize("");
    }

    protected void tearDown()
        throws Exception
    {
        Library.shutdown();
    }

    public void testNativeObject0()
        throws Exception
    {

        for (int i = 0; i < 500000; i++) {
            new NativeObjectTestClass();
        }
        // Library.clear() will force that all native objects
        // gets destroyed.
        Library.clear();
        assertEquals(created, destroyed);
    }

    public void testNativeCalls1()
        throws Exception
    {

        for (int i = 0; i < 1000; i++) {
            new NativeObjectRunner(new NativeObjectTestClass()).start();
        }
        // Library.clear() will force that all native objects
        // gets destroyed.
        Library.clear();
        assertEquals(created, destroyed);
    }

    public void testNativeCalls2()
        throws Exception
    {
        int i;
        NativeObjectTestClass[] no = new NativeObjectTestClass[1000];
        for (i = 0; i < 1000; i++) {
            no[i] = new NativeObjectTestClass();
        }
        for (i = 0; i < 1000; i++) {
            new NativeObjectRunner(no[i]).start();
        }
        for (i = 0; i < 1000; i++) {
            no[i].clear();
        }

        // Library.clear() will force that all native objects
        // gets destroyed.
        Library.clear();
        assertEquals(created, destroyed);
    }

    public void testNativeCalls3()
        throws Exception
    {
        int i;
        NativeObjectTestClass[] no = new NativeObjectTestClass[1000];
        for (i = 0; i < 1000; i++) {
            no[i] = new NativeObjectTestClass();
        }
        for (i = 0; i < 1000; i++) {
            new NativeObjectRunner(no[i]).start();
        }
        for (i = 0; i < 10; i++) {
            new NativeObjectCleaner(no).start();
        }
        // Library.clear() will force that all native objects
        // gets destroyed.
        Library.clear();
        assertEquals(created, destroyed);
        for (i = 0; i < 1000; i++) {
            no[i] = null;
        }
        System.gc();
        Thread.sleep(1000);
        System.gc();
    }

}
