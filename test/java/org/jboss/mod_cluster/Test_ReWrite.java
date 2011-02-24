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

import org.apache.catalina.core.StandardServer;
import org.apache.catalina.LifecycleListener;


public class  Test_ReWrite extends TestCase {

    /* Test that newly created httpd process work ok without sessions */

    String url = "http://localhost:8000/";

    public void test_ReWrite() {
        boolean clienterror = false;
        StandardServer server = Maintest.getServer();
        JBossWeb service = null;
        LifecycleListener cluster = null;

        System.out.println("Test_ReWrite Started");
        try {
            String [] Aliases = new String[1];
            Aliases[0] = "cluster.domain.com";
            service = new JBossWeb("node1",  "localhost", false, "myapp", Aliases);
            service.addConnector(8011);
            service.addConnector(8080, "http");
            server.addService(service);
            cluster = Maintest.createClusterListener("224.0.1.105", 23364, false, null, true, false, true, "secret");
            server.addLifecycleListener(cluster);
        } catch(IOException ex) {
            ex.printStackTrace();
            fail("can't start service");
        }
        // start the server thread.
        ServerThread wait = new ServerThread(3000, server);
        wait.start();

        // Wait until httpd as received the nodes information.
        String [] nodes = new String[1];
        nodes[0] = "node1";
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

        // Start the client
        Client client = new Client();
        client.setVirtualHost("cluster.domain.com");
        try {
            client.runit(url, 100, false, 1);
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Failed: can't start client");
        }
        System.out.println("making \"second\" requests");
        client.start();
        try {
            client.join();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        if (!client.getresultok()) {
            fail("Failed: client failed");
        }

        // Stop the jboss and remove the services.
        try {
            wait.stopit();
            wait.join();

            server.removeService(service);
            server.removeLifecycleListener(cluster);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            fail("can't stop service");
        }

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
        System.gc();
        System.out.println("Test_ReWrite Done");
    }
}
