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

import junit.framework.TestCase;

import org.apache.catalina.Engine;
import org.apache.catalina.ServerFactory;
import org.apache.catalina.Service;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardServer;

public class Test_196 extends TestCase {

    StandardServer server = null;

    /* Test failAppover */
    public void test_196() {

        boolean clienterror = false;
        server = Maintest.getServer();
        JBossWeb service = null;
        JBossWeb service2 = null;
        Connector connector = null;
        Connector connector2 = null;
        LifecycleListener cluster = null;
        System.out.println("Test_196 Started");
        try {
            // server = (StandardServer) ServerFactory.getServer();

            service = new JBossWeb("node3",  "localhost", false, "test");
            connector = service.addConnector(8013);
            service.AddContext("/abc", "/abc");
            server.addService(service);

            service2 = new JBossWeb("node4",  "localhost", false, "test");
            connector2 = service2.addConnector(8014);
            service2.AddContext("/abc/xyz", "/abc/xyz", "MyCount", false);
            server.addService(service2);

            cluster = Maintest.createClusterListener("224.0.1.105", 23364, false, "dom1", true, false, true, "secret");
            server.addLifecycleListener(cluster);
            // Maintest.listServices();

        } catch(IOException ex) {
            ex.printStackTrace();
            fail("can't start service");
        }

        // start the server thread.
        ServerThread wait = new ServerThread(3000, server);
        wait.start();

        // Wait until httpd answers to ping and the 2 nodes are up.
        String [] nodes = new String[2];
        nodes[0] = "node3";
        nodes[1] = "node4";
        if (!Maintest.WaitForNodes(cluster, nodes)) {
            fail("can't start nodes");
        }

        // Start the client and wait for it.
        Client client = new Client();

        // Wait for it.
        try {
            if (client.runit("/abc/xyz/MyCount", 10, false, true) != 0)
                clienterror = true;
        } catch (Exception ex) {
            ex.printStackTrace();
            clienterror = true;
        }
        if (clienterror)
            fail("Client error");

        // Stop the connector that has received the request...
        String node = client.getnode();
        if (!"node4".equals(node))
            fail("Request processed by the wrong node");

        // Finish the test...
        try {
            client.setdelay(30000);
            client.start();
            client.join();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (client.getresultok())
            System.out.println("Test DONE");
        else {
            System.out.println("Test FAILED");
            clienterror = true;
        }

        // Stop the server or services.
        try {
            wait.stopit();
            wait.join();
            server.removeService(service);
            server.removeService(service2);
            server.removeLifecycleListener(cluster);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        // Wait until httpd as received the stop messages.
        Maintest.TestForNodes(cluster, null);

        // Test client result.
        if (clienterror)
            fail("Client test failed");

        Maintest.testPort(8013);
        Maintest.testPort(8014);
        System.out.println("Test_196 Done");
    }
}
