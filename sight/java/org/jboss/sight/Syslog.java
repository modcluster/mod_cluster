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

/**
 * Native OS System Log support.
 * <br />
 * @author Mladen Turk
 *
 */
public class Syslog
{
    private static native void      init0(String domain);
    private static native void      close0();
    private static native void      log0(int level, String msg);

    private static boolean isOn = false;
    private boolean isOwner     = false;

    public Syslog()
    {
        if (!isOn) {
            isOn    = true;
            isOwner = true;
            init0(null);
        }
    }

    public Syslog(String domain)
    {
        if (!isOn) {
            isOn    = true;
            isOwner = true;
            init0(domain);
        }
    }

    public void close()
    {
        if (isOwner) {
            isOn = false;
            close0();
        }
    }

    /**
     * Object finalize callback.
     * Called by the garbage collector on an object when garbage
     * collection determines that there are no more references to the object.
     */
    protected final void finalize()
    {
        close();
    }

    /**
     *
     */
    public static void log(SyslogLevel level, String msg)
    {
        if (isOn) {
            log0(level.valueOf(), msg);
        }
    }

}
