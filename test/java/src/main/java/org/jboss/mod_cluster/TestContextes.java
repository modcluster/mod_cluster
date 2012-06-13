/*
 *  mod_cluster
 *
 *  Copyright(c) 2011 Red Hat Middleware, LLC,
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
import org.apache.catalina.Service;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardServer;

import org.jboss.modcluster.ModClusterService;

public class TestContextes extends TestCase {

    /* Test that 150 contexts are created and working... */
    public void testContextes() {

        boolean clienterror = false;
        StandardServer server = new StandardServer();
        JBossWeb service = null;
        JBossWeb service2 = null;
        Connector connector = null;
        Connector connector2 = null;
        ModClusterService cluster = null;
        System.out.println("TestContextes Started");
        try {

            service = new JBossWeb("node3",  "localhost", false, "bad_one");
            connector = service.addConnector(8013);
            for (int i=0; i<149; i++)
            service.AddContext("/test" + i , "/test" + i, "MyCount", false);
            server.addService(service);

            cluster = Maintest.createClusterListener(server, "224.0.1.105", 23364, false, "dom1", true, false, true, null);

        } catch(Exception ex) {
            ex.printStackTrace();
            fail("can't start service");
        }

        // start the server thread.
        ServerThread wait = new ServerThread(3000, server);
        wait.start();

        // Wait until 2 nodes are created in httpd.
        String [] nodes = new String[1];
        nodes[0] = "node3";
        int countinfo = 0;
        while ((!Maintest.checkProxyInfo(cluster, nodes)) && countinfo < 20) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            countinfo++;
        }
        if (countinfo == 20)
            fail("can't find node in httpd");

        // Test each context.
        for (int i=0; i<149; i++) {
            Client client = new Client();

            // Wait for it.
            try {
                if (client.runit("/test" + i + "/MyCount", 10, false, true) != 0)
                    clienterror = true;
            } catch (Exception ex) {
                ex.printStackTrace();
                clienterror = true;
            }
            if (clienterror)
                fail("Client failed (" + i + ")");
        }

        // Stop the server or services.
        try {
            wait.stopit();
            wait.join();
            server.removeService(service);
            server.removeService(service2);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        // Wait until httpd as received the stop messages.
        Maintest.TestForNodes(cluster, null);
        Maintest.StopClusterListener();

        // Test client result.
        if (clienterror)
            fail("Client test failed");

        Maintest.testPort(8013);
        Maintest.testPort(8014);
        System.out.println("TestAliases Done");
    }
}
