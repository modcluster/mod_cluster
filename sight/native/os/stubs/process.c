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
 * Process
 */

J_DECLARE_CLAZZ = {
    NULL,
    NULL,
    SIGHT_CLASS_PATH "Process"
};

J_DECLARE_F_ID(0000) = {
    NULL,
    "ParentId",
    "I"
};

J_DECLARE_F_ID(0001) = {
    NULL,
    "Name",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0002) = {
    NULL,
    "BaseName",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0003) = {
    NULL,
    "Arguments",
    "[Ljava/lang/String;"
};

J_DECLARE_F_ID(0004) = {
    NULL,
    "Environment",
    "[Ljava/lang/String;"
};

J_DECLARE_F_ID(0005) = {
    NULL,
    "ThreadCount",
    "I"
};

J_DECLARE_F_ID(0006) = {
    NULL,
    "ReadOperationCount",
    "J"
};

J_DECLARE_F_ID(0007) = {
    NULL,
    "WriteOperationCount",
    "J"
};

J_DECLARE_F_ID(0008) = {
    NULL,
    "OtherOperationCount",
    "J"
};

J_DECLARE_F_ID(0009) = {
    NULL,
    "ReadTransferCount",
    "J"
};

J_DECLARE_F_ID(0010) = {
    NULL,
    "WriteTransferCount",
    "J"
};

J_DECLARE_F_ID(0011) = {
    NULL,
    "OtherTransferCount",
    "J"
};

J_DECLARE_F_ID(0012) = {
    NULL,
    "CreateTime",
    "J"
};

J_DECLARE_F_ID(0013) = {
    NULL,
    "ExitTime",
    "J"
};

J_DECLARE_F_ID(0014) = {
    NULL,
    "InKernelTime",
    "J"
};

J_DECLARE_F_ID(0015) = {
    NULL,
    "InUserTime",
    "J"
};

J_DECLARE_F_ID(0016) = {
    NULL,
    "UserId",
    "J"
};

J_DECLARE_F_ID(0017) = {
    NULL,
    "GroupId",
    "J"
};

J_DECLARE_F_ID(0018) = {
    NULL,
    "CurrentWorkingDirectory",
    "Ljava/lang/String;"
};

J_DECLARE_M_ID(0000) = {
    NULL,
    "setState",
    "(I)V"
};

SIGHT_CLASS_LDEF(Process)
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
    J_LOAD_IFIELD(0010);
    J_LOAD_IFIELD(0011);
    J_LOAD_IFIELD(0012);
    J_LOAD_IFIELD(0013);
    J_LOAD_IFIELD(0014);
    J_LOAD_IFIELD(0015);
    J_LOAD_IFIELD(0016);
    J_LOAD_IFIELD(0017);
    J_LOAD_IFIELD(0018);
    J_LOAD_METHOD(0000);

    return 0;
}

SIGHT_CLASS_UDEF(Process)
{
    sight_unload_class(_E, &_clazzn);
}

SIGHT_EXPORT_DECLARE(jint, Process, getpid0)(SIGHT_STDARGS)
{
    UNREFERENCED_STDARGS;
    return (jint)getpid();
}

SIGHT_EXPORT_DECLARE(jintArray, Process, getpids0)(SIGHT_STDARGS)
{
    UNREFERENCED_STDARGS;
    UNREFERENCED(type);
    return NULL;
}

SIGHT_EXPORT_DECLARE(jint, Process, alloc0)(SIGHT_STDARGS,
                                            jobject thiz,
                                            jlong instance,
                                            jint pid)
{
    UNREFERENCED_STDARGS;
    UNREFERENCED(thiz);
    UNREFERENCED(instance);
    UNREFERENCED(pid);
    return APR_ENOTIMPL;
}

SIGHT_EXPORT_DECLARE(jint, Process, term0)(SIGHT_STDARGS, jint pid,
                                           jint signum)
{
    UNREFERENCED_STDARGS;
    UNREFERENCED(signum);
    UNREFERENCED(pid);
    return APR_ENOTIMPL;
}

SIGHT_EXPORT_DECLARE(jint, Process, signal0)(SIGHT_STDARGS, jint pid,
                                             jint signal)
{

    UNREFERENCED_STDARGS;
    UNREFERENCED(pid);
    UNREFERENCED(signal);
    return APR_ENOTIMPL;
}
