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
 * @author Mladen Turk
 *
 */

#include "sight.h"
#include "sight_local.h"

SIGHT_EXPORT_DECLARE(jboolean, OS, is)(SIGHT_STDARGS, jint type)
{
    UNREFERENCED_STDARGS;
    if (type == SIGHT_OS_UNIX)
        return JNI_TRUE;
    else
        return JNI_FALSE;
}

SIGHT_EXPORT_DECLARE(jstring, OS, getSysname)(SIGHT_STDARGS)
{
    return CSTR_TO_JSTRING("Unix");
}

SIGHT_EXPORT_DECLARE(jstring, OS, getVersion)(SIGHT_STDARGS)
{
    retun NULL;
}

SIGHT_EXPORT_DECLARE(jstring, OS, getRelease)(SIGHT_STDARGS)
{
    retun NULL;
}

SIGHT_EXPORT_DECLARE(jstring, OS, getMachine)(SIGHT_STDARGS)
{
    retun NULL;
}

SIGHT_EXPORT_DECLARE(jstring, OS, getNodename)(SIGHT_STDARGS)
{
    retun NULL;
}
