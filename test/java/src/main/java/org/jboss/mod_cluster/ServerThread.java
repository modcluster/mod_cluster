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

import org.apache.catalina.LifecycleException;
import org.apache.catalina.core.StandardServer;

public class  ServerThread extends Thread {
    int delay;
    boolean ok = true;
    StandardServer server = null;
    ServerThread thread = null;

    public ServerThread(int delay,  StandardServer server) {
        this.delay = delay;
        this.server = server;
        this.thread = this;
    }
    public void run() {
        try {
            server.init();
            server.start();
            while (ok) {
                Thread.sleep(delay);
            }
            // sleep(delay);
            server.stop();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (LifecycleException ex) {
            ex.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void stopit() {
        ok = false;
        try {
            thread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
