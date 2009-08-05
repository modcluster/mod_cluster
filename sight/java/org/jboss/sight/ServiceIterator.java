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

import java.util.Iterator;

/**
 * Process Iterator
 *
 * @author Mladen Turk
 *
 */

public class ServiceIterator implements Iterator<Service>, Iterable<Service>
{

    private String[]        array;
    private int             pos = 0;
    private int             len = 0;
    ServiceControlManager   scm;

    protected ServiceIterator(ServiceControlManager s, String[] services)
    {
        scm   = s;
        array = services;
        if (array != null)
            len = array.length;
    }

    public boolean hasNext()
    {
        if (pos < len)
            return true;
        else
            return false;
    }

    public Iterator<Service> iterator()
    {
         return this;
    }

    public Service next()
    {
        if (pos < len) {
            try {
                return new Service(scm, array[pos++]);
            }
            catch (Exception e) {
                return null;
            }
        }
        else
            return null;
    }

    public void remove()
    {
        throw new UnsupportedOperationException();
    }

}
