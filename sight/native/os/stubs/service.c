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
 * Service implementation
 *
 */

#include "sight.h"
#include "sight_local.h"
#include "sight_types.h"
#include "sight_private.h"

J_DECLARE_CLAZZ = {
    NULL,
    NULL,
    SIGHT_CLASS_PATH "Service"
};

J_DECLARE_F_ID(0000) = {
    NULL,
    "Name",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0001) = {
    NULL,
    "BinaryPathName",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0002) = {
    NULL,
    "Dependencies",
    "[Ljava/lang/String;"
};

J_DECLARE_F_ID(0003) = {
    NULL,
    "ServiceStartName",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0004) = {
    NULL,
    "DisplayName",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0005) = {
    NULL,
    "Description",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0006) = {
    NULL,
    "LoadOrderGroup",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0007) = {
    NULL,
    "ExitCode",
    "I"
};

J_DECLARE_F_ID(0008) = {
    NULL,
    "ServiceSpecificExitCode",
    "I"
};

J_DECLARE_F_ID(0009) = {
    NULL,
    "ProcessId",
    "I"
};

J_DECLARE_M_ID(0000) = {
    NULL,
    "setState",
    "(I)V"
};

SIGHT_CLASS_LDEF(Service)
{
    if (sight_load_class(_E, &_clazzn))
        return 1;
    J_LOAD_IFIELD(0000);
    J_LOAD_IFIELD(0001);
    J_LOAD_IFIELD(0002);
    J_LOAD_IFIELD(0003);
    J_LOAD_IFIELD(0004);
    J_LOAD_IFIELD(0005);
    J_LOAD_IFIELD(0006);
    J_LOAD_IFIELD(0007);
    J_LOAD_IFIELD(0008);
    J_LOAD_IFIELD(0009);
    J_LOAD_METHOD(0000);

    return 0;
}

SIGHT_CLASS_UDEF(Service)
{
    sight_unload_class(_E, &_clazzn);
}

SIGHT_EXPORT_DECLARE(jint, Service, open0)(SIGHT_STDARGS,
                                           jobject thiz,
                                           jlong instance,
                                           jlong scm,
                                           jstring name,
                                           jint access)
{
    UNREFERENCED_STDARGS;
    UNREFERENCED(thiz);
    UNREFERENCED(instance);
    UNREFERENCED(scm);
    UNREFERENCED(name);
    UNREFERENCED(access);
    return APR_ENOTIMPL;
}

SIGHT_EXPORT_DECLARE(jint, Service, ctrl0)(SIGHT_STDARGS,
                                           jlong instance,
                                           jint cmd)
{
    UNREFERENCED_STDARGS;
    UNREFERENCED(instance);
    UNREFERENCED(cmd);
    return APR_ENOTIMPL;
}

SIGHT_EXPORT_DECLARE(jint, Service, stats0)(SIGHT_STDARGS,
                                            jobject thiz,
                                            jlong instance)
{
    UNREFERENCED_STDARGS;
    UNREFERENCED(thiz);
    UNREFERENCED(instance);
    return APR_ENOTIMPL;
}

SIGHT_EXPORT_DECLARE(jint, Service, wait0)(SIGHT_STDARGS,
                                            jobject thiz,
                                            jlong instance,
                                            jlong timeout,
                                            jint state)
{
    UNREFERENCED_STDARGS;
    UNREFERENCED(thiz);
    UNREFERENCED(instance);
    UNREFERENCED(timeout);
    UNREFERENCED(state);
    return APR_ENOTIMPL;
}

SIGHT_EXPORT_DECLARE(jint, Service, wait1)(SIGHT_STDARGS,
                                            jobject thiz,
                                            jlong instance,
                                            jobject progress,
                                            jlong timeout,
                                            jint state)
{
    UNREFERENCED_STDARGS;
    UNREFERENCED(thiz);
    UNREFERENCED(instance);
    UNREFERENCED(progress);
    UNREFERENCED(timeout);
    UNREFERENCED(state);
    return APR_ENOTIMPL;
}
