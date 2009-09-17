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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.catalina.startup.Embedded;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Context;
import org.apache.catalina.*;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.*;
import org.apache.catalina.startup.HostConfig;

import org.apache.catalina.LifecycleListener;

public class ProcJBossWeb {

    StandardServer server = null;
    JBossWeb service = null;
    LifecycleListener cluster = null;

    static InputStreamReader input;
    static BufferedReader bufferedreader;

    public static void main(String[] args) {
        input = new InputStreamReader(System.in);
        bufferedreader = new BufferedReader(input);

        ProcJBossWeb proc = new ProcJBossWeb();
        proc.start();
    }
    public void start() {

        server = Maintest.getServer();

        ServerThread wait = null;
        try {
            String line;
            while ((line = bufferedreader.readLine()) != null) {
                /* Process a command line */
                String cmd[] = line.split(" ");
                if (cmd[0].compareToIgnoreCase("exit") == 0) {
                    System.out.println("OK");
                    System.out.flush();
                    System.exit(0);
                } else if (cmd[0].compareToIgnoreCase("new") == 0) {
                    service = new JBossWeb(cmd[1], cmd[2]);
                } else if (cmd[0].compareToIgnoreCase("addConnector") == 0) {
                    service.addConnector(Integer.parseInt(cmd[1]));
                } else if (cmd[0].compareToIgnoreCase("addService")== 0) {
                    server.addService(service);
                } else if (cmd[0].compareToIgnoreCase("addLifecycleListener") == 0) {
                    cluster = Maintest.createClusterListener(cmd[1], Integer.parseInt(cmd[2]), false);
                    server.addLifecycleListener(cluster);
                } else if (cmd[0].compareToIgnoreCase("start") == 0) {
                    wait = new ServerThread(3000, server);
                    wait.start();
                } else if (cmd[0].compareToIgnoreCase("stop") == 0) {
                    wait.stopit();
                    wait.join();
                    server.removeService(service);
                    server.removeLifecycleListener(cluster);
                } else if (cmd[0].compareToIgnoreCase("getProxyInfo") == 0) {
                    String result = Maintest.getProxyInfo(cluster);
                    if (result == null) {
                        System.out.println("0");
                        System.out.println();
                    } else {
                        System.out.println(result.length());
                        System.out.println(result);
                    }
                    continue;
                }
                /* Done */
                System.out.println("OK");
                System.out.flush();
            }   
        } catch (Exception ex) {
            System.out.println("FAILED " + ex);
            ex.printStackTrace(System.out);
            System.out.flush();
            System.exit(1);
        }
    }
}
