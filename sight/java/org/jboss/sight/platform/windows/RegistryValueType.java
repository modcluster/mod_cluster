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
 * A registry value can store data in various formats. When you store data
 * under a registry value, for instance by calling the RegSetValueEx function,
 * you can specify one of the following values to indicate the type of data
 * being stored. When you retrieve a registry value, functions such as
 * RegQueryValueEx use these values to indicate the type of data retrieved.
 */
public enum RegistryValueType
{
    /**
     * Unknown or unsupported Registry value type.
     */
    UNKNOWN(    0),

    /**
     * Binary data in any form.
     */
    BINARY(     1),

    /**
     * A 32-bit number.
     */
    DWORD(      2),

    /**
     * Null-terminated string that contains unexpanded references to
     * environment variables (for example, "%PATH%").
     */
    EXPAND_SZ(  3),

    /**
     * Array of null-terminated strings, terminated by two null characters.
     */
    MULTI_SZ(   4),

    /**
     * A 64-bit number.
     */
    QWORD(      5),

    /**
     * Null-terminated string
     */
    SZ(         6);

    private int value;
    private RegistryValueType(int v)
    {
        value = v;
    }

    public int valueOf()
    {
        return value;
    }

    public static RegistryValueType valueOf(int value)
    {
        for (RegistryValueType e : values()) {
            if (e.value == value)
                return e;
        }
        return UNKNOWN;
    }

}
