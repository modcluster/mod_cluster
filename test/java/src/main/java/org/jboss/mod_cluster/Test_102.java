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
import org.jboss.modcluster.ModClusterService;


public class  Test_102 extends TestCase {

    /* Test that newly created httpd process work ok without sessions */

    String url = "http://localhost:8000/ROOT/MyTest";

    public void test_102() {
        boolean clienterror = false;
        StandardServer server = Maintest.getServer();
        JBossWeb service = null;
        ModClusterService cluster = null;

        System.out.println("Test_102 Started");
        try {
            service = new JBossWeb("node1",  "localhost");
            service.addConnector(8011);
            server.addService(service);
            cluster = Maintest.createClusterListener("224.0.1.105", 23364, false, null, true, false, true, "secret");
        } catch(Exception ex) {
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

        // Start a bunch of clients.
        Client client[] = new Client[200];
        for (int i=0; i<client.length; i++) {
            client[i] = new Client();  
            try {
                client[i].runit(url, 2, false, 1);
            } catch (Exception ex) {
                ex.printStackTrace();
                fail("Failed: can't start client");
            }
        }
        System.out.println("making \"second\" requests");
        for (int i=0; i<client.length; i++) {
            client[i].start();
        }
        for (int i=0; i<client.length; i++) {
            try {
                client[i].join();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        for (int i=0; i<client.length; i++) {
            if (!client[i].getresultok()) {
                fail("Failed: at least one client failed");
            }
        }

        // Stop the jboss and remove the services.
        try {
            wait.stopit();
            wait.join();

            server.removeService(service);
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
        Maintest.StopClusterListener();
        System.gc();
        System.out.println("Test_102 Done");
    }
}

