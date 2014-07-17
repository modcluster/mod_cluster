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

import junit.framework.TestCase;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardServer;
import org.jboss.modcluster.ModClusterService;

public class TestQuery extends TestCase {

    int MAXSTOPCOUNT = 200;

    /* Test Context handling:
     * / 
     * /test
     * /testtest
     * Using the servlet MyTest (in ROOT).
     */
    public void testQuery() {

        boolean clienterror = false;
        StandardServer server = new StandardServer();
        JBossWeb service = null;
        Connector connector = null;
        ModClusterService cluster = null;
        System.out.println("TestQuery Started");
        System.setProperty("org.apache.catalina.core.StandardService.DELAY_CONNECTOR_STARTUP", "false");
        try {
            String [] Aliases = new String[1];
            Aliases[0] = "cluster.domain.info";
            service = new JBossWeb("node3",  "localhost", false, "ROOT", Aliases);
            connector = service.addConnector(8013);
            service.AddContext("/test", "/test", "MyTest", false);
            service.AddContext("/testtest", "/testtest", "MyTest", false);
            server.addService(service);

            cluster = Maintest.createClusterListener(server, "224.0.1.105", 23364, false, "dom1", true, false, true, "secret");

        } catch(Exception ex) {
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
        if (!Maintest.TestForNodes(cluster, nodes))
            fail("can't start nodes");

        // Start the client and wait for it.
        Client client = new Client();

        // Do a request.
        try {
            if (client.runit("/test/MyTest?name=edwin&state=NY", 1, false, false) != 0)
                clienterror = true;
        } catch (Exception ex) {
            ex.printStackTrace();
            clienterror = true;
        }
        if (clienterror)
            fail("Client error");

        // Check for the result.
        String response = client.getResponse();
        if (response.indexOf("name=edwin&state=NY") == -1) {
            System.out.println("response: " + client.getResponse());
            fail("Can't find the query string in the response");
        }

                /*
         * Check for Apache httpd
         */
        try {
            String proxy = Maintest.getProxyAddress(cluster);
            ManagerClient managerclient = new ManagerClient(proxy);
            if (!managerclient.isApacheHttpd()) {
                Maintest.stop(MAXSTOPCOUNT, wait, server, service, cluster);
                System.gc();
                System.out.println("TestQuery Skipped");
                return;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Can't check proxy type");
        }

        // Try with the rewrite rule.
        // RewriteCond %{HTTP_HOST} ^cluster\.domain\.info [NC]
        // ^/?([^/.]+)/(.*)$ balancer://mycluster/$2?partnerpath=/$1 [P,QSA]
        client = new Client();
        client.setVirtualHost("cluster.domain.info");

        // Do a request.
        try {
            if (client.runit("/hisname/MyTest?name=edwin&state=NY", 1, false, false) != 0)
                clienterror = true;
        } catch (Exception ex) {
            ex.printStackTrace();
            clienterror = true;
        }
        if (clienterror)
            fail("Client error");

        // Check for the result.
        response = client.getResponse();
        if (response.indexOf("partnerpath=/hisname&name=edwin&state=NY") == -1) {
            System.out.println("response: " + client.getResponse());
            fail("Can't find the query string in the response");
        }

        // Stop the server or services.
        try {
            wait.stopit();
            wait.join();
            server.removeService(service);
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
        System.out.println("TestQuery Done");
    }
}
