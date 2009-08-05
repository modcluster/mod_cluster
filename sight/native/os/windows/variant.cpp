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
 * VARIANT type wrapper
 *
 */

#include "sight.h"
#include "sight_local.h"
#include "sight_types.h"
#include "sight_private.h"

#include <comdef.h>

extern "C"
void *alloc_variant()
{
    VARIANT *v = (VARIANT *)calloc(1, sizeof(VARIANT));
    VariantInit(v);
    return (void *)v;
}

extern "C"
void free_variant(void *var)
{
    VARIANT *v = (VARIANT *)var;
    if (v) {
        VariantClear(v);
        free(v);
    }
}

extern "C"
SIGHT_PLATFORM_DECLARE(jlong, VARIANT, alloc0)(SIGHT_STDARGS)
{
    VARIANT *v = (VARIANT *)calloc(1, sizeof(VARIANT));

    UNREFERENCED_STDARGS;
    VariantInit(v);
    return P2J(v);
}

extern "C"
SIGHT_PLATFORM_DECLARE(void, VARIANT, free0)(SIGHT_STDARGS, jlong vp)
{
    VARIANT *v = J2P(vp, VARIANT *);

    UNREFERENCED_STDARGS;
    if (v) {
        VariantClear(v);
        free(v);
    }
}

extern "C"
SIGHT_PLATFORM_DECLARE(jint, VARIANT, type0)(SIGHT_STDARGS, jlong vp)
{
    VARIANT *v = J2P(vp, VARIANT *);

    UNREFERENCED_STDARGS;
    if (v)
        return v->vt;
    else
        return -1;
}

extern "C"
SIGHT_PLATFORM_DECLARE(jboolean, VARIANT, getvz)(SIGHT_STDARGS, jlong vp)
{
    VARIANT *v = J2P(vp, VARIANT *);

    UNREFERENCED_STDARGS;
    if (v && v->vt == VT_BOOL)
        return v->boolVal ? JNI_TRUE : JNI_FALSE;
    else {
        return JNI_FALSE;
    }
}

extern "C"
SIGHT_PLATFORM_DECLARE(jstring, VARIANT, getvs)(SIGHT_STDARGS, jlong vp)
{
    VARIANT *v = J2P(vp, VARIANT *);

    UNREFERENCED_O;
    if (v && v->vt == VT_BSTR)
        return _E->NewString(v->bstrVal, SysStringLen(v->bstrVal));
    else {
        return NULL;
    }
}

extern "C"
SIGHT_PLATFORM_DECLARE(jlong, VARIANT, getvj)(SIGHT_STDARGS, jlong vp)
{
    VARIANT *v = J2P(vp, VARIANT *);

    UNREFERENCED_O;
    if (!v) {
        return 0;
    }
    switch (v->vt) {
        case VT_INT:
            return v->intVal;
        case VT_UINT:
            return v->uintVal;
        case VT_I2:
            return v->iVal;
        case VT_I4:
            return v->lVal;
        case VT_I8:
            return v->llVal;
        case VT_UI2:
            return v->uiVal;
        case VT_UI4:
            return v->ulVal;
        case VT_UI8:
            return v->ullVal;
        case VT_R4:
            return (jlong)v->fltVal;
        case VT_R8:
            return (jlong)v->fltVal;
        case VT_BOOL:
            return v->boolVal;
    }
    return 0;
}

extern "C"
SIGHT_PLATFORM_DECLARE(jdouble, VARIANT, getvd)(SIGHT_STDARGS, jlong vp)
{
    VARIANT *v = J2P(vp, VARIANT *);

    UNREFERENCED_O;
    if (!v) {
        return 0.0;
    }
    switch (v->vt) {
        case VT_INT:
            return v->intVal;
        case VT_UINT:
            return v->uintVal;
        case VT_I2:
            return v->iVal;
        case VT_I4:
            return v->lVal;
        case VT_I8:
            return (jdouble)v->llVal;
        case VT_UI2:
            return v->uiVal;
        case VT_UI4:
            return v->ulVal;
        case VT_UI8:
            return (jdouble)((__int64)v->ullVal);
        case VT_R4:
            return v->fltVal;
        case VT_R8:
            return v->fltVal;
        case VT_BOOL:
            return v->boolVal;
    }
    return 0.0;
}
