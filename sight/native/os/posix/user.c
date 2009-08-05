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
 * POSIX user implementation
 *
 */

#include "sight.h"
#include "sight_local.h"
#include "sight_types.h"
#include "sight_private.h"

#if defined(_sun)
#include <utmpx.h>
#define SIGHT_UTMP_FILE _UTMPX_FILE
#define u_time ut_tv.tv_sec
#else
#include <utmp.h>
#ifdef UTMP_FILE
#define SIGHT_UTMP_FILE UTMP_FILE
#else
#define SIGHT_UTMP_FILE _PATH_UTMP
#endif
#endif


static const char etc_usr[] = "/etc/passwd";

/*
 * User
 */

J_DECLARE_CLAZZ = {
    NULL,
    NULL,
    SIGHT_CLASS_PATH "User"
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
    "FullName",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0002) = {
    NULL,
    "Comment",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0003) = {
    NULL,
    "Id",
    "J"
};

J_DECLARE_F_ID(0004) = {
    NULL,
    "Home",
    "Ljava/lang/String;"
};

SIGHT_CLASS_LDEF(User)
{
    if (sight_load_class(_E, &_clazzn))
        return 1;

    J_LOAD_METHOD(0000);
    J_LOAD_IFIELD(0000);
    J_LOAD_IFIELD(0001);
    J_LOAD_IFIELD(0002);
    J_LOAD_IFIELD(0003);
    J_LOAD_IFIELD(0004);

    return 0;
}

SIGHT_CLASS_UDEF(User)
{
    sight_unload_class(_E, &_clazzn);
}

typedef struct posix_user_t {
    int     gid;
    int     uid;
} posix_user_t;

extern apr_pool_t *sight_global_pool;

static jobject new_user_class(SIGHT_STDARGS, jlong instance)
{
    if (_clazzn.i && _m0000n.i)
        return (*_E)->NewObject(_E, _clazzn.i, _m0000n.i, (jint)0, instance);
    else
        return NULL;
}

SIGHT_EXPORT_DECLARE(void, User, free0)(SIGHT_STDARGS, jlong instance)
{
    UNREFERENCED_STDARGS;
    if (instance) {
        posix_user_t *u = J2P(instance, posix_user_t *);

        free(u);
    }
}

SIGHT_EXPORT_DECLARE(jobjectArray, User, users0)(SIGHT_STDARGS)
{

    jobjectArray users = NULL;
    sight_arr_t *tusr  = NULL;
    jsize i;

    UNREFERENCED_O;

    if (!(tusr = sight_arr_cload(etc_usr, "#"))) {
        goto cleanup;
    }
    if (tusr->siz)
        users = (*_E)->NewObjectArray(_E, tusr->siz, _clazzn.a, NULL);
    if (!users)
        goto cleanup;

    for (i = 0; i < tusr->siz; i++) {
        jobject u;
        char *token;
        char *titer;
        posix_user_t *nu = (posix_user_t *)malloc(sizeof(posix_user_t));
        
        if (!(u = new_user_class(_E, _O, P2J(nu)))) {
            free(nu);
            users = NULL;
            goto cleanup;
        }
        /* 1. UserName */
        token = sight_strtok_c(tusr->arr[i], ':', &titer);
        SET_IFIELD_S(0000, u, token);
        /* 2. Password */
        token = sight_strtok_c(NULL, ':', &titer);
        /* 3. UID */
        token = sight_strtok_c(NULL, ':', &titer);
        nu->uid = sight_strtoi32(token);
        SET_IFIELD_J(0003, u, (jlong) nu->uid);
        /* 4. GID */
        token = sight_strtok_c(NULL, ':', &titer);
        nu->gid = sight_strtoi32(token);
        /* 5. FullName */
        token = sight_strtok_c(NULL, ':', &titer);
        SET_IFIELD_N(0001, u, token);
        /* 6. Home */
        token = sight_strtok_c(NULL, ':', &titer);
        SET_IFIELD_N(0004, u, token);
        (*_E)->SetObjectArrayElement(_E, users, i, u);
        (*_E)->DeleteLocalRef(_E, u);
    }
cleanup:
    if (tusr)
        sight_arr_free(tusr);
    return users;
}

SIGHT_EXPORT_DECLARE(jlong, User, getuser0)(SIGHT_STDARGS,
                                            jobject thiz,
                                            jlong sid)
{

    sight_arr_t *tusr = NULL;
    posix_user_t *usr = NULL;
    jsize i;

    UNREFERENCED_O;

    if (!(tusr = sight_arr_cload(etc_usr, "#"))) {
        goto cleanup;
    }

    for (i = 0; i < tusr->siz; i++) {
        int  uid;
        char *uname;
        char *token;
        char *titer;
        
        /* 1. UserName */
        uname = sight_strtok_c(tusr->arr[i], ':', &titer);
        /* 2. Password */
        token = sight_strtok_c(NULL, ':', &titer);
        /* 3. UID */
        token = sight_strtok_c(NULL, ':', &titer);
        uid = sight_strtoi32(token);
        if (uid == sid) {
            usr = (posix_user_t *)malloc(sizeof(posix_user_t));
            usr->uid = uid;
            SET_IFIELD_S(0000, thiz, uname);
            SET_IFIELD_J(0003, thiz, (jlong) usr->uid);
            /* 4. GID */
            token = sight_strtok_c(NULL, ':', &titer);
            usr->gid = sight_strtoi32(token);
            /* 5. FullName */
            token = sight_strtok_c(NULL, ':', &titer);
            SET_IFIELD_N(0001, thiz, token);
            /* 6. Home */
            token = sight_strtok_c(NULL, ':', &titer);
            SET_IFIELD_N(0004, thiz, token);
            break;
        }
    }
cleanup:
    if (tusr)
        sight_arr_free(tusr);
    return P2J(usr);
}

SIGHT_EXPORT_DECLARE(jobjectArray, User, who0)(SIGHT_STDARGS)
{
    FILE *fu;
#if defined(_sun)
    struct futmpx su;
#else
    struct utmp su;
#endif
    jsize i, j, nusers = 0;
    jobjectArray users = NULL;
    cache_table_t *tuc;
    cache_entry_t *e;
    sight_arr_t   *tusr = NULL;


    UNREFERENCED_O;
    if (!(tuc = cache_new(16))) {
        return NULL;
    }
    if (!(tusr = sight_arr_cload(etc_usr, "#"))) {
        goto cleanup;
    }
    if (!(fu = fopen(SIGHT_UTMP_FILE, "r"))) {        
        goto cleanup;
    }    
    /* Read the user sessions */
    while (fread(&su, sizeof(su), 1, fu)) {
        if (!*su.ut_name)
            continue;
#ifdef USER_PROCESS
        if (su.ut_type != USER_PROCESS)
            continue;
#endif
        e = cache_add(tuc, su.ut_name);        
    }
    fclose(fu);
    if ((nusers = tuc->siz) > 0)
        users = (*_E)->NewObjectArray(_E, nusers, _clazzn.a, NULL);
    if (!users) {
        goto cleanup;
    }
    for (j = 0; j < nusers; j++) {
        jobject u;
        posix_user_t *nu;
        if (!(nu = (posix_user_t *)sight_malloc(_E,
                                                sizeof(posix_user_t),
                                                THROW_FMARK))) {
            users = NULL;
            goto cleanup;
        }
        nu->uid = -1;
        nu->gid = -1;
        if (!(u = new_user_class(_E, _O, P2J(nu)))) {
            free(nu);
            users = NULL;
            goto cleanup;
        }

        for (i = 0; i < tusr->siz; i++) {
            int  uid;
            char *uname;
            char *token;
            char *titer;
        
            /* 1. UserName */
            uname = sight_strtok_c(tusr->arr[i], ':', &titer);
            if (strcmp(uname, tuc->list[j]->key)) {
                continue;
            }
            else {
                /* 2. Password */
                token = sight_strtok_c(NULL, ':', &titer);
                /* 3. UID */
                token = sight_strtok_c(NULL, ':', &titer);
                nu->uid = sight_strtoi32(token);
                SET_IFIELD_S(0000, u, uname);
                SET_IFIELD_J(0003, u, (jlong) nu->uid);
                /* 4. GID */
                token = sight_strtok_c(NULL, ':', &titer);
                nu->gid = sight_strtoi32(token);
                /* 5. FullName */
                token = sight_strtok_c(NULL, ':', &titer);
                SET_IFIELD_N(0001, u, token);
                /* 6. Home */
                token = sight_strtok_c(NULL, ':', &titer);
                SET_IFIELD_N(0004, u, token);
                break;
            }
        }
        (*_E)->SetObjectArrayElement(_E, users, j, u);
        (*_E)->DeleteLocalRef(_E, u);
    }

cleanup:
    if (tusr)
        sight_arr_free(tusr);
    cache_free(tuc, NULL);
    return users;
}
