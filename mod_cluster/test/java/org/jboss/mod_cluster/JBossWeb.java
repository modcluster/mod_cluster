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

import java.net.InetAddress;
import java.net.*;

import org.apache.catalina.startup.Embedded;
import org.apache.catalina.Realm;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Context;
import org.apache.catalina.*;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.IntrospectionUtils;
import org.apache.catalina.realm.MemoryRealm;
import org.apache.catalina.core.*;

import org.jboss.web.cluster.ClusterListener;

public class JBossWeb extends Embedded {

    private void copyFile(File in, File out) throws IOException {
        FileInputStream fis  = new FileInputStream(in);
        FileOutputStream fos = new FileOutputStream(out);
        byte[] buf = new byte[1024];
        int i = 0;
        while((i=fis.read(buf))!=-1) {
            fos.write(buf, 0, i);
        }
        fis.close();
        fos.close();
    }


    public JBossWeb(String route, String host) throws IOException {

        setCatalinaBase(route);
        setCatalinaHome(route);

        //Create an Engine
        Engine baseEngine = createEngine();

        baseEngine.setName(host + "Engine" + route);
        baseEngine.setDefaultHost(host);
        baseEngine.setJvmRoute(route);
        baseEngine.setRealm(null);

        // Create node1/webapps/ROOT and index.html
        File fd = new File ( route + "/webapps/ROOT");
        fd.mkdirs();
        String docBase = fd.getAbsolutePath();
        String appBase = fd.getParent();
        fd = new File (route + "/webapps/ROOT" , "index.html");
        FileWriter out = new FileWriter(fd);
        out.write(route + ":This is a test\n");
        out.close();

        // Copy a small servlet for testing.
        fd = new File ( route + "/webapps/ROOT/WEB-INF/classes");
        fd.mkdirs();
        fd = new File (route + "/webapps/ROOT/WEB-INF/classes" , "MyCount.class");
        File fdin = new File ("MyCount.class");
        copyFile(fdin, fd);
        

        //Create Host
        Host baseHost =  createHost( host, appBase);
        baseEngine.addChild( baseHost );

        //Create default context
        Context rootContext = createContext("/",docBase);
        rootContext.setIgnoreAnnotations(true);
        baseHost.addChild( rootContext );
        addEngine( baseEngine );
        baseEngine.setService(this);
    }
    public void addConnector(int port) throws IOException {
    

        Connector connector = createConnector( (java.net.InetAddress) null,
                                              port, "http");
        //                                      port, "ajp");

        // Look in StandardService to see why it works ;-)
        addConnector( connector );
    }


    /* Test */
    public static void main(String[] args) {

        StandardServer server = null;
        JBossWeb service = null;
        JBossWeb service2 = null;
        try {
            server = (StandardServer) ServerFactory.getServer();

            service = new JBossWeb("node1",  "localhost");
            service.addConnector(8009);
            server.addService(service);
 
            service2 = new JBossWeb("node2",  "localhost");
            service2.addConnector(8000);
            server.addService(service2);

            ClusterListener cluster = new ClusterListener();
            cluster.setAdvertiseGroupAddress("232.0.0.2");
            cluster.setAdvertisePort(23364);
            cluster.setSsl(false);
            // SSL ?
            server.addLifecycleListener((LifecycleListener) cluster);

            Service[] services = server.findServices();
            for (int i = 0; i < services.length; i++) {
               System.out.println("service[" + i + "]: " + services[i]);
               Engine engine = (Engine) services[i].getContainer();
               System.out.println("engine: " + engine);
               // System.out.println("service: " + engine.getService());
               // System.out.println("connectors: " + engine.getService().findConnectors());
               System.out.println("JFC connectors: " + services[i].findConnectors());
               Connector [] connectors = services[i].findConnectors();
               for (int j = 0; j < connectors.length; j++) {
                    System.out.println("JFC connector: " + connectors[j]);
               }
            }

            server.start();

        } catch(IOException ex) {
            ex.printStackTrace();
        } catch (LifecycleException ex) {
            ex.printStackTrace();
        }

        // Wait until httpd as received the nodes information.
        try {
            Thread.sleep(15000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        // Start the client and wait for it.
        Client client = new Client();

        // Wait for it.
        try {
            client.runit("http://localhost:7779/ROOT/MyCount", "cmd", 10, true);
            client.start();
            client.join();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (client.getresultok())
            System.out.println("Test DONE");
        else
            System.out.println("Test FAILED");

        // Stop the server or services.
        try {
            server.stop();
            // service.stop();
            // service2.stop();
        } catch (LifecycleException ex) {
            ex.printStackTrace();
        }

        // Wait until httpd as received the stop messages.
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }
}
