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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import java.lang.Exception;

import org.apache.catalina.ServerFactory;
import org.apache.catalina.Service;
import org.apache.catalina.Engine;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.connector.Connector;

import org.apache.catalina.LifecycleListener;

import org.jboss.web.cluster.ClusterListener;

public class Maintest extends TestCase {

    static StandardServer server = null;
    static boolean isJBossWEB = true;
    public static void main( String args[] ) {
       TestRunner.run(suite());
    }
    public static Test suite() {
       TestSuite suite = new TestSuite();
       server = (StandardServer) ServerFactory.getServer();
       
       // Read the -Dcluster=true/false.
       String jbossweb = System.getProperty("cluster");
       if (jbossweb != null && jbossweb.equalsIgnoreCase("false")) {
            System.out.println("Running tests with jbossweb listener");
    	    isJBossWEB = true;
       } else {
            System.out.println("Running tests with mod_cluster listener");
    	    isJBossWEB = false;
       }

       // Read the -Dtest="value".
       String test = System.getProperty("test");
       if (test != null) {
            System.out.println("Running single test: " + test);
            try {
                Class clazz = Class.forName("org.jboss.mod_cluster." + test);
                suite.addTest(new TestSuite(clazz));
            } catch (ClassNotFoundException ex) {
                System.out.println("Running single test: " + test + " Not found");
                return null;
            }
       } else {
            suite.addTest(new TestSuite(TestAddDel.class));
            System.gc();
            suite.addTest(new TestSuite(TestBase.class));
            System.gc();
            suite.addTest(new TestSuite(TestFailover.class));
            System.gc();
            suite.addTest(new TestSuite(TestStickyForce.class));
            System.gc();
            /* XXX The JBWEB_117 tests are not really related to mod_cluster
             * Run them one by one using ant one -Dtest=test
            suite.addTest(new TestSuite(TestJBWEB_117.class));
            System.gc();
            suite.addTest(new TestSuite(Test_Native_JBWEB_117.class));
            System.gc();
            suite.addTest(new TestSuite(Test_Chunk_JBWEB_117.class));
            System.gc();
            */
       }
       return suite;
    }
    static StandardServer getServer() {
       return server;
    }
    static boolean isJBossWEB() {
    	return isJBossWEB;
    }

    /* Print the service and connectors the server knows */
    static void listServices() {
            Service[] services = server.findServices();
            for (int i = 0; i < services.length; i++) {
                System.out.println("service[" + i + "]: " + services[i]);
                Engine engine = (Engine) services[i].getContainer();
                System.out.println("engine: " + engine);
                System.out.println("connectors: " + services[i].findConnectors());
                Connector [] connectors = services[i].findConnectors();
                for (int j = 0; j < connectors.length; j++) {
                    System.out.println("connector: " + connectors[j]);
                }
            }
    }
    static LifecycleListener createClusterListener(String groupa, int groupp, boolean ssl) {
    	return createClusterListener(groupa, groupp, ssl, null);
    }
    static LifecycleListener createClusterListener(String groupa, int groupp, boolean ssl, String domain) {
    	return createClusterListener(groupa, groupp, ssl, domain, true, false, true);
    }
    static LifecycleListener createClusterListener(String groupa, int groupp, boolean ssl, String domain,
                                                   boolean stickySession, boolean stickySessionRemove,
                                                   boolean stickySessionForce) {
        LifecycleListener lifecycle = null;
        ClusterListener jcluster = null;
        org.jboss.modcluster.ModClusterListener pcluster = null;

        if (isJBossWEB) {
            jcluster = new ClusterListener();
            jcluster.setAdvertiseGroupAddress(groupa);
            jcluster.setAdvertisePort(groupp);
            jcluster.setSsl(ssl);
            jcluster.setDomain(domain);
            jcluster.setStickySession(stickySession);
            jcluster.setStickySessionRemove(stickySessionRemove);
            jcluster.setStickySessionForce(stickySessionForce);
            lifecycle = jcluster;
        } else {
            pcluster = new org.jboss.modcluster.ModClusterListener();
            pcluster.setAdvertiseGroupAddress(groupa);
            pcluster.setAdvertisePort(groupp);
            pcluster.setSsl(ssl);
            pcluster.setDomain(domain);
            pcluster.setStickySession(stickySession);
            pcluster.setStickySessionRemove(stickySessionRemove);
            pcluster.setStickySessionForce(stickySessionForce);
            lifecycle = pcluster;

        }

        return lifecycle;
    }
    static String getProxyInfo(LifecycleListener lifecycle) {
        String result = null;
        if (isJBossWEB) {
            ClusterListener jcluster = (ClusterListener) lifecycle;
            result = jcluster.getProxyInfo();
        } else {
            org.jboss.modcluster.ModClusterListener pcluster = (org.jboss.modcluster.ModClusterListener) lifecycle;
            result = pcluster.getProxyInfo();
        }
        return result;
    }
}
