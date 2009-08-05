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

package org.jboss.sight.platform.windows;

/**
 * An application can use handles to these keys as entry points to the
 * registry. These handles are valid for all implementations of the
 * registry, although the use of the handles may vary from platform to
 * platform. In addition, other predefined handles have been defined
 * for specific platforms. The following are handles to the predefined
 * keys.
 */
public enum HKEY
{
    /** Registry entries subordinate to this key define types (or classes)
     * of documents and the properties associated with those types. Shell
     * and COM applications use the information stored under this key.
     * For more information, see HKEY_CLASSES_ROOT Key
     * <BR/>
     * This key also provides backward compatibility with the Windows 3.1
     * registration database by storing information for DDE and OLE support.
     * File viewers and user interface extensions store their OLE class
     * identifiers in HKEY_CLASSES_ROOT, and in-process servers are
     * registered in this key.
     * <BR/>
     * This handle should not be used in a service or an application that
     * impersonates different users.
     */
    CLASSES_ROOT(   1),

    /** Contains information about the current hardware profile of the
     * local computer system. The information under HKEY_CURRENT_CONFIG
     * describes only the differences between the current hardware
     * configuration and the standard configuration. Information about
     * the standard hardware configuration is stored under the Software
     * and System keys of HKEY_LOCAL_MACHINE.
     */
    CURRENT_CONFIG( 2),

    /** Registry entries subordinate to this key define the preferences of
     * the current user. These preferences include the settings of
     * environment variables, data about program groups, colors, printers,
     * network connections, and application preferences. This key makes it
     * easier to establish the current user's settings; the key maps to the
     * current user's branch in HKEY_USERS.
     */
    CURRENT_USER(   3),

    /** Registry entries subordinate to this key define the physical state
     * of the computer, including data about the bus type, system memory,
     * and installed hardware and software. It contains subkeys that hold
     * current configuration data, including Plug and Play information
     * (the Enum branch, which includes a complete list of all hardware that
     * has ever been on the system), network logon preferences, network
     * security information, software-related information (such as server
     * names and the location of the server), and other system information.
     */
    LOCAL_MACHINE(  4),

    /** Registry entries subordinate to this key define the default user
     * configuration for new users on the local computer and the user
     * configuration for the current user.
     */
    USERS (         5);


    private int value;
    private HKEY(int v)
    {
        value = v;
    }

    public int valueOf()
    {
        return value;
    }

    public static HKEY valueOf(int value)
    {
        for (HKEY e : values()) {
            if (e.value == value)
                return e;
        }
        throw new IllegalArgumentException("Invalid initializer: " + value);
    }

}
