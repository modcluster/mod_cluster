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
 * Process Mutext Test.
 *
 */
public class MutexTest extends TestCase
{

    int counter = 0;

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(MutexTest.class);
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

    class MutexTestClient extends Thread
    {
        GlobalMutex m;

        MutexTestClient()
            throws Exception
        {
            m = new GlobalMutex("./ProcessMutex.lock", MutexType.DEFAULT);
        }

        public void run() {
            /* First one should be locked */
            int rv = m.tryLock();

            if (rv != Error.APR_ENOTIMPL) {
                // Must unixes are missing trylock
                assertTrue(Status.APR_STATUS_IS_EBUSY(rv));
            }
            try {
                rv = m.lock();
                assertEquals(0, counter);
                assertEquals(Error.APR_SUCCESS, rv);
                rv = m.unlock();
                assertEquals(Error.APR_SUCCESS, rv);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public void testMutex()
        throws Exception
    {
        GlobalMutex m = new GlobalMutex("./ProcessMutex.lock", MutexType.DEFAULT);

        m.lock();
        new MutexTestClient().start();
        /* Settle up */
        Thread.sleep(1000);
        m.unlock();
        for (; counter < 10; counter++) {
            m.lock();
            Thread.sleep(10);
            m.unlock();
        }
        assertNotNull(m.getName());

        m.destroy();
    }

}
