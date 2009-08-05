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
 * WMI windows utilities
 *
 */

#include "sight.h"
#include "sight_local.h"
#include "sight_types.h"
#include "sight_private.h"


/*
 * WMI and VARIANT API
 */

J_DECLARE_CLAZZ = {
    NULL,
    NULL,
    SIGHT_PLATFORM_CLASS_PATH "VARIANT"
};

J_DECLARE_M_ID(0000) = {
    NULL,
    "<init>",
    "(J)V"
};

SIGHT_CLASS_LDEF(VARIANT)
{
    if (sight_load_class(_E, &_clazzn))
        return 1;
    J_LOAD_METHOD(0000);
    return 0;
}

SIGHT_CLASS_UDEF(VARIANT)
{
    sight_unload_class(_E, &_clazzn);
}

jobject new_variant_class(SIGHT_STDARGS, jlong instance)
{
    if (_clazzn.i && _m0000n.i)
        return (*_E)->NewObject(_E, _clazzn.i, _m0000n.i, instance);
    else
        return NULL;
}

SIGHT_PLATFORM_DECLARE(jlong, WMI, create0)(SIGHT_STDARGS,
                                            jstring ns)
{
    void *wmi;
    SIGHT_ALLOC_WSTRING(ns);

    UNREFERENCED_O;
    wmi = wmi_intialize(_E, J2W(ns), JWL(ns));

    SIGHT_FREE_WSTRING(ns);
    return P2J(wmi);
}

SIGHT_PLATFORM_DECLARE(void, WMI, term0)(SIGHT_STDARGS,
                                         jlong wmi)
{
    UNREFERENCED_STDARGS;
    wmi_terminate(J2P(wmi, void *));
}

SIGHT_PLATFORM_DECLARE(jint, WMIQuery, query0)(SIGHT_STDARGS,
                                               jlong wmi,
                                               jstring lang,
                                               jstring query)
{
    int rc;
    SIGHT_ALLOC_WSTRING(lang);
    SIGHT_ALLOC_WSTRING(query);

    UNREFERENCED_O;
    rc = wmi_query(J2P(wmi, void *), J2W(lang), JWL(lang),
                   J2W(query), JWL(query));

    SIGHT_FREE_WSTRING(lang);
    SIGHT_FREE_WSTRING(query);
    return rc;
}

SIGHT_PLATFORM_DECLARE(jint, WMIQuery, next0)(SIGHT_STDARGS,
                                              jlong wmi)
{
    int rc;

    UNREFERENCED_STDARGS;
    rc = wmi_query_next(J2P(wmi, void *));

    return rc;
}

SIGHT_PLATFORM_DECLARE(jint, WMIQuery, skip0)(SIGHT_STDARGS,
                                              jlong wmi, jint count)
{
    int rc;

    UNREFERENCED_STDARGS;
    rc = wmi_query_skip(J2P(wmi, void *), count);

    return rc;
}

SIGHT_PLATFORM_DECLARE(jint, WMIQuery, reset0)(SIGHT_STDARGS,
                                               jlong wmi)
{
    int rc;

    UNREFERENCED_STDARGS;
    rc = wmi_query_reset(J2P(wmi, void *));

    return rc;
}

SIGHT_PLATFORM_DECLARE(jobject, WMIQuery, getv0)(SIGHT_STDARGS,
                                                 jlong wmi,
                                                 jstring prop)
{
    void *v;
    jobject o;
    SIGHT_ALLOC_WSTRING(prop);

    UNREFERENCED_O;
    v = wmi_query_get(_E, J2P(wmi, void *), J2W(prop), JWL(prop));
    o = new_variant_class(_E, _O, P2J(v));

    SIGHT_FREE_WSTRING(prop);
    return o;
}
