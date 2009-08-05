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
 * POSIX Group implementation
 *
 */

#include "sight.h"
#include "sight_local.h"
#include "sight_types.h"
#include "sight_private.h"

static const char etc_grp[] = "/etc/group";

/*
 * Group
 */


J_DECLARE_CLAZZ = {
    NULL,
    NULL,
    SIGHT_CLASS_PATH "Group"
};

J_DECLARE_M_ID(0000) = {
    NULL,
    "<init>",
    "(IJ)V"
};

J_DECLARE_F_ID(0000) = {
    NULL,
    "Name",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0001) = {
    NULL,
    "Comment",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0002) = {
    NULL,
    "Id",
    "J"
};

J_DECLARE_F_ID(0003) = {
    NULL,
    "IsLocal",
    "Z"
};

SIGHT_CLASS_LDEF(Group)
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

SIGHT_CLASS_UDEF(Group)
{
    sight_unload_class(_E, &_clazzn);
}

typedef struct posix_group_t {
    char    *users;
    int     gid;
} posix_group_t;


extern apr_pool_t *sight_global_pool;

static jobject new_group_class(SIGHT_STDARGS, jlong instance)
{
    if (_clazzn.i && _m0000n.i)
        return (*_E)->NewObject(_E, _clazzn.i, _m0000n.i, (jint)0, instance);
    else
        return NULL;
}

SIGHT_EXPORT_DECLARE(void, Group, free0)(SIGHT_STDARGS, jlong instance)
{
    UNREFERENCED_STDARGS;
    if (instance) {
        posix_group_t *g = J2P(instance, posix_group_t *);
        SIGHT_FREE(g->users);
        free(g);
    }
}

SIGHT_EXPORT_DECLARE(jobjectArray, Group, ggroups0)(SIGHT_STDARGS)
{

    UNREFERENCED_STDARGS;
    return NULL;
}

SIGHT_EXPORT_DECLARE(jobjectArray, Group, lgroups0)(SIGHT_STDARGS)
{
    jobjectArray groups = NULL;
    sight_arr_t *tgrp;
    jsize i;

    UNREFERENCED_O;

    if (!(tgrp = sight_arr_cload(etc_grp, "#"))) {
        goto cleanup;
    }
    if (tgrp->siz)
        groups = (*_E)->NewObjectArray(_E, tgrp->siz, _clazzn.a, NULL);
    if (!groups)
        goto cleanup;

    for (i = 0; i < tgrp->siz; i++) {
        jobject u;
        char *token;
        char *titer;
        posix_group_t *ng = (posix_group_t *)malloc(sizeof(posix_group_t));

        if (!(u = new_group_class(_E, _O, P2J(ng)))) {
            free(ng);
            groups = NULL;
            goto cleanup;
        }
        /* 1. GroupName */
        token = sight_strtok_c(tgrp->arr[i], ':', &titer);
        SET_IFIELD_S(0000, u, token);
        /* 2. Password */
        token = sight_strtok_c(NULL, ':', &titer);
        /* 3. GID */
        token = sight_strtok_c(NULL, ':', &titer);
        ng->gid = sight_strtoi32(token);
        SET_IFIELD_J(0002, u, (jlong) ng->gid);
        SET_IFIELD_Z(0003, u, JNI_TRUE);
        /* 4. Users */
        token = sight_strtok_c(NULL, ':', &titer);
        ng->users = sight_strdup(_E, token, THROW_FMARK);
        (*_E)->SetObjectArrayElement(_E, groups, i, u);
        (*_E)->DeleteLocalRef(_E, u);
    }
cleanup:
    if (tgrp)
        sight_arr_free(tgrp);
    return groups;
}

SIGHT_EXPORT_DECLARE(jlong, Group, getlgrp1)(SIGHT_STDARGS,
                                             jobject thiz,
                                             jlong sid)
{
    UNREFERENCED_STDARGS;
    UNREFERENCED(thiz);
    UNREFERENCED(sid);
    return 0;
}

SIGHT_EXPORT_DECLARE(jlong, Group, getlgrp0)(SIGHT_STDARGS,
                                             jobject thiz,
                                             jlong sid)
{
    sight_arr_t *tgrp;
    jsize i;
    posix_group_t *grp = NULL;

    UNREFERENCED_O;

    if (!(tgrp = sight_arr_cload(etc_grp, "#"))) {
        goto cleanup;
    }
    for (i = 0; i < tgrp->siz; i++) {
        int grpid;
        char *gname;
        char *token;
        char *titer;

        /* 1. GroupName */
        gname = sight_strtok_c(tgrp->arr[i], ':', &titer);
        /* 2. Password */
        token = sight_strtok_c(NULL, ':', &titer);
        /* 3. GID */
        token = sight_strtok_c(NULL, ':', &titer);
        grpid = sight_strtoi32(token);
        if (grpid == sid) {
            grp = (posix_group_t *)malloc(sizeof(posix_group_t));
            if (!grp) {
            
                break;
            }
            grp->gid = grpid;
            SET_IFIELD_S(0000, thiz, gname);
            SET_IFIELD_J(0002, thiz, (jlong) grp->gid);
            SET_IFIELD_Z(0003, thiz, JNI_TRUE);
            /* 4. Users */
            token = sight_strtok_c(NULL, ':', &titer);
            grp->users = sight_strdup(_E, token, THROW_FMARK);
            break;
        }
    }

cleanup:
    if (tgrp)
        sight_arr_free(tgrp);
    return P2J(grp);
}
