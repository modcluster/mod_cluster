/*
 *  mod_cluster
 *
 *  Copyright(c) 2013 Red Hat Middleware, LLC,
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
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;

import junit.framework.TestCase;

import org.apache.catalina.Engine;
import org.apache.catalina.Service;
import org.jboss.modcluster.ModClusterService;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardServer;

public class Test349 extends TestCase {

    /* Test that the sessions are really sticky */
    public void test349() {

        boolean clienterror = false;
        StandardServer server = new StandardServer();
        JBossWeb service = null;
        JBossWeb service2 = null;
        ModClusterService cluster = null;

        System.out.println("Test349 Started");

        Maintest.waitForFreePorts(8010, 2);

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
        String proxy = null;
        int tries = 0;
        while (proxy == null && tries<60) {
            proxy = Maintest.getProxyAddress(cluster);
            if (proxy != null)
                break;
            try {
                Thread me = Thread.currentThread();
                me.sleep(5000);
                tries++;
            } catch (Exception ex) {
            }
        }
        if (tries == 60) {
            fail("can't find proxy");
        }
        System.out.println("PROXY: "  + proxy);

        String [] nodes = new String[2];
        nodes[0] = "node1";
        nodes[1] = "node2";
        int countinfo = 0;
        while ((!Maintest.checkProxyInfo(cluster, nodes)) && countinfo < 20) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            countinfo++;
        }

        int index = proxy.indexOf(":"); 
        String host = proxy.substring(0, index);
        int port = Integer.parseInt(proxy.substring(index+1));
        System.out.println("PROXY: "  + host + " " + port);

        try {
	    String msg = "JVMRoute=" + nodes[1] + "&Alias=alias-1.example.com,alias-2.example.com,alias-3.example.com,alias-4.example.com,alias-5.example.com,alias-6.example.com,alias-7.example.com,alias-8.example.com,alias-9.example.com,alias-10.example.com,alias-11.example.com,alias-12.example.com,alias-13.example.com,alias-14.example.com,alias-15.example.com,alias-16.example.com,alias-17.example.com,alias-18.example.com,alias-19.example.com,alias-20.example.com,alias-21.example.com,alias-22.example.com,alias-23.example.com,alias-24.example.com,alias-25.example.com,alias-26.example.com,alias-27.example.com,alias-28.example.com,alias-29.example.com,alias-30.example.com,alias-31.example.com,alias-32.example.com,alias-33.example.com,alias-34.example.com,alias-35.example.com,alias-36.example.com,alias-37.example.com,alias-38.example.com,alias-39.example.com,alias-40.example.com,alias-41.example.com,alias-42.example.com,alias-43.example.com,alias-44.example.com,alias-45.example.com,alias-46.example.com,alias-47.example.com,alias-48.example.com,alias-49.example.com,alias-50.example.com,alias-51.example.com,alias-52.example.com,alias-53.example.com,alias-54.example.com,alias-55.example.com,alias-56.example.com,alias-57.example.com,alias-58.example.com,alias-59.example.com,alias-60.example.com,alias-61.example.com,alias-62.example.com,alias-63.example.com,alias-64.example.com,alias-65.example.com,alias-66.example.com,alias-67.example.com,alias-68.example.com,alias-69.example.com,alias-70.example.com,alias-71.example.com,alias-72.example.com,alias-73.example.com,alias-74.example.com,alias-75.example.com,alias-76.example.com,alias-77.example.com,alias-78.example.com,alias-79.example.com,alias-80.example.com,alias-81.example.com,alias-82.example.com,alias-83.example.com,alias-84.example.com,alias-85.example.com,alias-86.example.com,alias-87.example.com,alias-88.example.com,alias-89.example.com,alias-90.example.com,alias-91.example.com,alias-92.example.com,alias-93.example.com,alias-94.example.com,alias-95.example.com,alias-96.example.com,alias-97.example.com,alias-98.example.com,alias-99.example.com,alias-100.example.com,alias-101.example.com,alias-102.example.com,alias-103.example.com,alias-104.example.com,alias-105.example.com,alias-106.example.com,alias-107.example.com,alias-108.example.com,alias-109.example.com,alias-110.example.com,alias-111.example.com,alias-112.example.com,alias-113.example.com,alias-114.example.com,alias-115.example.com,alias-116.example.com,alias-117.example.com,alias-118.example.com,alias-119.example.com,alias-120.example.com,alias-121.example.com,alias-122.example.com,alias-123.example.com,alias-124.example.com,alias-125.example.com&Context=%2fModCluster349";
	    String req = "ENABLE-APP / HTTP/1.1\r\n"
                      + "Host: " + host + ":" + port + "\r\n" 
                      + "Content-Length: " + msg.length() + "\r\n"
                      + "User-Agent: ClusterListener/1.0\r\n"
                      + "Connection: Close\r\n"
                      + "\r\n"
                      + msg;
	    byte[] buffer = req.getBytes();
	    Socket socket = new Socket(host, port);
	    OutputStream out = socket.getOutputStream();
	    out.write(buffer, 0, 1000);
	    out.flush();
	    Thread.sleep(1000);
	    out.write(buffer, 1000, buffer.length - 1000);
	    out.flush();
	    InputStream in  = socket.getInputStream();
	    for (int read = in.read(buffer); read >= 0; read = in.read(buffer))
	        System.out.write(buffer, 0, read);
	    out.close();
	    in.close();
	    socket.close();
       } catch (Exception ex) {
            ex.printStackTrace();
            fail("can't send message");
       }

        // Read the result.
        String result = Maintest.getProxyInfo(cluster);
        System.out.println("INFO: " + result);
        if (result.indexOf("ModCluster349")==-1)
            fail("can't find ModCluster349 in: " + result);

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
        System.out.println("Test349 Done");
    }
}
