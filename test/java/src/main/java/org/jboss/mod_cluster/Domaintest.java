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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import java.lang.Exception;
import java.net.Socket;
import java.io.PrintWriter;
import java.io.InputStream;

public class Domaintest extends TestCase {

    public static void main( String args[] ) {
        TestRunner.run(suite());
    }
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new TestSuite(Domaintest.class));
        return suite;
    }

    public void testDomain() {
        boolean clienterror = false;
        String node;
        int stopport = 0;
        // The test expects 4 JBossWEB started on node1 to node4 and port 8005 to 8005.
        System.out.println("Running Domain tests");

        // Wait for the JBossWEB to start
        try {
            Thread.sleep(25000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        Client client = new Client();
        try {
            client.runit("/ROOT/MyCount", 10, false, true);
        } catch (Exception ex) {
            ex.printStackTrace();
            clienterror = true;
        }

        node = client.getnode();
        if (node == null)
            clienterror = true;

        if (clienterror) {
           Util.stopAll();
           fail("Client error");
        }

        if (node.equals("node1")) {
           node = "node2";
           stopport = 8005;
        } else if (node.equals("node2")) {
           node = "node1";
           stopport = 8006;
        } else if (node.equals("node3")) {
           node = "node4";
           stopport = 8007;
        } else if (node.equals("node4")) {
           node = "node3";
           stopport = 8008;
        }
        client.setnode(node);
        Util.stopNode(stopport);
        try {
            client.start();
            client.join();
        } catch (Exception ex) {
            ex.printStackTrace();
            clienterror = true;
        }

        if (client.getresultok())
            System.out.println("Test DONE");
        else {
            System.out.println("Test FAILED");
            clienterror = true;
        }
        Util.stopAll();
 
    }
}
