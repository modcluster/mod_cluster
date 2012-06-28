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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/*
 * It controls are JBossWeb running via the stdin/stdout.
 */
public class ControlJBossWeb {

    private BufferedReader bufferedreader;
    private PrintStream out;

    public ControlJBossWeb() throws Exception {
        Runtime runtime = Runtime.getRuntime();
        String classpath = System.getProperty("java.class.path");
        Process proc = runtime.exec("java -cp " + classpath + " org.jboss.mod_cluster.ProcJBossWeb");
        InputStreamReader input = new InputStreamReader(proc.getInputStream());
        bufferedreader = new BufferedReader(input);
        out = new PrintStream(proc.getOutputStream());
    }
    private boolean isResponseOK() {
        try {
            String line;
            while ((line = bufferedreader.readLine()) != null) {
                if (line.compareToIgnoreCase("OK") == 0)
                    return true;
                if (line.startsWith("FAILED")) {
                    System.out.println(line);
                    while ((line = bufferedreader.readLine()) != null) {
                        System.out.println(line);
                    }
                    return false;
                }
                System.out.println(line);
            }
        } catch (IOException ex) {
            return false;
        }
        return false;
    }
    public void stop() throws IOException {
        out.println("stop");
        out.flush();
        if (!isResponseOK())
            throw new IOException("response is not OK");
    }
    public void start() throws IOException {
        out.println("start");
        out.flush();
        if (!isResponseOK())
            throw new IOException("response is not OK");
    }
    public void exit() throws IOException {
        out.println("exit");
        out.flush();
        if (!isResponseOK())
            throw new IOException("response is not OK");
    }
    public void newJBossWeb(String node, String host) throws IOException {
        out.println("new " + node + " " + host);
        out.flush();
        if (!isResponseOK())
            throw new IOException("response is not OK");
    }
    public void addConnector(int port) throws IOException {
        out.println("addConnector " + port);
        out.flush();
        if (!isResponseOK())
            throw new IOException("response is not OK");
    }
    public void addService() throws IOException {
        out.println("addService");
        out.flush();
        if (!isResponseOK())
            throw new IOException("response is not OK");
    }
    public void addLifecycleListener(String ip, int port) throws IOException {
        out.println("addLifecycleListener " + ip + " " + port);
        out.flush();
        if (!isResponseOK())
            throw new IOException("response is not OK");
    }
    public String getProxyInfo() throws IOException {
        out.println("getProxyInfo");
        out.flush();
        return(readProxyCommandResponse(true));
    }
    public String getProxyAddress() throws IOException {
        out.println("getProxyAddress");
        out.flush();
        return(readProxyCommandResponse(false));
    }
    public String readProxyCommandResponse(boolean concat) throws IOException {
        String line = bufferedreader.readLine();

        /* throw again trace and other */
        int l = -1;
       
        while (l == -1) { 
            if (line.startsWith("FAILED")) {
                l = 1024; /* Enough ? */
                break;
            } else {
                try {
                    l = Integer.parseInt(line);
                } catch(Exception e) {
                    l = -1;
                    System.out.println(line);
                }
            }
            if (l >= 0)
                break;
            line = bufferedreader.readLine();
        }
        String result = "";
        int len = l;
        if (!concat)
            len = l-2;
        while (result.length() < len && (line = bufferedreader.readLine()) != null) {
            result = result.concat(line);
            if (concat)
                result = result.concat("\n");
        }
        if (l == 0)
            return null;
        return result;
    }
}
