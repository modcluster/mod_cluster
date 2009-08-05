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
 * TCP Connection
 */

J_DECLARE_CLAZZ = {
    NULL,
    NULL,
    SIGHT_CLASS_PATH "TcpConnection"
};

J_DECLARE_M_ID(0000) = {
    NULL,
    "<init>",
    "()V"
};

J_DECLARE_M_ID(0001) = {
    NULL,
    "setState",
    "(I)V"
};

J_DECLARE_F_ID(0000) = {
    NULL,
    "LocalAddr",
    "L" SIGHT_CLASS_PATH "NetworkAddress;"
};

J_DECLARE_F_ID(0001) = {
    NULL,
    "RemoteAddr",
    "L" SIGHT_CLASS_PATH "NetworkAddress;"
};

J_DECLARE_F_ID(0002) = {
    NULL,
    "Pid",
    "I"
};

J_DECLARE_F_ID(0003) = {
    NULL,
    "CreateTimestamp",
    "J"
};

J_DECLARE_F_ID(0004) = {
    NULL,
    "Timeout",
    "I"
};

SIGHT_CLASS_LDEF(TcpConnection)
{
    if (sight_load_class(_E, &_clazzn))
        return 1;
    J_LOAD_IFIELD(0000);
    J_LOAD_IFIELD(0001);
    J_LOAD_IFIELD(0002);
    J_LOAD_IFIELD(0003);
    J_LOAD_IFIELD(0004);
    J_LOAD_METHOD(0000);
    J_LOAD_METHOD(0001);

    return 0;
}

SIGHT_CLASS_UDEF(TcpConnection)
{
    sight_unload_class(_E, &_clazzn);
}

jobject sight_new_tcpconn_class(SIGHT_STDARGS)
{
    if (_clazzn.i && _m0000n.i)
        return (*_E)->NewObject(_E, _clazzn.i, _m0000n.i, NULL);
    else
        return NULL;
}

jobjectArray sight_new_tcpconn_array(SIGHT_STDARGS, jsize len)
{
    if (_clazzn.a)
        return (*_E)->NewObjectArray(_E, len, _clazzn.a, NULL);
    else
        return NULL;
}

void sight_tcpconn_set_local(SIGHT_STDARGS, jobject val)
{
    SET_IFIELD_O(0000, _O, val);
}

void sight_tcpconn_set_remote(SIGHT_STDARGS, jobject val)
{
    SET_IFIELD_O(0001, _O, val);
}

void sight_tcpconn_set_pid(SIGHT_STDARGS, jint val)
{
    SET_IFIELD_I(0002, _O, val);
}

void sight_tcpconn_set_cts(SIGHT_STDARGS, jlong val)
{
    SET_IFIELD_J(0003, _O, val);
}

void sight_tcpconn_set_tmo(SIGHT_STDARGS, jint val)
{
    SET_IFIELD_I(0004, _O, val);
}

void sight_tcpconn_set_state(SIGHT_STDARGS, jint val)
{
    CALL_METHOD1(0001, _O, val);
}

