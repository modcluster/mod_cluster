/*
 *  mod_cluster
 *
 *  Copyright(c) 2008 Red Hat Middleware, LLC,
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
 * @author Jean-Frederic Clere
 * @version $Revision$
 */

package org.jboss.mod_cluster;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayInputStream;

import junit.framework.TestCase;

import org.apache.catalina.Engine;
import org.apache.catalina.Service;
import org.jboss.modcluster.ModClusterService;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardServer;

public class Test_Chunk_JBWEB_117 extends TestCase {

    /* Test for JBWEB-117 */
    public void test_Chunk_JBWEB_117() {
        byte [] buf = new byte[1024*6];
        for (int i=0; i<buf.length;i++)
            buf[i] = 'a';
        ByteArrayInputStream fd = new ByteArrayInputStream(buf);
        String result = ClientBasicAuthen.test(false, fd);
        if (result != null)
            fail(result);
    }
}
