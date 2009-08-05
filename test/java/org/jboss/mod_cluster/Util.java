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

import java.lang.Exception;
import java.net.Socket;
import java.io.PrintWriter;
import java.io.InputStream;

public class Util {

    public static void stopNode(int port) {
        System.out.println("Stopping: " + port);
        try {
            Socket socket = new Socket("localhost", port);
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            InputStream in = socket.getInputStream();
            out.print("SHUTDOWN");
            out.flush();
            in.read();
            socket.close();
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
        System.out.println("Stopped: " + port);
    }
    public static void stopAll() {
        stopNode(8005);
        stopNode(8006);
        stopNode(8007);
        stopNode(8008);
    }
    public static void main(String[] args) {
        try {
  
            Thread.currentThread().sleep(5000);
        }
        catch(InterruptedException ie){
        }
        stopAll();
    }
}
