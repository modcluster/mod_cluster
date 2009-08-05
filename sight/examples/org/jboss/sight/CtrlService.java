/*
 *  SIGHT - System information gathering hybrid tool
 *
 *  Copyright(c) 2007 Red Hat Middleware, LLC,
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
 */
package org.jboss.sight;

import java.util.Date;
import java.util.Random;
import java.util.EnumSet;

/**
 * Control service example
 *
 * @author Mladen Turk
 */

public class CtrlService {

    private Service svc;
    public CtrlService(String name)
    {
        try {
            ServiceControlManager scm = new ServiceControlManager();
            scm.open(null, EnumSet.of(GenericAccessRights.READ, GenericAccessRights.EXECUTE));
            svc = new Service(scm, name, EnumSet.of(GenericAccessRights.READ, GenericAccessRights.EXECUTE));
            System.out.println("Service: " + svc.DisplayName);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void control(ServiceControl cmd) {
        try {
            System.out.println("State: " + svc.State);
            svc.control(cmd);
            if (cmd == ServiceControl.START) {
                svc.waitFor(ServiceState.RUNNING, 3000);
            }
            else if (cmd == ServiceControl.STOP) {
                svc.waitFor(ServiceState.STOPPED, 3000);
            }
            System.out.println("State: " + svc.State);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
    public static void main(String [] args) {
        try {
            if (args.length < 2) {
                System.err.println("Usage: <start|stop> <name>");
                System.exit(1);
            }
            Library.initialize("");
            CtrlService ls = new CtrlService(args[1]);
            if (args[0].equalsIgnoreCase("start")) {
                ls.control(ServiceControl.START);
            }
            else if (args[0].equalsIgnoreCase("stop")) {
                ls.control(ServiceControl.STOP);
            }
            else if (args[0].equalsIgnoreCase("pause")) {
                ls.control(ServiceControl.PAUSE);
            }
            else if (args[0].equalsIgnoreCase("continue")) {
                ls.control(ServiceControl.CONTINUE);
            }
            else {
                System.err.println("Usage: <start|stop> <name>");
            }
            Library.shutdown();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
 }
