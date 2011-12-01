/*
 *  mod_cluster
 *
 *  Copyright(c) 2010 Red Hat Middleware, LLC,
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

public class TestPingTimeout extends TestCase {

    StandardServer server = null;

    /* Test Context handling:
     * / 
     * /test
     * /testtest
     * Using the servlet MyTest (in ROOT).
     */
    public void testPingTimeout() {

        boolean clienterror = false;
        server = Maintest.getServer();
        JBossWeb service = null;
        Connector connector = null;
        LifecycleListener cluster = null;
        System.out.println("TestPingTimeout Started");
        try {
            service = new JBossWeb("node3",  "localhost", false, "ROOT");
            connector = service.addConnector(8013);
            service.AddContext("/test", "/test");
            server.addService(service);

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

        // Wait until we are able to connect to httpd.
        int tries = Maintest.WaitForHttpd(cluster, 60);
        if (tries == -1) {
            fail("can't find PING-RSP in proxy response");
        }

        // Wait until the node is created in httpd.
        String [] nodes = new String[1];
        nodes[0] = "node3";
        Maintest.TestForNodes(cluster, nodes);

        // Start the client and wait for it.
        Client client = new Client();

        // Do a request.
        try {
            if (client.runit("/test/MyTest?test=timeout", 1, false, false) != 0)
                clienterror = true;
        } catch (Exception ex) {
            ex.printStackTrace();
            clienterror = true;
        }
        if (clienterror)
            fail("Client error");

        // Stop the server or services.
        try {
            wait.stopit();
            wait.join();
            server.removeService(service);
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
        System.out.println("TestPingTimeout Done");
    }
}
