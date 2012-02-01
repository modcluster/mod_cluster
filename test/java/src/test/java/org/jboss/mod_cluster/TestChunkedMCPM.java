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

import java.util.ArrayList;

import junit.framework.TestCase;

import org.apache.catalina.Engine;
import org.apache.catalina.ServerFactory;
import org.apache.catalina.Service;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardServer;

public class TestChunkedMCPM extends TestCase {

    /* Test */
    public void testChunkedMCPM() {

        boolean clienterror = false;
        int numbnodes = 30;
        String [] nodenames = new String [numbnodes];
        JBossWeb [] service = new JBossWeb[numbnodes];
        LifecycleListener lifecycle = null;

        System.out.println("TestChunkedMCPM Started");
        StandardServer server = Maintest.getServer();
        for (int i=0; i<numbnodes; i++) {
            try {
                // server = (StandardServer) ServerFactory.getServer();
                String name = "node" + i;
                nodenames[i] = name;
                service[i] = new JBossWeb(name,  "localhost");
                service[i].addConnector(8010 + i);
                server.addService(service[i]);
 
            } catch(IOException ex) {
                ex.printStackTrace();
                fail("can't start service");
            }
        }

        lifecycle = Maintest.createClusterListener("224.0.1.105", 23364, false, null, true, false, true, "secret");

        server.addLifecycleListener(lifecycle);

        // Debug Stuff
        // Maintest.listServices();

        // start the server thread.
        ServerThread wait = new ServerThread(3000, server);
        wait.start();

        // Wait until we are able to connect to httpd.
        int tries = Maintest.WaitForHttpd(lifecycle, 60);
        if (tries == -1) {
            fail("can't find PING-RSP in proxy response");
        }

        // Wait until httpd as received the nodes information.
        try {
            Thread.sleep(tries*1000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        // Read the result via INFO.
        String result = Maintest.getProxyInfo(lifecycle);

        ArrayList nodes = new ArrayList();
        if (result != null) {
            System.out.println(result);
            String [] records = result.split("\n");
            int l = 0;
            for (int i=0; i<records.length; i++) {
                // System.out.println("records[" + i + "]: " + records[i]);
                NodeInfo nodeinfo = null;
                String [] results = records[i].split(",");
                for (int j=0; j<results.length; j++, l++) {
                    // System.out.println("results[" + j + "]: " + results[j]);
                    String [] data = results[j].split(": ");
                    // System.out.println("data[" + 0 + "]: " + data[0] + "*");
                    if ("Node".equals(data[0]) && nodeinfo == null) {
                        nodeinfo = new NodeInfo();
                        continue;
                    }
                    if ("Name".equals(data[0])) {
                        nodeinfo.JVMRoute = data[1];
                    }
                    else if ("Load".equals(data[0])) {
                        nodeinfo.lbfactor = Integer.valueOf(data[1]).intValue();
                    }
                    else if ("Elected".equals(data[0])) {
                        nodeinfo.elected = Integer.valueOf(data[1]).intValue();
                    }
                }
                if (nodeinfo != null) {
                    // System.out.println("Adding: " + nodeinfo);
                    nodes.add(nodeinfo);
                }
            }
        } else {
            System.out.println("getProxyInfo failed");
            clienterror = true;
        }

        // Check the nodes.
        if (!clienterror) {
            if (!NodeInfo.check(nodes, nodenames)) {
                System.out.println("getProxyInfo nodes incorrect");
                NodeInfo.print(nodes, nodenames);
                clienterror = true;
            }
        }
         
        // Stop the jboss and remove the services.
        try {
            wait.stopit();
            wait.join();

            for (int i=0; i<numbnodes; i++) {
                server.removeService(service[i]);
            }
            server.removeLifecycleListener(lifecycle);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            fail("can't stop service");
        }

        // Wait until httpd as received the stop messages.
        System.gc();
        if (!Maintest.TestForNodes(lifecycle, null))
            fail("Can't stop nodes");

        if (clienterror)
            fail("Client error");
        System.out.println("TestChunkedMCPM Done");

    }
}
