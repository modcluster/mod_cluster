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
import java.net.ServerSocket;

import junit.framework.TestCase;

import org.apache.catalina.Engine;
import org.apache.catalina.Service;
import org.jboss.modcluster.ModClusterService;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardServer;

public class TestPing extends TestCase {

    /* Test that the sessions are really sticky */
    public void testPing() {

        boolean clienterror = false;
        StandardServer server = new StandardServer();
        JBossWeb service = null;
        JBossWeb service2 = null;
        ModClusterService cluster = null;

        System.out.println("Testping Started");
        System.setProperty("org.apache.catalina.core.StandardService.DELAY_CONNECTOR_STARTUP", "false");
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
        int tries = Maintest.WaitForHttpd(cluster, 60);
        if (tries == -1) {
            fail("can't find PING-RSP in proxy response");
        }
        if (tries == 60) {
            fail("can't find proxy");
        }

        // PING the 2 nodes...
        String [] nodes = new String[2];
        nodes[0] = "node1";
        nodes[1] = "node2";
        int countinfo = 0;
        while (countinfo < 20) {
            int i;
            for (i=0; i< nodes.length; i++) {
                String result = Maintest.doProxyPing(cluster, nodes[i]);
                if (result == null)
                    fail("Maintest.doProxyPing failed");
                if (!Maintest.checkProxyPing(result))
                    break;
            }
            if (i == nodes.length)
                break;
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            countinfo++;
        }
        if (countinfo == 20)
            fail("can't find node(s) PING-RSP in proxy response");
        // Try a not existing node.
        String result = Maintest.doProxyPing(cluster, "NONE");
        if (result == null)
           fail("Maintest.doProxyPing failed");
        if (Maintest.checkProxyPing(result))
           fail("doProxyPing on not existing node should have failed");

        // Get the connection back.
        tries = Maintest.WaitForHttpd(cluster, 20);
        if (tries == -1) {
            fail("can't find PING-RSP in proxy response");
        }
        if (tries == 20) {
            fail("can't find proxy");
        }

        // Ping using url
        result = Maintest.doProxyPing(cluster, "ajp", "localhost", 8011);
        if (result == null)
           fail("Maintest.doProxyPing failed");
        if (!Maintest.checkProxyPing(result))
           fail("doProxyPing on " + "ajp://localhost:8011" + " have failed");
        // Try a not existing node.
        result = Maintest.doProxyPing(cluster, "ajp", "localhost", 8012);
        if (result == null)
           fail("Maintest.doProxyPing failed");
        if (Maintest.checkProxyPing(result))
           fail("doProxyPing on " + "ajp://localhost:8012" + " should have failed");

        /*
         * Untertow doesn't support yet (2014/07/15) to ping an ajp URL
         */
        try {
            String proxy = Maintest.getProxyAddress(cluster);
            ManagerClient managerclient = new ManagerClient(proxy);
            if (!managerclient.isApacheHttpd()) {
                stop(wait, server, service, service2, cluster);
                System.gc();
                System.out.println("Test_ReWrite Skipped");
                return;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Can't check proxy type");
        }

        // Try a timeout node.
        try {
        ServerSocket sock = new ServerSocket(8012);
        } catch (Exception ex) {
            fail("can't create ServerSocket on 8012");
        }
        tries = Maintest.WaitForHttpd(cluster, 20);
        if (tries == -1) {
            fail("can't find PING-RSP in proxy response");
        }
        if (tries == 20) {
            fail("can't find proxy");
        }
        result = Maintest.doProxyPing(cluster, "ajp", "localhost", 8012);
        if (result == null)
           fail("Maintest.doProxyPing failed");
        if (Maintest.checkProxyPing(result))
           fail("doProxyPing on " + "ajp://localhost:8012" + " should have failed");

        stop(wait, server, service, service2, cluster);
        System.gc();
        System.out.println("TestPing Done");
    }

    private void stop(ServerThread wait, StandardServer server, JBossWeb service, JBossWeb service2, ModClusterService cluster) {
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

        // Wait until httpd as received the stop messages.
        int countinfo = 0;
        String [] nodes = null;
        while ((!Maintest.checkProxyInfo(cluster, nodes)) && countinfo < 20) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            countinfo++;
        }
        Maintest.StopClusterListener();
    }
}
