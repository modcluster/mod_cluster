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

public class Maintest extends TestCase {

    static StandardServer server = null;
    public static void main( String args[] ) {
       TestRunner.run(suite());
    }
    public static Test suite() {
       TestSuite suite = new TestSuite();
       server = (StandardServer) ServerFactory.getServer();
       suite.addTest(new TestSuite(TestAddDel.class));
       System.gc();
       suite.addTest(new TestSuite(TestBase.class));
       System.gc();
       suite.addTest(new TestSuite(TestFailover.class));
       System.gc();
       return suite;
    }
    static StandardServer getServer() {
       return server;
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
}
