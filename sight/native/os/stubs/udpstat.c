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
#include "sight_types.h"

#define SIGHT_WANT_LATE_DLL
#include "sight_private.h"

/*
 * UDP statistics implementation
 */

J_DECLARE_CLAZZ = {
    NULL,
    NULL,
    SIGHT_CLASS_PATH "UdpStatistics"
};

J_DECLARE_F_ID(0000) = {
    NULL,
    "InDatagrams",
    "I"
};

J_DECLARE_F_ID(0001) = {
    NULL,
    "NoPorts",
    "I"
};

J_DECLARE_F_ID(0002) = {
    NULL,
    "InErrors",
    "I"
};

J_DECLARE_F_ID(0003) = {
    NULL,
    "OutDatagrams",
    "I"
};

J_DECLARE_F_ID(0004) = {
    NULL,
    "NumAddrs",
    "I"
};


SIGHT_CLASS_LDEF(UdpStatistics)
{
    if (sight_load_class(_E, &_clazzn))
        return 1;
    J_LOAD_IFIELD(0000);
    J_LOAD_IFIELD(0001);
    J_LOAD_IFIELD(0002);
    J_LOAD_IFIELD(0003);
    J_LOAD_IFIELD(0004);

    return 0;
}

SIGHT_CLASS_UDEF(UdpStatistics)
{
    sight_unload_class(_E, &_clazzn);
}

static const char *eiftype = "Unsupported NetworkAddressFamily type";

/* Initialize volume enumeration */
SIGHT_EXPORT_DECLARE(void, UdpStatistics, info0)(SIGHT_STDARGS,
                                                 jobject thiz,
                                                 jint iftype,
                                                 jlong pool)
{
    apr_status_t rc = APR_ENOTIMPL;
    UNREFERENCED_O;

    if (iftype == 1) {
        /* AF_INET */
        rc = 0;
    }
    else if (iftype == 2) {
        /* AF_INET6 */
        rc = 0;
    }
    else {
        throwOSException(_E, eiftype);
    }
    if (rc) {
        throwAprException(_E, APR_FROM_OS_ERROR(rc));
        return;
    }
}

SIGHT_EXPORT_DECLARE(jlong, UdpStatistics, enum0)(SIGHT_STDARGS,
                                                  jint iftype,
                                                  jlong pool)
{
    apr_status_t rc = APR_ENOTIMPL;
    UNREFERENCED_O;

    if (iftype == 1) {
        /* AF_INET */
        rc = 0;
    }
    else if (iftype == 2) {
        /* AF_INET6 */
        rc = 0;
    }
    else {
        throwOSException(_E, eiftype);
    }
    if (rc) {
        throwAprException(_E, APR_FROM_OS_ERROR(rc));
        return;
    }
}

/* Get the number of entries */
SIGHT_EXPORT_DECLARE(jint, UdpStatistics, enum1)(SIGHT_STDARGS,
                                                 jlong handle)
{
    UNREFERENCED_STDARGS;
    return 0;
}

SIGHT_EXPORT_DECLARE(void, UdpStatistics, enum2)(SIGHT_STDARGS,
                                                 jobject conn,
                                                 jint index,
                                                 jlong handle)
{

}

/* Close TCP conn enumeration */
SIGHT_EXPORT_DECLARE(void, UdpStatistics, enum3)(SIGHT_STDARGS,
                                                 jlong handle)
{
    UNREFERENCED_STDARGS;
}
