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

import org.jboss.modcluster.ModClusterService;
import org.apache.catalina.core.StandardServer;

public class TestAliases extends TestCase {
    
    public void testAliases() {
        String [] Aliases = new String[10];
        /* HOSTALIASZ is 100 that should be enough */
        Aliases[0] = "alias0";
        Aliases[1] = "alias1";
        Aliases[2] = "alias2";
        Aliases[3] = "alias3";
        Aliases[4] = "alias5";
        Aliases[5] = "alias5";
        Aliases[6] = "alias6";
        Aliases[7] = "alias7";
        Aliases[8] = "alias8";
        Aliases[9] = "alias9";
        
        String [] Aliases2 = new String[1];
        Aliases2[0] = "alias0123456789012345678901234567890123456789012345678901234567890123456789012345out";

        myAliases(Aliases, Aliases2);

    }
    
    public void testAliases2() {
        String [] Aliases = new String[2];
        Aliases[0] = "alias0";
        Aliases[1] = "alias1";
        
        String [] Aliases2 = new String[2];
        Aliases2[0] = "alias0123456789012345678901234567890123456789012345678901234567890123456789012345out";
        Aliases2[1] = "alias1123456789012345678901234567890123456789012345678901234567890123456789012345out";
        myAliases(Aliases, Aliases2);

    }
    
    /* Test failAppover */
    private void myAliases(String [] Aliases,String [] Aliases2 ) {
    	System.setProperty("org.apache.catalina.core.StandardService.DELAY_CONNECTOR_STARTUP", "false");
        boolean clienterror = false;
        StandardServer server =  new StandardServer();
        JBossWeb service = null;
        JBossWeb service2 = null;
        ModClusterService cluster = null;
        System.out.println("TestAliases Started");
        try {
 
            service = new JBossWeb("node3",  "localhost", false, "ROOT", Aliases);
            service.addConnector(8013);
            service.AddContext("/test", "/test");
            server.addService(service);

            service2 = new JBossWeb("node4",  "localhost", false, "ROOT", Aliases2);
            service2.addConnector(8014);
            service2.AddContext("/test", "/test");
            server.addService(service2);

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

        // Wait until 2 nodes are created in httpd.
        String [] nodes = new String[2];
        nodes[0] = "node3";
        nodes[1] = "node4";
        Maintest.TestForNodes(cluster, nodes);

        // Test wrong Hostname.
        Client client = new Client();
        client.setVirtualHost("mycluster.domain.com");

        // Wait for it.
        try {
            if (client.runit("/MyCount", 10, false, true) != 0)
                clienterror = true;
        } catch (Exception ex) {
            ex.printStackTrace();
            clienterror = true;
        }
        if (!clienterror)
            fail("Client should fail (wrong host)");

        // Test long Hostname.
        client = new Client();
        client.setVirtualHost(Aliases2[0]);
        clienterror = false;

        // Wait for it.
        try {
            if (client.runit("/MyCount", 10, false, true) != 0)
                clienterror = true;
        } catch (Exception ex) {
            ex.printStackTrace();
            clienterror = true;
        }
        if (clienterror)
            fail("Client fail (long host)");
        String node = client.getnode();
        if (!"node4".equals(node)) {
            fail("Fail (long host) wrong node");
        }
        
        if (Aliases2.length>1) {
        	// Check that the last alias is also working.
            client = new Client();
            client.setVirtualHost(Aliases2[Aliases2.length-1]);
            try {
                if (client.runit("/MyCount", 10, false, true) != 0)
                    clienterror = true;
            } catch (Exception ex) {
                ex.printStackTrace();
                clienterror = true;
            }
            if (clienterror)
                fail("Client fail (long host: " +Aliases2[Aliases2.length-1] + ")");
        }

        // Start the client and wait for it.
        client = new Client();
        clienterror = false;

        // Wait for it.
        try {
            if (client.runit("/MyCount", 10, false, true) != 0)
                clienterror = true;
        } catch (Exception ex) {
            ex.printStackTrace();
            clienterror = true;
        }
        if (clienterror)
            fail("Client error");

        // Stop the connector that has received the request...
        node = client.getnode();
        if ("node4".equals(node)) {
            service2.removeContext("");
            node = "node3";
        } else {
            service.removeContext("");
            node = "node4";
        }

        // Run a test on it. (it waits until httpd as received the nodes information).
        client.setnode(node);
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
