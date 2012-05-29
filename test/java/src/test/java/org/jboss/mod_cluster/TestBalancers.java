/*
 *  mod_cluster
 *
 *  Copyright(c) 2012 Red Hat Middleware, LLC,
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

public class TestBalancers extends TestCase {

    /* Test that the sessions are really sticky */
    public void testBalancers() {
        myBalancers(null, null, null, null);
    }
    public void testBalancers2() {
        myBalancers("balancer", "dom1", "balancer", "dom1");
    }
    public void testBalancers3() {
        myBalancers("balancer", "dom1", "balancer", "dom2");
    }
    
    /* Can't work
    public void testBalancers4() {
        myBalancers("balancer1", "dom1", "balancer2", "dom2");
    }
    */

    public void myBalancers(String balancer, String loadBalancingGroup, String balancer2, String loadBalancingGroup2) {
        boolean clienterror = false;
        System.setProperty("org.apache.catalina.core.StandardService.DELAY_CONNECTOR_STARTUP", "false");
        StandardServer server =  new StandardServer();
        StandardServer server2 = new StandardServer();
        JBossWeb service = null;
        JBossWeb service2 = null;
        ModClusterService cluster = null;
        ModClusterService cluster2 = null;

        System.out.println("TestBalancers Started");
        try {

            service = new JBossWeb("node1",  "localhost");
            service.addConnector(8011);
            server.addService(service);
 
            service2 = new JBossWeb("node2",  "localhost");
            service2.addConnector(8012);
            server2.addService(service2);

            cluster = Maintest.createClusterListener(server, "224.0.1.105", 23364, false, null, true, false, true, "secret", balancer, loadBalancingGroup);
            cluster2 = Maintest.createClusterListener(server2, "224.0.1.105", 23364, false, null, true, false, true, "secret", balancer2, loadBalancingGroup2);

        } catch(Exception ex) {
            ex.printStackTrace();
            fail("can't start service");
        }

        // start the server thread.
        ServerThread wait = new ServerThread(3000, server);
        wait.start();
        ServerThread wait2 = new ServerThread(3000, server2);
        wait2.start();
         
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
           fail("Can't start node1");
        nodes[0] = "node2";
        countinfo = 0;
        while ((!Maintest.checkProxyInfo(cluster2, nodes)) && countinfo < 20) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            countinfo++;
        }
        if (countinfo == 20)
           fail("Can't start node2");

        // Start the client and wait for it.
        Client client = new Client();
        String node = null;
        try {
			client.runit("/MyCount", 20, true);
			node = client.getnode();
		} catch (Exception e) {
			e.printStackTrace();
			clienterror = true;
		}
        countinfo = 0;
        while (client.getnode().equals(node) && !clienterror && countinfo < 20) {
        	Client client2 = new Client();
        	try {
        		client2.runit("/MyCount", 20, true);
        		client.start();
        		client.join();
        	} catch (Exception e) {
    			e.printStackTrace();
    			clienterror = true;       		
        	}
        	client = client2;
        	countinfo++;
        }
        if (countinfo == 20)
        	fail("Can't connect to " + node);
      
         // Wait for it.
        try {
            client.start();
            client.join();
        } catch (Exception ex) {
            ex.printStackTrace();
            clienterror = true;
        }
        if (client.getresultok())
            System.out.println("Test DONE");
        else {
            System.out.println("Test FAILED");
            clienterror = true;
        }

        // Stop the jboss and remove the services.
        try {
            wait.stopit();
            wait.join();
            wait2.stopit();
            wait2.join();

            server.removeService(service);
            server2.removeService(service2);
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
        System.out.println("TestBalancers Done");
    }
}
