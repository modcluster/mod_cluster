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
 * Cpu windows implementation
 *
 */

#include "sight.h"
#include "sight_local.h"
#include "sight_types.h"

#define SIGHT_WANT_LATE_DLL
#include "sight_private.h"

J_DECLARE_CLAZZ = {
    NULL,
    NULL,
    SIGHT_CLASS_PATH "Cpu"
};

J_DECLARE_F_ID(0000) = {
    NULL,
    "IsBigEndian",
    "Z"
};

J_DECLARE_F_ID(0001) = {
    NULL,
    "NumberOfProcessors",
    "I"
};

J_DECLARE_F_ID(0002) = {
    NULL,
    "Family",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0003) = {
    NULL,
    "Model",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0004) = {
    NULL,
    "Stepping",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0005) = {
    NULL,
    "Vendor",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0006) = {
    NULL,
    "Name",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0007) = {
    NULL,
    "MHz",
    "D"
};

J_DECLARE_F_ID(0008) = {
    NULL,
    "Bogomips",
    "D"
};

SIGHT_CLASS_LDEF(Cpu)
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

    return 0;
}

SIGHT_CLASS_UDEF(Cpu)
{
    sight_unload_class(_E, &_clazzn);
}

SIGHT_EXPORT_DECLARE(jint, Cpu, alloc0)(SIGHT_STDARGS,
                                        jobject thiz,
                                        jlong instance)
{
    HKEY key;
    sight_object_t *no = J2P(instance, sight_object_t *);
    char *p;
    LONG rc;
    REGSAM s = KEY_READ;
    apr_int32_t mhz;
    char buff[SIGHT_STYPE_SIZ];

    if (!no || !no->pool)
        return APR_EINVAL;
    rc = RegOpenKeyExA(HKEY_LOCAL_MACHINE,
                       SIGHT_REGS_CPU0, 0, KEY_READ, &key);
    if (rc != ERROR_SUCCESS)
        return APR_FROM_OS_ERROR(rc);

    SET_IFIELD_Z(0000, thiz, sight_byteorder());
    SET_IFIELD_I(0001, thiz, sight_osinf->dwNumberOfProcessors);
    itoa(sight_osinf->wProcessorLevel, buff, 10);
    SET_IFIELD_S(0002, thiz, buff);
    itoa(sight_osinf->wProcessorRevision >> 8, buff, 10);
    SET_IFIELD_S(0003, thiz, buff);
    itoa(sight_osinf->wProcessorRevision & 0x00FF, buff, 10);
    SET_IFIELD_S(0004, thiz, buff);
    SET_IFIELD_S(0005, thiz, sight_registry_get_lpstr(key,
                                            "VendorIdentifier",
                                            buff, SIGHT_STYPE_LEN));
    p = sight_registry_get_lpstr(key, "ProcessorNameString",
                                 buff, SIGHT_STYPE_LEN);
    SET_IFIELD_S(0006, thiz, sight_trim(p));
    sight_registry_get_int32(key, "~MHz", &mhz);
    SET_IFIELD_D(0007, thiz, mhz);

    RegCloseKey(key);
    return APR_SUCCESS;
}
