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

/**
 * Windows signal helper class.
 */
final class SignalHelper
{

    private static boolean isLoaded = false;

    private SignalHelper()
    {
        // Disable creation.
    }

    public static boolean onSignal(int signum)
    {
        // TODO: Write Reflection Callback handler
        System.out.println("Signal " + signum + " raised.");
        if (signum == 9) {
            System.exit(9);    
        }
        return true;
    }

    private static native boolean init(String serviceName)
        throws Exception;

    private static native void term()
        throws Exception;


    public static synchronized void initialize(String serviceName)
        throws Exception
    {
        if (!isLoaded) {
            try {
                System.loadLibrary("jbosssch");
            }
            catch(UnsatisfiedLinkError e) {
                // In case the library is not present fail
                return;
            }
            isLoaded = init(serviceName);
        }
    }

    public static synchronized void terminate()
        throws Exception
    {
        if (isLoaded) {
            isLoaded = false;
            term();
        }
    }    
}
