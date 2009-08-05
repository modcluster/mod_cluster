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

/**
 * Service Control Manager implementation
 *
 */

#include "sight.h"
#include "sight_local.h"
#include "sight_types.h"
#include "sight_private.h"

SIGHT_EXPORT_DECLARE(jint, ServiceControlManager, open0)(SIGHT_STDARGS,
                                                         jlong instance,
                                                         jstring database,
                                                         jint mode)
{
    UNREFERENCED_STDARGS;
    UNREFERENCED(instance);
    UNREFERENCED(database);
    UNREFERENCED(mode);
    return APR_ENOTIMPL;
}

SIGHT_EXPORT_DECLARE(void, ServiceControlManager, close0)(SIGHT_STDARGS,
                                                          jlong instance)
{
    UNREFERENCED_STDARGS;
    UNREFERENCED(instance);
}

SIGHT_EXPORT_DECLARE(jobjectArray, ServiceControlManager,
                     enum0)(SIGHT_STDARGS, jlong instance,
                            jint drivers, jint what)
{
    UNREFERENCED_STDARGS;
    UNREFERENCED(instance);
    UNREFERENCED(drivers);
    UNREFERENCED(what);
    return NULL;
}
