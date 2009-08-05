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

#define SIGHT_WANT_LATE_DLL
#include "sight_private.h"

/*
 * Process
 */

J_DECLARE_CLAZZ = {
    NULL,
    NULL,
    SIGHT_CLASS_PATH "Module"
};

J_DECLARE_M_ID(0000) = {
    NULL,
    "<init>",
    "(II)V"
};


J_DECLARE_F_ID(0000) = {
    NULL,
    "Name",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0001) = {
    NULL,
    "BaseName",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0002) = {
    NULL,
    "BaseAddress",
    "J"
};

J_DECLARE_F_ID(0003) = {
    NULL,
    "Size",
    "J"
};


SIGHT_CLASS_LDEF(Module)
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

SIGHT_CLASS_UDEF(Module)
{
    sight_unload_class(_E, &_clazzn);
}

static jobject new_module_class(SIGHT_STDARGS, jint pid, jint id)
{
    if (_clazzn.i && _m0000n.i)
        return (*_E)->NewObject(_E, _clazzn.i, _m0000n.i, pid, id, NULL);
    else
        return NULL;
}


SIGHT_EXPORT_DECLARE(jobjectArray, Module, enum0)(SIGHT_STDARGS,
                                                  jint pid)
{

    HANDLE hModuleSnap = INVALID_HANDLE_VALUE;
    MODULEENTRY32W me32;
    jsize j = 0, nmods = 0;
    jobjectArray mods = NULL;

    hModuleSnap = CreateToolhelp32Snapshot(TH32CS_SNAPMODULE, pid);

    if (IS_INVALID_HANDLE(hModuleSnap)) {
        throwAprException(_E, apr_get_os_error());
        return NULL;
    }

    me32.dwSize = sizeof(MODULEENTRY32W);

    /* Determine the number of modules */
    if (!Module32FirstW(hModuleSnap, &me32)) {
        throwAprException(_E, apr_get_os_error());
        goto cleanup;
    }
    do {
        nmods++;
    } while (Module32NextW(hModuleSnap, &me32));

    mods = (*_E)->NewObjectArray(_E, nmods, _clazzn.a, NULL);
    if (!mods || (*_E)->ExceptionCheck(_E)) {
        goto cleanup;
    }

    if (!Module32FirstW(hModuleSnap, &me32)) {
        throwAprException(_E, apr_get_os_error());
        mods = NULL;
        goto cleanup;
    }
    do {
        jobject m = new_module_class(_E, _O, pid, j);
        if (!m || (*_E)->ExceptionCheck(_E)) {
            mods = NULL;
            goto cleanup;
        }

        SET_IFIELD_W(0000, m, me32.szModule);
        SET_IFIELD_W(0001, m, me32.szExePath);
        SET_IFIELD_J(0002, m, P2J(me32.modBaseAddr));
        SET_IFIELD_J(0003, m, me32.modBaseSize);

        (*_E)->SetObjectArrayElement(_E, mods, j++, m);
        (*_E)->DeleteLocalRef(_E, m);
        if (j > nmods)
            break;
    } while (Module32NextW(hModuleSnap, &me32));

cleanup:
    CloseHandle(hModuleSnap);
    return mods;
}
