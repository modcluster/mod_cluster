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
 * Group implementation
 *
 */

#include "sight.h"
#include "sight_local.h"
#include "sight_types.h"

#define SIGHT_WANT_LATE_DLL
#include "sight_private.h"

/*
 * Group implementation
 */


typedef struct win32_group_t {
    SID_NAME_USE    sidtype;
    PSID            sid;
} win32_group_t;

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
        win32_group_t *u = J2P(instance, win32_group_t *);
        SIGHT_FREE(u->sid);
        free(u);
    }
}

/* This gives strange results (None)
 */
SIGHT_EXPORT_DECLARE(jobjectArray, Group, ggroups0)(SIGHT_STDARGS)
{

    DWORD resumehandle = 0, total;
    PGROUP_INFO_1 pb;
    DWORD  res, dwRec, n, i = 0;
    jsize j = 0, ngroups = 0;
    jobjectArray groups = NULL;

    UNREFERENCED_O;
    do {
        res = NetGroupEnum(NULL, 1, (LPBYTE *)&pb, MAX_PREFERRED_LENGTH,
                           &dwRec, &total, (PDWORD_PTR)&resumehandle );
        if ((res == ERROR_SUCCESS) || (res == ERROR_MORE_DATA)) {
            ngroups += dwRec;
            NetApiBufferFree(pb);
        }
    } while (res == ERROR_MORE_DATA);

    if (ngroups)
        groups = (*_E)->NewObjectArray(_E, ngroups, _clazzn.a, NULL);
    if (!groups)
        return NULL;

    resumehandle = 0;
    do {
        res = NetGroupEnum(NULL, 1, (LPBYTE *)&pb, MAX_PREFERRED_LENGTH,
                           &dwRec, &total, (PDWORD_PTR)&resumehandle );
        if ((res == ERROR_SUCCESS) || (res == ERROR_MORE_DATA)) {
            for (n = 0; n < dwRec; n++) {
                if (j < ngroups) {
                    jobject g;
                    win32_group_t *ng;
                    if (!(ng = (win32_group_t *)sight_malloc(_E,
                                                sizeof(win32_group_t),
                                                THROW_FMARK))) {
                        NetApiBufferFree(pb);
                        return NULL;
                    }
                    if (!(g = new_group_class(_E, _O, P2J(ng)))) {
                        NetApiBufferFree(pb);
                        free(ng);
                        return NULL;
                    }
                    if ((*_E)->ExceptionCheck(_E)) {
                        NetApiBufferFree(pb);
                        free(ng);
                        return NULL;
                    }
                    ng->sid = sight_get_sid(pb[n].grpi1_name, &ng->sidtype);
                    SET_IFIELD_W(0000, g, pb[n].grpi1_name);
                    SET_IFIELD_W(0001, g, pb[n].grpi1_comment);
                    SET_IFIELD_J(0002, g, P2J(ng->sid));
                    SET_IFIELD_Z(0003, g, JNI_TRUE);

                    (*_E)->SetObjectArrayElement(_E, groups, j, g);
                    (*_E)->DeleteLocalRef(_E, g);
                }
                j++;
            }
            NetApiBufferFree(pb);
        }
    } while (res == ERROR_MORE_DATA);

    return groups;
}

SIGHT_EXPORT_DECLARE(jobjectArray, Group, lgroups0)(SIGHT_STDARGS)
{

    DWORD resumehandle = 0, total;
    PLOCALGROUP_INFO_1 pb;
    DWORD  res, dwRec, n, i = 0;
    jsize j = 0, ngroups = 0;
    jobjectArray groups = NULL;

    UNREFERENCED_O;
    do {
        res = NetLocalGroupEnum(NULL, 1, (LPBYTE *)&pb, MAX_PREFERRED_LENGTH,
                                &dwRec, &total, (PDWORD_PTR)&resumehandle );
        if ((res == ERROR_SUCCESS) || (res == ERROR_MORE_DATA)) {
            ngroups += dwRec;
            NetApiBufferFree(pb);
        }
    } while (res == ERROR_MORE_DATA);

    if (ngroups)
        groups = (*_E)->NewObjectArray(_E, ngroups, _clazzn.a, NULL);
    if (!groups)
        return NULL;

    resumehandle = 0;
    do {
        res = NetLocalGroupEnum(NULL, 1, (LPBYTE *)&pb, MAX_PREFERRED_LENGTH,
                                &dwRec, &total, (PDWORD_PTR)&resumehandle );
        if ((res == ERROR_SUCCESS) || (res == ERROR_MORE_DATA)) {
            for (n = 0; n < dwRec; n++) {
                if (j < ngroups) {
                    jobject g;
                    win32_group_t *ng;
                    if (!(ng = (win32_group_t *)sight_malloc(_E,
                                                sizeof(win32_group_t),
                                                THROW_FMARK))) {
                        NetApiBufferFree(pb);
                        return NULL;
                    }
                    if (!(g = new_group_class(_E, _O, P2J(ng)))) {
                        NetApiBufferFree(pb);
                        free(ng);
                        return NULL;
                    }
                    if ((*_E)->ExceptionCheck(_E)) {
                        NetApiBufferFree(pb);
                        free(ng);
                        return NULL;
                    }
                    ng->sid = sight_get_sid(pb[n].lgrpi1_name, &ng->sidtype);
                    SET_IFIELD_W(0000, g, pb[n].lgrpi1_name);
                    SET_IFIELD_W(0001, g, pb[n].lgrpi1_comment);
                    SET_IFIELD_J(0002, g, P2J(ng->sid));
                    SET_IFIELD_Z(0003, g, JNI_TRUE);

                    (*_E)->SetObjectArrayElement(_E, groups, j, g);
                    (*_E)->DeleteLocalRef(_E, g);
                }
                j++;
            }
            NetApiBufferFree(pb);
        }
    } while (res == ERROR_MORE_DATA);

    return groups;
}

SIGHT_EXPORT_DECLARE(jlong, Group, getlgrp1)(SIGHT_STDARGS,
                                             jobject thiz,
                                             jlong sid)
{

    DWORD resumehandle = 0, total;
    PLOCALGROUP_INFO_1 pb;
    DWORD  res, dwRec, n;
    PSID pSid = J2P(sid, PSID);
    win32_group_t *ng = NULL;
    SID_NAME_USE    sidtype;
    WCHAR buf[MAX_PATH+1];

    UNREFERENCED_O;
    if (!pSid || !IsValidSid(pSid))
        return 0;
    sidtype = get_sid_name(buf, MAX_PATH, pSid);
    if (sidtype == -1)
        return 0;
    do {
        res = NetLocalGroupEnum(NULL, 1, (LPBYTE *)&pb, MAX_PREFERRED_LENGTH,
                                &dwRec, &total, (PDWORD_PTR)&resumehandle );
        if ((res == ERROR_SUCCESS) || (res == ERROR_MORE_DATA)) {
            for (n = 0; n < dwRec; n++) {
                PSID gs = sight_get_sid(pb[n].lgrpi1_name, &sidtype);
                if (gs) {
                    if (EqualSid(gs, pSid)) {
                        if (!(ng = (win32_group_t *)sight_malloc(_E,
                                                sizeof(win32_group_t),
                                                THROW_FMARK))) {
                            NetApiBufferFree(pb);
                            free(gs);
                            return 0;
                        }
                        ng->sid = gs;
                        ng->sidtype = sidtype;
                        SET_IFIELD_W(0000, thiz, pb[n].lgrpi1_name);
                        SET_IFIELD_W(0001, thiz, pb[n].lgrpi1_comment);
                        SET_IFIELD_J(0002, thiz, P2J(ng->sid));
                        SET_IFIELD_Z(0003, thiz, JNI_TRUE);
                        break;
                    }
                    else {
                        free(gs);
                    }
                }
            }
            NetApiBufferFree(pb);
        }
        if (ng) {
            return P2J(ng);
        }
    } while (res == ERROR_MORE_DATA);

    if (!(ng = (win32_group_t *)sight_malloc(_E,
                                    sizeof(win32_group_t),
                                    THROW_FMARK))) {
        return 0;
    }

    ng->sid = NULL;
    ng->sidtype = sidtype;
    SET_IFIELD_W(0000, thiz, buf);
    SET_IFIELD_J(0002, thiz, pSid);
    SET_IFIELD_Z(0003, thiz, JNI_TRUE);

    return P2J(ng);
}

SIGHT_EXPORT_DECLARE(jlong, Group, getlgrp0)(SIGHT_STDARGS,
                                             jobject thiz,
                                             jlong sid)
{

    PLOCALGROUP_INFO_1 pb;
    DWORD  res;
    PSID pSid = J2P(sid, PSID);
    win32_group_t *ng = NULL;
    SID_NAME_USE    sidtype;
    WCHAR buf[MAX_PATH+1];

    UNREFERENCED_O;
    if (!pSid || !IsValidSid(pSid))
        return 0;
    sidtype = get_sid_name(buf, MAX_PATH, pSid);
    if (sidtype == -1)
        return 0;
    res = NetGroupGetInfo(NULL, buf, 1, (LPBYTE *)&pb);
    if (res == ERROR_SUCCESS) {
        PSID gs = sight_get_sid(pb->lgrpi1_name, &sidtype);
        if (gs) {
            if (!(ng = (win32_group_t *)sight_malloc(_E,
                                            sizeof(win32_group_t),
                                            THROW_FMARK))) {
                NetApiBufferFree(pb);
                free(gs);
                return 0;
            }
            ng->sid = gs;
            ng->sidtype = sidtype;
            SET_IFIELD_W(0000, thiz, pb->lgrpi1_name);
            SET_IFIELD_W(0001, thiz, pb->lgrpi1_comment);
            SET_IFIELD_J(0002, thiz, P2J(ng->sid));
            SET_IFIELD_Z(0003, thiz, JNI_TRUE);
        }
        NetApiBufferFree(pb);
        if (ng) {
            return P2J(ng);
        }
    }
    if (!(ng = (win32_group_t *)sight_malloc(_E,
                                    sizeof(win32_group_t),
                                    THROW_FMARK))) {
        return 0;
    }
    ng->sid = NULL;
    ng->sidtype = sidtype;
    SET_IFIELD_W(0000, thiz, buf);
    SET_IFIELD_J(0002, thiz, pSid);
    SET_IFIELD_Z(0003, thiz, JNI_TRUE);

    return P2J(ng);
}
