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
 * Network adapter implementation
 */

J_DECLARE_CLAZZ = {
    NULL,
    NULL,
    SIGHT_CLASS_PATH "Volume"
};

J_DECLARE_F_ID(0000) = {
    NULL,
    "Name",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0001) = {
    NULL,
    "Description",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0002) = {
    NULL,
    "MountPoint",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0003) = {
    NULL,
    "SectorsPerCluster",
    "I"
};

J_DECLARE_F_ID(0004) = {
    NULL,
    "BytesPerSector",
    "I"
};

J_DECLARE_F_ID(0005) = {
    NULL,
    "FreeBytesAvailable",
    "J"
};

J_DECLARE_F_ID(0006) = {
    NULL,
    "TotalNumberOfBytes",
    "J"
};

J_DECLARE_F_ID(0007) = {
    NULL,
    "TotalNumberOfFreeBytes",
    "J"
};

J_DECLARE_F_ID(0008) = {
    NULL,
    "NumberOfMountPoints",
    "I"
};

J_DECLARE_M_ID(0000) = {
    NULL,
    "setFSType",
    "(I)V"
};

J_DECLARE_M_ID(0001) = {
    NULL,
    "setFlags",
    "(I)V"
};

J_DECLARE_M_ID(0002) = {
    NULL,
    "setDType",
    "(I)V"
};


SIGHT_CLASS_LDEF(Volume)
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
    J_LOAD_METHOD(0000);
    J_LOAD_METHOD(0001);
    J_LOAD_METHOD(0002);

    return 0;
}

SIGHT_CLASS_UDEF(Volume)
{
    sight_unload_class(_E, &_clazzn);
}

/* Initialize volume enumeration */
SIGHT_EXPORT_DECLARE(jlong, Volume, enum0)(SIGHT_STDARGS,
                                           jlong pool)
{
    return 0;
}

SIGHT_EXPORT_DECLARE(jint, Volume, enum1)(SIGHT_STDARGS,
                                          jlong handle)
{
    return 0;
}

SIGHT_EXPORT_DECLARE(void, Volume, enum2)(SIGHT_STDARGS,
                                          jobject thiz,
                                          jlong handle)
{
}

SIGHT_EXPORT_DECLARE(jint, Volume, enum3)(SIGHT_STDARGS,
                                          jobject thiz,
                                          jlong handle)
{
    return 0;
}

/* Close volume enumeration */
SIGHT_EXPORT_DECLARE(void, Volume, enum4)(SIGHT_STDARGS,
                                          jlong handle)
{
}
