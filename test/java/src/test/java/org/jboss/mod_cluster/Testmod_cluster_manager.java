/*
 *  mod_cluster
 *
 *  Copyright(c) 2009 Red Hat Middleware, LLC,
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
import org.jboss.modcluster.ModClusterService;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardServer;

public class Testmod_cluster_manager extends TestCase {

    /* Test that the sessions are really sticky */
    public void testmod_cluster_manager() {

        boolean clienterror = false;
        StandardServer server = new StandardServer();
        JBossWeb service = null;
        JBossWeb service2 = null;
        ModClusterService cluster = null;

        System.out.println("Testmod_cluster_manager Started");
        try {

            service = new JBossWeb("node1",  "localhost");
            service.addConnector(8011);
            server.addService(service);
 
            service2 = new JBossWeb("node2",  "localhost");
            service2.addConnector(8010);
            server.addService(service2);

            cluster = Maintest.createClusterListener(server, "224.0.1.105", 23364, false, null, true, false, true, "secret");

        } catch(Exception ex) {
            ex.printStackTrace();
            fail("can't start service");
        }

        // start the server thread.
        ServerThread wait = new ServerThread(3000, server);
        wait.start();
         
        // Wait until httpd as received the nodes information.
        String proxy = null;
        int tries = 0;
        while (proxy == null && tries<60) {
            proxy = Maintest.getProxyAddress(cluster);
            if (proxy != null)
                break;
            try {
                Thread me = Thread.currentThread();
                me.sleep(5000);
                tries++;
            } catch (Exception ex) {
            }
        }
        if (tries == 60) {
            fail("can't find proxy");
        }
        ManagerClient managerclient = null;
        try {
              managerclient = new ManagerClient(proxy);
        } catch (Exception ex) {
            fail("can't connect to proxy " + proxy);
        }

        String [] nodes = new String[2];
        nodes[0] = "node1";
        nodes[1] = "node2";
        int countinfo = 0;
        while ((!Maintest.checkProxyInfo(cluster, nodes)) && countinfo < 20) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            countinfo++;
        }

        // Start the client and wait for it.
        Client client = new Client();
        try {
            if (client.runit("/MyCount", 20, true) != 0)
                clienterror = true;
        } catch (Exception ex) {
            ex.printStackTrace();
            clienterror = true;
        }
        if (clienterror)
            fail("Client error");

        // Read the node and DISABLE all applications.
        String node = client.getnode();
        try {
            managerclient.disable(node);
        } catch (Exception ex) {
            fail("Manager Client error");
        }

        // Wait for it.
        try {
            client.start();
            client.join();
        } catch (Exception ex) {
            ex.printStackTrace();
            clienterror = true;
        }
        if (!client.getresultok())
            fail("Failed disabled node should process request with sessionid");

        // Now start a new client it should use the other node.
        // Start the client and wait for it.
        client = new Client();
        try {
            if (client.runit("/MyCount", 20, true) != 0)
                clienterror = true;
        } catch (Exception ex) {
            ex.printStackTrace();
            clienterror = true;
        }
        if (clienterror)
            fail("Client error");

        // Read the node and DISABLE all applications.
        String newnode = client.getnode();
        if (newnode.compareTo(node) == 0)
            fail("Disabled node shouldn't accept request with sessionid");

        // Wait for it.
        try {
            client.start();
            client.join();
        } catch (Exception ex) {
            ex.printStackTrace();
            clienterror = true;
        }
        if (client.getresultok())
            System.out.println("Test Done");
        else {
            System.out.println("Test FAILED");
            clienterror = true;
        }

        // Stop the jboss and remove the services.
        try {
            wait.stopit();
            wait.join();

            server.removeService(service);
            server.removeService(service2);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            fail("can't stop service");
        }
        if (clienterror)
            fail("Client error");

        // Wait until httpd as received the stop messages.
        countinfo = 0;
        nodes = null;
        while ((!Maintest.checkProxyInfo(cluster, nodes)) && countinfo < 20) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            countinfo++;
        }
        Maintest.StopClusterListener();
        System.gc();
        System.out.println("Testmod_cluster_manager Done");
    }
}
