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


public class  Test_ReWrite extends TestCase {

    int MAXSTOPCOUNT = 200;

    /* Test that newly created httpd process work ok without sessions */

    public void test_ReWrite() {
        boolean clienterror = false;
        StandardServer server = new StandardServer();
        JBossWeb service = null;
        ModClusterService cluster = null;

        System.out.println("Test_ReWrite Started");
        System.setProperty("org.apache.catalina.core.StandardService.DELAY_CONNECTOR_STARTUP", "false");
        try {
            String [] Aliases = new String[3];
            Aliases[0] = "cluster.domain.com";
            Aliases[1] = "cluster.domain.org";
            Aliases[2] = "cluster.domain.net";
            service = new JBossWeb("node1",  "localhost", false, "myapp", Aliases);
            service.addConnector(8011);
            service.addConnector(8080, "http");
            server.addService(service);
            cluster = Maintest.createClusterListener(server, "224.0.1.105", 23364, false, null, true, false, true, "secret");
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

        /*
         * Check for Apache httpd
         */
        try {
            String proxy = Maintest.getProxyAddress(cluster);
            ManagerClient managerclient = new ManagerClient(proxy);
            if (!managerclient.isApacheHttpd()) {
                stop(wait, server, service, cluster);
                System.gc();
                System.out.println("Test_ReWrite Skipped");
                return;
            }
        } catch (Exception ex) {
                ex.printStackTrace();
                fail("Can't check proxy type");
        }

        String proxyinfo = Maintest.getProxyInfo(cluster);
        System.out.println(proxyinfo);

        // Test RewriteRule ^/$ /myapp/MyCount [PT]
        Client client = new Client();
        client.setVirtualHost("cluster.domain.com");
        try {
            String url = "http://localhost:8000/";
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

        // Test RewriteRule ^/(.*)$ balancer://mycluster/myapp/$1 [L,PT]
        client = new Client();
        client.setVirtualHost("cluster.domain.org");
        try {
            String url = "http://localhost:8000/MyCount";
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

        // Test RewriteRule ^/$ balancer://mycluster/ [L,P]
        client = new Client();
        client.setVirtualHost("cluster.domain.net");
        try {
            String url = "http://localhost:8000/test/MyCount";
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

        stop(wait, server, service, cluster);
        System.gc();
        System.out.println("Test_ReWrite Done");
    }

    private void stop(ServerThread wait, StandardServer server, JBossWeb service, ModClusterService cluster) {
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
        int countinfo = 0;
        String [] nodes = null;
        while ((!Maintest.checkProxyInfo(cluster, nodes)) && countinfo < MAXSTOPCOUNT) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            countinfo++;
        }
        Maintest.StopClusterListener();
        if (countinfo == MAXSTOPCOUNT)
            fail("node doesn't dispair");
    }
}
