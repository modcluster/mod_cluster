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

import java.util.EnumSet;
import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Directory Test.
 *
 */
public class DirectoryTest extends TestCase
{

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(DirectoryTest.class);
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

    public void testDirectoryCreate()
        throws Exception
    {
        int rc;
        rc = Directory.make("foo", FileProtection.OS_DEFAULT);
        assertEquals(Error.APR_SUCCESS, rc);
        rc = Directory.makeRecursive("bar/foo", FileProtection.OS_DEFAULT);
        assertEquals(Error.APR_SUCCESS, rc);

        FileInfo fi;
        Directory dir = new Directory("bar");
        DirectoryIterator di = dir.getContent(EnumSet.of(FileInfoFlags.DIRENT));
        fi = di.next();
        assertEquals(".",   fi.BaseName);
        fi = di.next();
        assertEquals("..",  fi.BaseName);
        fi = di.next();
        assertEquals("foo", fi.BaseName);
        dir.close();

        rc = Directory.remove("bar/foo");
        assertEquals(Error.APR_SUCCESS, rc);
        rc = Directory.remove("bar");
        assertEquals(Error.APR_SUCCESS, rc);
        rc = Directory.remove("foo");
        assertEquals(Error.APR_SUCCESS, rc);
    }

}
