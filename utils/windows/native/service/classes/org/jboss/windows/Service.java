/*
 *  JBOSSSCH - JBoss Service helper
 *
 *  Copyright(c) 2006 Red Hat Middleware, LLC,
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
 * @author Mladen Turk
 */

package org.jboss.windows;
import java.lang.reflect.*;

/**
 * Windows sservice helper class.
 */
final class Service
{
    /**
     * This is where the magic begins.
     * @param args    The command line arguments.
     */
    public static void main(final String[] args)
        throws Exception
    {
        try {
            SignalHelper.initialize(null);
        }
        catch(Exception x) {
            // Ignore all native initialization exceptions
        }
        try {
            // Presume we have at least one argument
            // (org.jboss.Main)
            Class  mainClass   = Class.forName(args[0]);
            Object mainObject  = mainClass.newInstance();

            Class paramTypes[] = new Class[1];
            paramTypes[0]      = Class.forName("[Ljava.lang.String;");
            Object argList[]   = new Object[1];
            String[] cmdArgs   = new String[args.length - 1];
            for (int i = 0; i < cmdArgs.length; i++)
                cmdArgs[i] = args[i + 1];
            argList[0] = cmdArgs;
            Method mainMethod = mainClass.getMethod("main", paramTypes);
            // Invoke the main method from args[0]
            mainMethod.invoke(mainObject, argList);
        }
        catch (Throwable e) {
            // Perhaps we should throw some errors here, but they'll be
            // printed anyhow
           System.err.println(e);
        }
    }
}
