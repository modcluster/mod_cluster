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
 * Group implementation
 *
 */

#include "sight.h"
#include "sight_local.h"
#include "sight_types.h"
#include "sight_private.h"

/*
 * Group implementation
 */


J_DECLARE_CLAZZ = {
    NULL,
    NULL,
    SIGHT_CLASS_PATH "Group"
};

J_DECLARE_M_ID(0000) = {
    NULL,
    "<init>",
    "(IJ)V"
};

J_DECLARE_F_ID(0000) = {
    NULL,
    "Name",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0001) = {
    NULL,
    "Comment",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0002) = {
    NULL,
    "Id",
    "J"
};

J_DECLARE_F_ID(0003) = {
    NULL,
    "IsLocal",
    "Z"
};

SIGHT_CLASS_LDEF(Group)
{
    if (sight_load_class(_E, &_clazzn))
        return 1;
    J_LOAD_METHOD(0000);
    J_LOAD_IFIELD(0000);
    J_LOAD_IFIELD(0001);
    J_LOAD_IFIELD(0002);
    J_LOAD_IFIELD(0003);

    return 0;
}

SIGHT_CLASS_UDEF(Group)
{
    sight_unload_class(_E, &_clazzn);
}

static jobject new_group_class(SIGHT_STDARGS, jlong instance)
{
    if (_clazzn.i && _m0000n.i)
        return (*_E)->NewObject(_E, _clazzn.i, _m0000n.i, (jint)0, instance);
    else
        return NULL;
}

SIGHT_EXPORT_DECLARE(void, Group, free0)(SIGHT_STDARGS, jlong instance)
{
    UNREFERENCED_STDARGS;
    UNREFERENCED(instance);
}

/* This gives strange results (None)
 */
SIGHT_EXPORT_DECLARE(jobjectArray, Group, ggroups0)(SIGHT_STDARGS)
{
    UNREFERENCED_STDARGS;
    return NULL;
}

SIGHT_EXPORT_DECLARE(jobjectArray, Group, lgroups0)(SIGHT_STDARGS)
{
    UNREFERENCED_STDARGS;
    return NULL;
}

SIGHT_EXPORT_DECLARE(jlong, Group, getlgrp1)(SIGHT_STDARGS,
                                             jobject thiz,
                                             jlong sid)
{
    UNREFERENCED_STDARGS;
    UNREFERENCED(thiz);
    UNREFERENCED(sid);
    return 0;
}

SIGHT_EXPORT_DECLARE(jlong, Group, getlgrp0)(SIGHT_STDARGS,
                                             jobject thiz,
                                             jlong sid)
{
    UNREFERENCED_STDARGS;
    UNREFERENCED(thiz);
    UNREFERENCED(sid);
    return 0;
}
