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

import org.jboss.sight.Error;
import org.jboss.sight.OS;
import org.jboss.sight.OperatingSystemException;
import org.jboss.sight.UnsupportedOperatingSystemException;

/**
 * Windows WMIQuery support
 *
 * @author Mladen Turk
 */

public class WMIQuery extends WMI {

    private static native int       query0(long instance, String lang,
                                           String qry);
    private static native int       next0(long instance);
    private static native int       skip0(long instance, int count);
    private static native int       reset0(long instance);
    private static native VARIANT   getv0(long instance, String prop);

    public WMIQuery()
        throws OperatingSystemException, UnsupportedOperatingSystemException
    {
        super();
    }

    public WMIQuery(String nameSpace)
        throws OperatingSystemException, UnsupportedOperatingSystemException
    {
        super(nameSpace);
    }

    public WMIQuery(String strQueryLanguage, String strQuery)
        throws OperatingSystemException, UnsupportedOperatingSystemException
    {
        super();
        ExecQuery(strQueryLanguage, strQuery);
    }

    public WMIQuery(String nameSpace, String strQueryLanguage, String strQuery)
        throws OperatingSystemException, UnsupportedOperatingSystemException
    {
        super(nameSpace);
        ExecQuery(strQueryLanguage, strQuery);
    }

    public boolean ExecQuery(String strQueryLanguage, String strQuery)
    {
        int rv = query0(INSTANCE, strQueryLanguage, strQuery);
        if (rv == Error.APR_SUCCESS)
            return true;
        else {

            return false;
        }
    }

    public boolean Next()
    {
        int rv = next0(INSTANCE);
        if (rv == Error.APR_SUCCESS)
            return true;
        else if (rv == Error.APR_EOF)
            return false;
        else {

            return false;
        }
    }

    public boolean Skip(int count)
    {
        int rv = skip0(INSTANCE, count);
        if (rv == Error.APR_SUCCESS)
            return true;
        else if (rv == Error.APR_EOF)
            return false;
        else {

            return false;
        }
    }

    public boolean Reset()
    {
        int rv = reset0(INSTANCE);
        if (rv == Error.APR_SUCCESS)
            return true;
        else if (rv == Error.APR_EOF)
            return false;
        else {

            return false;
        }
    }

    public VARIANT Get(String szName)
    {
        return getv0(INSTANCE, szName);
    }

}
