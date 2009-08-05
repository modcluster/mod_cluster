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
 * Process windows implementation
 *
 */

#include "sight.h"
#include "sight_local.h"
#include "sight_types.h"
#include "sight_private.h"

/*
 * User
 */

J_DECLARE_CLAZZ = {
    NULL,
    NULL,
    SIGHT_CLASS_PATH "User"
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
    "FullName",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0002) = {
    NULL,
    "Comment",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0003) = {
    NULL,
    "Id",
    "J"
};

J_DECLARE_F_ID(0004) = {
    NULL,
    "Home",
    "Ljava/lang/String;"
};

SIGHT_CLASS_LDEF(User)
{
    if (sight_load_class(_E, &_clazzn))
        return 1;

    J_LOAD_METHOD(0000);
    J_LOAD_IFIELD(0000);
    J_LOAD_IFIELD(0001);
    J_LOAD_IFIELD(0002);
    J_LOAD_IFIELD(0003);
    J_LOAD_IFIELD(0004);

    return 0;
}

SIGHT_CLASS_UDEF(User)
{
    sight_unload_class(_E, &_clazzn);
}

static jobject new_user_class(SIGHT_STDARGS, jlong instance)
{
    if (_clazzn.i && _m0000n.i)
        return (*_E)->NewObject(_E, _clazzn.i, _m0000n.i, (jint)0, instance);
    else
        return NULL;
}

SIGHT_EXPORT_DECLARE(void, User, free0)(SIGHT_STDARGS, jlong instance)
{
    UNREFERENCED_STDARGS;
}


SIGHT_EXPORT_DECLARE(jobjectArray, User, users0)(SIGHT_STDARGS)
{

    UNREFERENCED_STDARGS;
    return NULL;
}

SIGHT_EXPORT_DECLARE(jlong, User, getuser0)(SIGHT_STDARGS,
                                            jobject thiz,
                                            jlong sid)
{

    UNREFERENCED_STDARGS;
    UNREFERENCED(thiz);
    UNREFERENCED(sid);
    return 0;
}

SIGHT_EXPORT_DECLARE(jobjectArray, User, who0)(SIGHT_STDARGS)
{

    UNREFERENCED_STDARGS;
    return NULL;
}
