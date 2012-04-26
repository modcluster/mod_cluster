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

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;

public class TestBaseJgroups extends TestCase {

    /* Test that the sessions are really sticky */
    public void testBase() {

        boolean clienterror = false;
        StandardServer server = Maintest.getServer();
        ModClusterService cluster = null;
        JBossWeb service = null;

        System.out.println("TestBaseJgroups Started");
        try {
            service = new JBossWeb("node1",  "localhost");
            service.addConnector(8011);
            server.addService(service);
            cluster = Maintest.createClusterListener("224.0.1.105", 23364, false, null, true, false, true, "secret");
        } catch(Exception ex) {
            ex.printStackTrace();
            fail("can't start service");
        }

        // start the server thread.
        ServerThread wait = new ServerThread(3000, server);
        wait.start();

        // Wait until httpd we know about httpd.
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

        // Create 2 JGroups ID and query the result and remove them.
        String proxy = Maintest.getProxyAddress(cluster);
        String URL = "http://" + proxy + "/";
        HttpClient httpClient = new HttpClient();
        PostMethod pm = null;
        pm = (PostMethod) new AddIdMethod(URL);
        pm.addParameter("JGroupUuid", "ID1");
        pm.addParameter("JGroupData", "DATA1");
        String response = processrequest(pm, httpClient);
        if (response == null)
            fail("ADDID(1) failed on: " + URL);

        pm = (PostMethod) new AddIdMethod(URL);
        pm.addParameter("JGroupUuid", "ID2");
        pm.addParameter("JGroupData", "DATA2");
        response = processrequest(pm, httpClient);
        if (response == null)
            fail("ADDID(2) failed");

        pm = (PostMethod) new QueryMethod(URL);
        pm.addParameter("JGroupUuid", "*");
        response = processrequest(pm, httpClient);
        if (response == null)
            fail("QUERY failed");
        System.out.println("Response:\n" + response);
        String [] records = response.split("\n");
        if (records.length != 2)
            fail("QUERY return " + records.length + " JGroupUuid instead 2");

        pm = (PostMethod) new RemoveIdMethod(URL);
        pm.addParameter("JGroupUuid", "ID2");
        response = processrequest(pm, httpClient);
        if (response == null)
            fail("REMOVE(ID2) failed");

        pm = (PostMethod) new RemoveIdMethod(URL);
        pm.addParameter("JGroupUuid", "ID1");
        response = processrequest(pm, httpClient);
        if (response == null)
            fail("REMOVE(ID1) failed");

/* See MODCLUSTER-282 it doesn't work on all hudson boxes.
        pm = (PostMethod) new QueryMethod(URL);
        pm.addParameter("JGroupUuid", "*");
        response = processrequest(pm, httpClient);
        if (response == null)
            fail("QUERY failed");
        System.out.println("Response:\n" + response);
        if (response.length() == 0)
            System.out.println("AddId + Remove OK");
        else
            fail("QUERY returns " + response + " instead nothing");
 */

        // Stop the jboss and remove the services.
        try {
            wait.stopit();
            wait.join();
            server.removeService(service);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            fail("can't stop service");
        }
        if (clienterror)
            fail("Client error");

        // Wait until httpd as received the stop messages.
        countinfo = 0;
        while ((!Maintest.checkProxyInfo(cluster, null)) && countinfo < 20) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            countinfo++;
        }
        Maintest.StopClusterListener();
        
        System.gc();
        System.out.println("TestBaseJgroups Done");
    }

    public static String processrequest(PostMethod pm, HttpClient httpClient)
    {
        Integer connectionTimeout = 40000;
        pm.getParams().setParameter("http.socket.timeout", connectionTimeout);
        pm.getParams().setParameter("http.connection.timeout", connectionTimeout);
        httpClient.getParams().setParameter("http.socket.timeout", connectionTimeout);
        httpClient.getParams().setParameter("http.connection.timeout", connectionTimeout);

        int httpResponseCode = 0;
        try {
            httpResponseCode = httpClient.executeMethod(pm);
            System.out.println("response: " + httpResponseCode);
            System.out.println("response: " + pm.getStatusLine());
            if (httpResponseCode == 500) {
                System.out.println(pm.getResponseHeader("Version"));
                System.out.println(pm.getResponseHeader("Type"));
                System.out.println(pm.getResponseHeader("Mess"));
                return null;
            }
            if (httpResponseCode == 200) {
                int len = (int) pm.getResponseContentLength();
                return pm.getResponseBodyAsString(len);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public class AddIdMethod extends PostMethod {
        public String getName() {
            return "ADDID";
        }
        public AddIdMethod(String uri) {
            super(uri);
        }
    }
    public class RemoveIdMethod extends PostMethod {
        public String getName() {
            return "REMOVEID";
        }
        public RemoveIdMethod(String uri) {
            super(uri);
        }
    }
    public class QueryMethod extends PostMethod {
        public String getName() {
            return "QUERY";
        }
        public QueryMethod(String uri) {
            super(uri);
        }
    }
}
