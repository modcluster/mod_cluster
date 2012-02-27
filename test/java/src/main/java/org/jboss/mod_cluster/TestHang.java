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
import java.net.ServerSocket;

import junit.framework.TestCase;

import org.apache.catalina.Engine;
import org.apache.catalina.Service;
import org.jboss.modcluster.ModClusterService;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardServer;

public class TestHang extends TestCase {

    /* Test that the sessions are really sticky */
    public void testHang() {

        boolean clienterror = false;
        ControlJBossWeb control = null;

        System.out.println("Hang Started");
        try {
            control = new ControlJBossWeb();
            control.newJBossWeb("node1",  "localhost");
            control.addConnector(8011);
            control.addService();
            control.addLifecycleListener("224.0.1.105", 23364);
            control.start();

        } catch(Exception ex) {
            ex.printStackTrace();
            fail("can't start service");
        }

        String result = null;
        try {
            // read the INFO message
            while ((result = control.getProxyInfo()) == null) {
                try {
                    Thread me = Thread.currentThread();
                    me.sleep(5000);
                } catch (Exception ex) {
                }
            }

            String [] nodes = new String[1];
            nodes[0] = "node1";
            while (!Maintest.checkProxyInfo(result, nodes)) {
                try {
                    Thread me = Thread.currentThread();
                    me.sleep(5000);
                } catch (Exception ex) {
                }
                result = control.getProxyInfo();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("can't find node1 running");
        }

        try {
            // exit the jboss the node is crashed
            control.exit();
            // Stop the jboss and remove the services.
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("can't exit service");
        }

        // Read the proxy from the result of getProxyInfo()
        String [] records = result.split("\n");
        String [] results = records[0].split(": \\[.*\\/");
        String proxy = null;
        if (results.length >=2 ) {
            records = results[1].split("\\]");
            proxy = records[0];
        } else
            fail("can't read proxy from " + result);

        ManagerClient managerclient = null;
        try {
            managerclient = new ManagerClient(proxy);
            int countinfo = 0;
            result = managerclient.getProxyInfo();
            String [] nodes = new String[1];
            nodes[0] = "node1";
            if (!Maintest.checkProxyInfo(result, nodes))
                fail("can't find node");
            while (!Maintest.checkProxyInfo(result, null) && countinfo < 60) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                countinfo++;
                result = managerclient.getProxyInfo();
            }
            if (countinfo == 60)
                fail("node doesn't dispair");

        } catch(Exception ex) {
            ex.printStackTrace();
            fail("can't test node");
        }


        System.gc();
        System.out.println("TestHang Done");
    }
}
