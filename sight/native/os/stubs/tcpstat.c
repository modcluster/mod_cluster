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
 * TCP statistics implementation
 */

J_DECLARE_CLAZZ = {
    NULL,
    NULL,
    SIGHT_CLASS_PATH "TcpStatistics"
};

J_DECLARE_F_ID(0000) = {
    NULL,
    "RtoMin",
    "I"
};

J_DECLARE_F_ID(0001) = {
    NULL,
    "RtoMax",
    "I"
};

J_DECLARE_F_ID(0002) = {
    NULL,
    "MaxConn",
    "I"
};

J_DECLARE_F_ID(0003) = {
    NULL,
    "ActiveOpens",
    "I"
};

J_DECLARE_F_ID(0004) = {
    NULL,
    "PassiveOpens",
    "I"
};

J_DECLARE_F_ID(0005) = {
    NULL,
    "AttemptFails",
    "I"
};

J_DECLARE_F_ID(0006) = {
    NULL,
    "EstabResets",
    "I"
};

J_DECLARE_F_ID(0007) = {
    NULL,
    "CurrEstab",
    "I"
};

J_DECLARE_F_ID(0008) = {
    NULL,
    "InSegs",
    "I"
};

J_DECLARE_F_ID(0009) = {
    NULL,
    "OutSegs",
    "I"
};

J_DECLARE_F_ID(0010) = {
    NULL,
    "RetransSegs",
    "I"
};

J_DECLARE_F_ID(0011) = {
    NULL,
    "InErrs",
    "I"
};

J_DECLARE_F_ID(0012) = {
    NULL,
    "OutRsts",
    "I"
};

J_DECLARE_F_ID(0013) = {
    NULL,
    "NumConns",
    "I"
};

SIGHT_CLASS_LDEF(TcpStatistics)
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

    return 0;
}

SIGHT_CLASS_UDEF(TcpStatistics)
{
    sight_unload_class(_E, &_clazzn);
}

static const char *eiftype = "Unsupported NetworkAddressFamily type";

/* Initialize volume enumeration */
SIGHT_EXPORT_DECLARE(void, TcpStatistics, info0)(SIGHT_STDARGS,
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

/* Initialize TCP conn enumeration */
SIGHT_EXPORT_DECLARE(jlong, TcpStatistics, enum0)(SIGHT_STDARGS,
                                                  jint iftype,
                                                  jlong pool)
{
    return 0;
}

/* Get the number of entries */
SIGHT_EXPORT_DECLARE(jint, TcpStatistics, enum1)(SIGHT_STDARGS,
                                                 jlong handle)
{
    return 0;
}

SIGHT_EXPORT_DECLARE(void, TcpStatistics, enum2)(SIGHT_STDARGS,
                                                 jobject conn,
                                                 jint index,
                                                 jlong handle)
{

}

/* Close TCP conn enumeration */
SIGHT_EXPORT_DECLARE(void, TcpStatistics, enum3)(SIGHT_STDARGS,
                                                 jlong handle)
{

}
