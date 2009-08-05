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

extern apr_pool_t *sight_temp_pool;

/*
 * FileInfo
 */

J_DECLARE_CLAZZ = {
    NULL,
    NULL,
    SIGHT_CLASS_PATH "FileInfo"
};

J_DECLARE_F_ID(0000) = {
    NULL,
    "Protection",
    "I"
};

J_DECLARE_F_ID(0001) = {
    NULL,
    "UserId",
    "J"
};

J_DECLARE_F_ID(0002) = {
    NULL,
    "GroupId",
    "J"
};

J_DECLARE_F_ID(0003) = {
    NULL,
    "NumLinks",
    "I"
};

J_DECLARE_F_ID(0004) = {
    NULL,
    "Size",
    "J"
};

J_DECLARE_F_ID(0005) = {
    NULL,
    "StorageSize",
    "J"
};

J_DECLARE_F_ID(0006) = {
    NULL,
    "LastAccessTime",
    "J"
};

J_DECLARE_F_ID(0007) = {
    NULL,
    "ModifiedTime",
    "J"
};

J_DECLARE_F_ID(0008) = {
    NULL,
    "CreatedTime",
    "J"
};

J_DECLARE_F_ID(0009) = {
    NULL,
    "Name",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0010) = {
    NULL,
    "BaseName",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0011) = {
    NULL,
    "InodeId",
    "J"
};

J_DECLARE_F_ID(0012) = {
    NULL,
    "DeviceId",
    "J"
};

J_DECLARE_M_ID(0000) = {
    NULL,
    "<init>",
    "()V"
};

J_DECLARE_M_ID(0001) = {
    NULL,
    "setType",
    "(I)V"
};
J_DECLARE_M_ID(0002) = {
    NULL,
    "setValid",
    "(I)V"
};

SIGHT_CLASS_LDEF(FileInfo)
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
    J_LOAD_METHOD(0000);
    J_LOAD_METHOD(0001);
    J_LOAD_METHOD(0002);

    return 0;
}

SIGHT_CLASS_UDEF(FileInfo)
{
    sight_unload_class(_E, &_clazzn);
}

jobject sight_new_finfo_class(SIGHT_STDARGS)
{
    if (_clazzn.i && _m0000n.i)
        return (*_E)->NewObject(_E, _clazzn.i, _m0000n.i, NULL);
    else
        return NULL;
}

void sight_finfo_fill(SIGHT_STDARGS, apr_finfo_t *finfo)
{
    if (!finfo)
        return;
    SET_IFIELD_I(0000, _O, finfo->protection);
    SET_IFIELD_J(0001, _O, P2J(finfo->user));
    SET_IFIELD_J(0002, _O, P2J(finfo->group));
    SET_IFIELD_I(0003, _O, finfo->nlink);
    SET_IFIELD_J(0004, _O, finfo->size);
    SET_IFIELD_J(0005, _O, finfo->csize);

    SET_IFIELD_J(0006, _O, apr_time_as_msec(finfo->atime));
    SET_IFIELD_J(0007, _O, apr_time_as_msec(finfo->mtime));
    SET_IFIELD_J(0008, _O, apr_time_as_msec(finfo->ctime));

    SET_IFIELD_S(0009, _O, finfo->fname);
    SET_IFIELD_S(0010, _O, finfo->name);
    SET_IFIELD_J(0011, _O, (jlong)finfo->inode);
    SET_IFIELD_J(0012, _O, (jlong)finfo->device);
    CALL_METHOD1(0001, _O, finfo->filetype);
    CALL_METHOD1(0002, _O, finfo->valid);
}

SIGHT_EXPORT_DECLARE(jint, FileInfo, stat0)(SIGHT_STDARGS,
                                            jobject thiz,
                                            jstring name,
                                            jint wanted)
{
    apr_pool_t *p = NULL;
    apr_status_t rv = APR_ENOPOOL;
    apr_finfo_t info;
    SIGHT_ALLOC_CSTRING(name);

    SIGHT_GLOBAL_TRY {
        if ((rv = apr_pool_create(&p, sight_temp_pool)) != APR_SUCCESS) {
            throwAprMemoryException(_E, THROW_FMARK, rv);
        }
        else {
            memset(&info, 0, sizeof(apr_finfo_t));
            rv = apr_stat(&info, J2S(name), wanted, p);
            if (rv == APR_SUCCESS || rv == APR_INCOMPLETE) {
                sight_finfo_fill(_E, thiz, &info);
            }
            else {
                throwAprException(_E, rv);
            }
            apr_pool_destroy(p);
        }
    } SIGHT_GLOBAL_END();

    SIGHT_FREE_CSTRING(name);
    return rv;
}
