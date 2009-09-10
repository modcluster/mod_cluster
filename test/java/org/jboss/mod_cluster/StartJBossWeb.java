/*
 *  mod_cluster
 *
 *  Copyright(c) 2008 Red Hat Middleware, LLC,
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.apache.catalina.startup.Embedded;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Context;
import org.apache.catalina.*;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.*;
import org.apache.catalina.startup.HostConfig;

import org.apache.catalina.LifecycleListener;

public class StartJBossWeb {

    /* Start a JBossWEB with domain */
    public static void main(String[] args) {

        StandardServer server = null;
        JBossWeb service = null;
        int port = 8009;
        int serverport = 8005;
        String node = "node1";
        String domain = "dom1";
        if (args.length == 4) {
            port = Integer.parseInt(args[0]);
            node = args[1];
            domain = args[2];
            serverport = Integer.parseInt(args[3]);
        }
        System.out.println("Starting JBossWEB on " + port + " " + node + " " + domain + " " + serverport);

        try {
            server = (StandardServer) ServerFactory.getServer();
            server.setPort(serverport);

            service = new JBossWeb(node,  "localhost");
            service.addConnector(port);
            server.addService(service);
 
            LifecycleListener cluster = Maintest.createClusterListener("224.0.1.105", 23364, false, domain);
            server.addLifecycleListener(cluster);

            server.start();

        } catch(IOException ex) {
            ex.printStackTrace();
        } catch (LifecycleException ex) {
            ex.printStackTrace();
        }

        // Wait until we are stopped...
        server.await();

        // Stop the server or services.
        try {
            server.stop();
            // service.stop();
            // service2.stop();
        } catch (LifecycleException ex) {
            ex.printStackTrace();
        }

    }
}
