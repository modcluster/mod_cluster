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
 * Console implementation
 *
 */
#include "sight.h"
#include "sight_local.h"
#include "sight_types.h"
#include "sight_private.h"

SIGHT_EXPORT_DECLARE(jlong, Console, alloc0)(SIGHT_STDARGS)
{
    UNREFERENCED_STDARGS;
    return 0;
}

SIGHT_EXPORT_DECLARE(void, Console, open0)(SIGHT_STDARGS,
                                           jlong instance)
{
    UNREFERENCED_STDARGS;
    UNREFERENCED(instance);
}

SIGHT_EXPORT_DECLARE(jint, Console, attach0)(SIGHT_STDARGS,
                                            jint pid)
{
    UNREFERENCED_STDARGS;
    UNREFERENCED(pid);
    return APR_ENOTIMPL;
}

SIGHT_EXPORT_DECLARE(void, Console, close0)(SIGHT_STDARGS,
                                            jlong instance)
{
    UNREFERENCED_STDARGS;
}

SIGHT_EXPORT_DECLARE(void, Console, echo0)(SIGHT_STDARGS,
                                           jlong instance,
                                           jboolean on)
{
    UNREFERENCED_STDARGS;
    UNREFERENCED(instance);
    UNREFERENCED(on);
}

SIGHT_EXPORT_DECLARE(void, Console, stitle0)(SIGHT_STDARGS,
                                             jstring title)
{
    UNREFERENCED_STDARGS;
    UNREFERENCED(title);
}

SIGHT_EXPORT_DECLARE(jstring, Console, gtitle0)(SIGHT_STDARGS)
{
    UNREFERENCED_O;
    return CSTR_TO_JSTRING("Unknown");
}

SIGHT_EXPORT_DECLARE(void, Console, kill3)(SIGHT_STDARGS)
{

    UNREFERENCED_STDARGS;
}


SIGHT_EXPORT_DECLARE(jstring, Console, gets0)(SIGHT_STDARGS,
                                              jlong instance)
{
    UNREFERENCED_STDARGS;
    UNREFERENCED(instance);
    return NULL;
}

SIGHT_EXPORT_DECLARE(jint, Console, getc0)(SIGHT_STDARGS,
                                           jlong instance)
{
    UNREFERENCED_STDARGS;
    UNREFERENCED(instance);
    return -1;
}

SIGHT_EXPORT_DECLARE(void, Console, putc0)(SIGHT_STDARGS,
                                           jlong instance,
                                           jint ch)
{
    UNREFERENCED_STDARGS;
    UNREFERENCED(instance);
    UNREFERENCED(ch);
}

SIGHT_EXPORT_DECLARE(void, Console, flush0)(SIGHT_STDARGS,
                                            jlong instance)
{
    UNREFERENCED_STDARGS;
    UNREFERENCED(instance);
}

SIGHT_EXPORT_DECLARE(void, Console, puts0)(SIGHT_STDARGS,
                                           jlong instance,
                                           jstring str)
{
    UNREFERENCED_STDARGS;
    UNREFERENCED(instance);
    UNREFERENCED(str);
}


SIGHT_EXPORT_DECLARE(jint, Console, denable0)(SIGHT_STDARGS,
                                              jlong instance)
{
    UNREFERENCED_STDARGS;
    UNREFERENCED(instance);
    return APR_ENOTIMPL;
}

SIGHT_EXPORT_DECLARE(void, Console, ddisable0)(SIGHT_STDARGS,
                                               jlong instance)
{
    UNREFERENCED_STDARGS;
    UNREFERENCED(instance);
}
