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
 * Process Test.
 *
 */
public class ProcessTest extends TestCase
{

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(ProcessTest.class);
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

    public void testProcessSelfObject()
        throws Exception
    {
        Process[] self = new Process[1000];
        for (int i = 0; i < 1000; i++) {
            self[i] = Process.self();
        }
        for (int i = 0; i < 1000; i++) {            
            for (int j = 0; j < 1000; j++) {
                self[j].refresh();
            }
        }
        for (int i = 0; i < 1000; i++) {
            self[i].destroy();
        }
        // Library.clear() will force that all native objects
        // gets destroyed.
        Library.clear();
    }

    public void testProcessList()
        throws Exception
    {
        for (int i = 0; i < 100; i++) {
            ProcessIterator list = Process.getProcesses();
            for (Process p : list) {
                User u  = new User(p.UserId);
                Group g = new Group(p.GroupId);
            }
            for (Process p : list) {
                p.refresh();
            }
            for (Process p : list) {
                p.destroy();
            }
            for (Process p : list) {
                p.refresh();
                User u  = new User(p.UserId);
                Group g = new Group(p.GroupId);
            }
        }
        // Library.clear() will force that all native objects
        // gets destroyed.
        Library.clear();
    }

}
