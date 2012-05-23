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
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.Map;

import org.apache.catalina.Service;
import org.apache.catalina.Engine;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.connector.Connector;

import org.jboss.modcluster.ModClusterService;
import org.jboss.modcluster.config.impl.ModClusterConfig;
import org.jboss.modcluster.container.catalina.CatalinaEventHandlerAdapter;
import org.jboss.modcluster.load.impl.SimpleLoadBalanceFactorProvider;

public class Maintest {

	static StandardServer server = null;
	static CatalinaEventHandlerAdapter adapter = null;
    static StandardServer getServer() {
    	System.setProperty("org.apache.catalina.core.StandardService.DELAY_CONNECTOR_STARTUP", "false");
    	server = new StandardServer();
        return server;
    }
    
    /* Print the service and connectors the server knows */
    static void listServices(StandardServer server) {
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
    static ModClusterService createClusterListener(String groupa, int groupp, boolean ssl) {
    	return createClusterListener(groupa, groupp, ssl, null);
    }
    static ModClusterService createClusterListener(String groupa, int groupp, boolean ssl, String domain) {
    	return createClusterListener(groupa, groupp, ssl, domain, true, false, true, null);
    }
    static ModClusterService createClusterListener(String groupa, int groupp, boolean ssl, String domain,
                                                   boolean stickySession, boolean stickySessionRemove,
                                                   boolean stickySessionForce) {
    	return createClusterListener(groupa, groupp, ssl, domain, stickySession, stickySessionRemove, stickySessionForce, null);
    }
    /* Create the listener
     * server: the server to use.
     * groupa: multi address to receive from httpd.
     * groupp: port to receive from httpd.
     * ssl: use ssl.
     * domain: domain to send to httpd (to fail over in the domain).
     * stickySession: use stickySession.
     * stickySessionRemove: remove the sessionid if we are sticky and need to failover.
     * stickySessionForce: return an error if we have to failover to another node.
     * advertiseSecurityKey: Key for the digest logic.
     * balancer: balancer name.
     * loadBalancingGroup: mod_cluster domain name.
     */ 
    static ModClusterService createClusterListener(StandardServer server,
                                                   String groupa, int groupp, boolean ssl, String domain,
                                                   boolean stickySession, boolean stickySessionRemove,
                                                   boolean stickySessionForce, String advertiseSecurityKey,
                                                   String balancer, String loadBalancingGroup) {
        ModClusterConfig config = new ModClusterConfig();
        config.setAdvertiseGroupAddress(groupa);
        config.setAdvertisePort(groupp);
        config.setSsl(ssl);
        config.setLoadBalancingGroup(domain);
        config.setStickySession(stickySession);
        config.setStickySessionRemove(stickySessionRemove);
        config.setStickySessionForce(stickySessionForce);
        config.setNodeTimeout(20000);
        if (balancer != null)
        	config.setBalancer(balancer);
        if (loadBalancingGroup != null)
        	config.setLoadBalancingGroup(loadBalancingGroup);
        if (advertiseSecurityKey != null)
        	config.setAdvertiseSecurityKey(advertiseSecurityKey);
        SimpleLoadBalanceFactorProvider load = new SimpleLoadBalanceFactorProvider();
        load.setLoadBalanceFactor(1);
        ModClusterService service = new ModClusterService(config, load);
        adapter = new CatalinaEventHandlerAdapter(service, server);
        adapter.start();
        
        return service;
    }
    static ModClusterService createClusterListener(StandardServer server,
            String groupa, int groupp, boolean ssl, String domain,
            boolean stickySession, boolean stickySessionRemove,
            boolean stickySessionForce, String advertiseSecurityKey)
    {
    	return createClusterListener(server, groupa, groupp, ssl, domain,
                stickySession, stickySessionRemove,
                stickySessionForce, advertiseSecurityKey, null, null);
    }

    static ModClusterService createClusterListener(String groupa, int groupp, boolean ssl, String domain,
                                                   boolean stickySession, boolean stickySessionRemove,
                                                   boolean stickySessionForce, String advertiseSecurityKey) {
         return createClusterListener(getServer(), groupa, groupp, ssl, domain,
                                                   stickySession, stickySessionRemove,
                                                   stickySessionForce, advertiseSecurityKey);
    }
    
    /*
     * Stop the adapter for the ClusterListener
     */
    static void StopClusterListener() {
    	if (adapter != null)
    		adapter.stop();
    	adapter = null;
    }
    /* ping httpd */
    static String doProxyPing(ModClusterService pcluster) {
        String result = null;
        
        Map<InetSocketAddress, String> map = pcluster.ping();
        if (map.isEmpty())
        	return null;
        Object results[] = map.values().toArray();
        result = (String ) results[0];

        return result;
    }
    /* ping a node (via JVmRoute). */
    static String doProxyPing(ModClusterService pcluster, String JvmRoute) {
    	String result = null;
     	Map<InetSocketAddress, String> map = pcluster.ping(JvmRoute);
    	if (map.isEmpty())
    		return null;
    	Object results[] = map.values().toArray();
    	/* We may have several answers return the first one that has PING-RSP in it */
    	for (int i=0; i<results.length; i++) {
    		result = (String) results[i];
    		if (result.indexOf("PING-RSP")>0)
    			break;
    	}
        return result;
    }
    static String doProxyPing(ModClusterService pcluster, String scheme, String host, int port) {
        String result = null;
        Map<InetSocketAddress, String> map = pcluster.ping(scheme, host, port);
        if (map.isEmpty())
        	return null;
        Object results[] = map.values().toArray();
        /* We may have several answers return the first one that has PING-RSP in it */
        for (int i=0; i<results.length; i++) {
        	result = (String) results[i];
        	if (result.indexOf("PING-RSP")>0)
        		break;
        }
        return result;
    }
    /* Analyse the PING-RSP message: Type=PING-RSP&State=OK&id=1 */
    static boolean checkProxyPing(String result) {
        String [] records = result.split("\n");
        String [] results = null;
        if (records.length == 3)
            results = records[1].split("&");
        else
            results = result.split("&");
        int ret = 0;
        for (int j=0; j<results.length; j++) {
            String [] data = results[j].split("=");
            if (data[0].compareToIgnoreCase("Type") == 0 &&
                data[1].compareToIgnoreCase("PING-RSP") == 0)
                ret++;
            if (data[0].compareToIgnoreCase("State") == 0 &&
                data[1].compareToIgnoreCase("OK") == 0)
                ret++;
        }
        if (ret == 2)
           return true;

        return false;
    }
    static String getProxyInfo(ModClusterService pcluster) {
        String result = null;

         Map<InetSocketAddress, String> map = pcluster.getProxyInfo();
        if (map.isEmpty())
        	return null;
        Object results[] = map.values().toArray();
        result = (String) results[0];
        return result;
    }
    static String  getProxyAddress(ModClusterService pcluster) {
        String proxy = null;

        Map<InetSocketAddress, String> map = pcluster.getProxyInfo();
        if (!map.isEmpty()) {
        	Object results[] = map.keySet().toArray();;
        	InetSocketAddress result = (InetSocketAddress) results[0];
        	proxy = result.getHostName() + ":" + result.getPort();
        }
        return proxy;
    }
    /* Check that the nodes are returned by the INFO command */
    static boolean checkProxyInfo(ModClusterService pcluster, String [] nodes) {
        String result = getProxyInfo(pcluster);
        return checkProxyInfo(result, nodes);
    }
    static boolean checkProxyInfo(String result, String [] nodes) {
        if (result == null) {
            if (nodes == null)
                return true;
            else
                return false;
        }
        /* create array to check the nodes */
        boolean [] n = null;
        if (nodes != null && nodes.length>0) {
            n = new boolean[nodes.length];
            for (int i=0; i<nodes.length; i++) {
                n[i] = false;
            }
        }

        String [] records = result.split("\n");
        int l = 0;
        for (int i=0; i<records.length; i++) {
            String [] results = records[i].split(",");
            /* result[0] should be Node: [n] */
            String [] data = results[0].split(": ");

            if ("Node".equals(data[0])) {
                if (n == null)
                    return false; /* we shouldn't have a node */

                /* Look for the "Load: " */
                boolean nodeok = false;
                for (int j=0; j<results.length; j++) {
                    int id = results[j].indexOf("Load: ");
                    if (id >= 0) {
                        String res = results[j].substring(6);
                        if (Integer.parseInt(res) > 0) {
                            nodeok = true;
                            break;
                        }
                    }
                }
                /* result[1] should be Name: node_name */
                data = results[1].split(": ");
                for (int j=0; j<nodes.length; j++) {
                    if (nodes[j].equals(data[1])) {
                        n[j] = nodeok; /* found it */
                    }
                }
            }
        }
        if (n == null)
            return true; /* done */
        for (int j=0; j<nodes.length; j++) {
            if (! n[j])
                return false; /* not found */
        }
        return true;
    }
    public static boolean testPort(int port) {
        boolean ret = true;
        Socket s = null;
        try {
            s =  new Socket("localhost", port);
            s.setSoLinger(true, 0);
        } catch (Exception e) {
            System.out.println("Can't connect to " + port);
            ret = false;
        } finally {
            if (s != null) {
                System.out.println("Was connected to " + port);
                try {
                    s.close();
                } catch (Exception e) {
                }
            }
        }
        return ret;
    }
    /* Wait until the node is in the ok status (load>0). */
    static  boolean TestForNodes(ModClusterService pcluster, String [] nodes) {
        int countinfo = 0;
        while ((!Maintest.checkProxyInfo(pcluster, nodes)) && countinfo < 80) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            countinfo++;
        }
        if (countinfo == 80) {
            System.out.println("TestForNodes failed: " + getProxyInfo(pcluster));
            return false;
        } else
            return true;
    }
    /* Wait until we are able to connect to httpd and then until the node is in the ok status (load>0). */
    static  boolean WaitForNodes(ModClusterService pcluster, String [] nodes) {
        if (WaitForHttpd(pcluster, 60) == -1) {
            System.out.println("can't find PING-RSP in proxy response");
            return false;
        }
        return TestForNodes(pcluster, nodes);
    }

    // Wait until we are able to PING httpd.
    // tries maxtries and wait 5 s between retries...
    static int WaitForHttpd(ModClusterService pcluster, int maxtries) {
        String result = null;
        int tries = 0;
        while (result == null && tries<maxtries) {
            result = doProxyPing(pcluster);
            if (result != null) {
                if (Maintest.checkProxyPing(result))
                    break; // Done
                System.out.println("WaitForHttpd (not a ping) failed: " + result);
                return -1; // Failed.
            }
            try {
                Thread me = Thread.currentThread();
                me.sleep(5000);
                tries++;
            } catch (Exception ex) {
                // Ignore
            }
        }
        if (tries == maxtries)
            System.out.println("WaitForHttpd (max) failed: " + result);
        return tries;
    }
    /* Just wait n0 sec: needed a the end of some tests */
    static void waitn() {
        try {
            Thread.sleep(35000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }
}
