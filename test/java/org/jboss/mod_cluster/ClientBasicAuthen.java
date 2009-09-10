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

import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;

import org.apache.catalina.Engine;
import org.apache.catalina.ServerFactory;
import org.apache.catalina.Service;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardServer;

import org.apache.catalina.core.AprLifecycleListener;

public class ClientBasicAuthen {

    /* Tests for Basic Authentication needs (JBWEB-117.war) */
    public static String test(boolean nat, File fd) {
        InputStream in = null;
        if (fd != null)
            try {
                in = new FileInputStream(fd);
            } catch(IOException ex) {
                ex.printStackTrace();
                return("can't start service");
            }
        return test(nat, in);
    }
    public static String test(boolean nat, InputStream fd) {

        boolean clienterror = false;
        StandardServer server = Maintest.getServer();
        JBossWeb service = null;
        LifecycleListener cluster = null;
        try {
            // server = (StandardServer) ServerFactory.getServer();

            service = new JBossWeb("node1",  "localhost", nat);
            if (nat)
                service.addConnector(8009, "org.apache.coyote.ajp.AjpAprProtocol");
            else
                service.addConnector(8009, "ajp");

            service.addWAR("JBWEB-117.war", "node1");
            server.addService(service);
 
            cluster =  Maintest.createClusterListener("224.0.1.105", 23364, false);
            server.addLifecycleListener(cluster);

            // Add AprLifecycleListener.
            if (nat) {
                AprLifecycleListener listener = new AprLifecycleListener();
                server.addLifecycleListener((LifecycleListener) listener);
            }

            // Debug Stuff
            // Maintest.listServices();

        } catch(IOException ex) {
            ex.printStackTrace();
            return("can't start service");
        }

        // start the server thread.
        ServerThread wait = new ServerThread(3000, server);
        wait.start();
         
        // Wait until httpd as received the nodes information.
        try {
            Thread.sleep(40000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        // Start the client and wait for it.
        Client client = new Client();

        // Wait for it.
        String data = "a";
        for (int i=0; i<517; i++)
            data = data.concat("a");
        int ret = 0;
        try {
            ret = client.runit("/JBWEB-117/JBWEB_117", 1, true, data, "manager" , "manager", fd);
            client.start();
            client.join();
        } catch (Exception ex) {
            ex.printStackTrace();
            clienterror = true;
        }

        // Stop the jboss and remove the services.
        try {
            wait.stopit();
            wait.join();

            server.removeService(service);
            server.removeLifecycleListener(cluster);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            return("can't stop service");
        }
        if (clienterror)
            return("Client error");

        if (ret != 401)
            return("Should get 401 code");

        // Wait until httpd as received the stop messages.
        System.gc();
        try {
            Thread.sleep(20000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    return null;
    }
}
