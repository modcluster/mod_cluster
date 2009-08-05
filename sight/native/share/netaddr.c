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
#include "sight_private.h"

/*
 * Network address
 */

J_DECLARE_CLAZZ = {
    NULL,
    NULL,
    SIGHT_CLASS_PATH "NetworkAddress"
};

J_DECLARE_M_ID(0000) = {
    NULL,
    "<init>",
    "()V"
};

J_DECLARE_M_ID(0001) = {
    NULL,
    "setFamily",
    "(I)V"
};

J_DECLARE_F_ID(0000) = {
    NULL,
    "HostName",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0001) = {
    NULL,
    "ServiceName",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0002) = {
    NULL,
    "Address",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0003) = {
    NULL,
    "Mask",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0004) = {
    NULL,
    "LeaseLifetime",
    "J"
};

J_DECLARE_F_ID(0005) = {
    NULL,
    "Port",
    "I"
};


SIGHT_CLASS_LDEF(NetworkAddress)
{
    if (sight_load_class(_E, &_clazzn))
        return 1;
    J_LOAD_IFIELD(0000);
    J_LOAD_IFIELD(0001);
    J_LOAD_IFIELD(0002);
    J_LOAD_IFIELD(0003);
    J_LOAD_IFIELD(0004);
    J_LOAD_IFIELD(0005);
    J_LOAD_METHOD(0000);
    J_LOAD_METHOD(0001);

    return 0;
}

SIGHT_CLASS_UDEF(NetworkAddress)
{
    sight_unload_class(_E, &_clazzn);
}

jobject sight_new_netaddr_class(SIGHT_STDARGS)
{
    if (_clazzn.i && _m0000n.i)
        return (*_E)->NewObject(_E, _clazzn.i, _m0000n.i, NULL);
    else
        return NULL;
}

jobjectArray sight_new_netaddr_array(SIGHT_STDARGS, jsize len)
{
    if (_clazzn.a)
        return (*_E)->NewObjectArray(_E, len, _clazzn.a, NULL);
    else
        return NULL;
}

void sight_netaddr_set_host(SIGHT_STDARGS, const char *val)
{
    SET_IFIELD_S(0000, _O, val);
}

void sight_netaddr_set_serv(SIGHT_STDARGS, const char *val)
{
    SET_IFIELD_S(0001, _O, val);
}

void sight_netaddr_set_addr(SIGHT_STDARGS, const char *val)
{
    SET_IFIELD_S(0002, _O, val);
}

void sight_netaddr_set_mask(SIGHT_STDARGS, const char *val)
{
    SET_IFIELD_S(0003, _O, val);
}

void sight_netaddr_set_llt(SIGHT_STDARGS, jlong val)
{
    SET_IFIELD_J(0004, _O, val);
}

void sight_netaddr_set_port(SIGHT_STDARGS, jint val)
{
    SET_IFIELD_I(0005, _O, val);
}

void sight_netaddr_set_family(SIGHT_STDARGS, jint val)
{
    jint family;
    switch (val) {
        case AF_INET:       family = 1; break;
        case AF_INET6:      family = 2; break;
        case AF_LOCAL:      family = 3; break;
        case AF_HARDWARE:   family = 4; break;
        default:            family = 0; break;
    }
    CALL_METHOD1(0001, _O, family);
}

