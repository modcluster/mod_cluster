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
 * Process windows implementation
 *
 */

#include "sight.h"
#include "sight_local.h"
#include "sight_types.h"

#define SIGHT_WANT_LATE_DLL
#include "sight_private.h"

typedef struct win32_user_t {
    SID_NAME_USE    sidtype;
    PSID            sid;
} win32_user_t;

/*
 * User implementation
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

static jobject new_user_class(SIGHT_STDARGS, jlong instance)
{
    if (_clazzn.i && _m0000n.i)
        return (*_E)->NewObject(_E, _clazzn.i, _m0000n.i, (jint)0, instance);
    else
        return NULL;
}

static void get_home_path(LPWSTR buf, DWORD blen, PSID sid)
{
    LPWSTR ssid = NULL;
    WCHAR regk[SIGHT_MBUFFER_SIZ];
    HKEY  key;
    DWORD rv, type;
    DWORD keylen;

    buf[0] = L'\0';

    if (!ConvertSidToStringSidW(sid, &ssid)) {
        return;
    }

    lstrcpyW(regk, L"SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\ProfileList\\");
    lstrcatW(regk, ssid);
    LocalFree(ssid);

    if (RegOpenKeyExW(HKEY_LOCAL_MACHINE, regk, 0,
                     KEY_QUERY_VALUE, &key) == ERROR_SUCCESS) {
        keylen = sizeof(regk);
        rv = RegQueryValueExW(key, L"ProfileImagePath", NULL, &type,
                                   (void *)regk, &keylen);
        RegCloseKey(key);
        if (rv != ERROR_SUCCESS)
            return;
        if (type == REG_SZ)
            lstrcpynW(buf, regk, blen);
        else if (type == REG_EXPAND_SZ)
            ExpandEnvironmentStringsW(regk, buf, blen);
        buf[blen] = L'\0';
    }
}

SIGHT_EXPORT_DECLARE(void, User, free0)(SIGHT_STDARGS, jlong instance)
{
    UNREFERENCED_STDARGS;
    if (instance) {
        win32_user_t *u = J2P(instance, win32_user_t *);
        SIGHT_FREE(u->sid);
        free(u);
    }
}

SIGHT_EXPORT_DECLARE(jobjectArray, User, users0)(SIGHT_STDARGS)
{

    DWORD resumehandle = 0, total;
    PUSER_INFO_2 pb;
    DWORD  res, dwRec, n, i = 0;
    jsize j = 0, nusers = 0;
    jobjectArray users = NULL;

    UNREFERENCED_O;
    do {
        res = NetUserEnum(NULL, 2, 0, (LPBYTE *)&pb, MAX_PREFERRED_LENGTH,
                                &dwRec, &total, (LPDWORD)&resumehandle );
        if ((res == ERROR_SUCCESS) || (res == ERROR_MORE_DATA)) {
            nusers += dwRec;
            NetApiBufferFree(pb);
        }
    } while (res == ERROR_MORE_DATA);

    if (nusers)
        users = (*_E)->NewObjectArray(_E, nusers, _clazzn.a, NULL);
    if (!users)
        return NULL;
    resumehandle = 0;
    do {
        res = NetUserEnum(NULL, 2, 0, (LPBYTE *)&pb, MAX_PREFERRED_LENGTH,
                                &dwRec, &total, (LPDWORD)&resumehandle );
        if ((res == ERROR_SUCCESS) || (res == ERROR_MORE_DATA)) {
            for (n = 0; n < dwRec; n++) {
                if (j < nusers) {
                    jobject u;
                    win32_user_t *nu;
                    if (!(nu = (win32_user_t *)sight_malloc(_E,
                                               sizeof(win32_user_t),
                                               THROW_FMARK))) {
                        NetApiBufferFree(pb);
                        return NULL;
                    }
                    if (!(u = new_user_class(_E, _O, P2J(nu)))) {
                        NetApiBufferFree(pb);
                        free(nu);
                        return NULL;
                    }
                    if ((*_E)->ExceptionCheck(_E)) {
                        NetApiBufferFree(pb);
                        free(nu);
                        return NULL;
                    }
                    nu->sid = sight_get_sid(pb[n].usri2_name, &nu->sidtype);
                    SET_IFIELD_W(0000, u, pb[n].usri2_name);
                    SET_IFIELD_W(0001, u, pb[n].usri2_full_name);
                    SET_IFIELD_W(0002, u, pb[n].usri2_comment);
                    SET_IFIELD_J(0003, u, P2J(nu->sid));
                    SET_IFIELD_W(0004, u, pb[n].usri2_home_dir);

                    (*_E)->SetObjectArrayElement(_E, users, j, u);
                    (*_E)->DeleteLocalRef(_E, u);
                }
                j++;
            }
            NetApiBufferFree(pb);
        }
    } while (res == ERROR_MORE_DATA);

    return users;

}

SIGHT_EXPORT_DECLARE(jlong, User, getuser0)(SIGHT_STDARGS,
                                            jobject thiz,
                                            jlong sid)
{

    PUSER_INFO_2 pb;
    DWORD  res;
    PSID pSid = J2P(sid, PSID);
    win32_user_t *nu = NULL;
    SID_NAME_USE  sidtype;
    WCHAR buf[MAX_PATH+1];
    WCHAR home[MAX_PATH+1];

    UNREFERENCED_O;
    if (!pSid || !IsValidSid(pSid))
        return 0;
    sidtype = get_sid_name(buf, MAX_PATH, pSid);
    if (sidtype == -1)
        return 0;

    res = NetUserGetInfo(NULL, buf, 2, (LPBYTE *)&pb);
    if (res == ERROR_SUCCESS) {
        PSID us = sight_get_sid(pb->usri2_name, &sidtype);
        if (us) {
            if (!(nu = (win32_user_t *)sight_malloc(_E,
                                       sizeof(win32_user_t),
                                       THROW_FMARK))) {
                NetApiBufferFree(pb);
                free(us);
                return 0;
            }
            nu->sid = us;
            nu->sidtype = sidtype;
            SET_IFIELD_W(0000, thiz, pb->usri2_name);
            SET_IFIELD_W(0001, thiz, pb->usri2_full_name);
            SET_IFIELD_W(0002, thiz, pb->usri2_comment);
            SET_IFIELD_J(0003, thiz, P2J(nu->sid));
            SET_IFIELD_W(0004, thiz, pb->usri2_home_dir);
        }
        NetApiBufferFree(pb);
        if (nu) {
            return P2J(nu);
        }
    }

    if (!(nu = (win32_user_t *)sight_malloc(_E,
                               sizeof(win32_user_t),
                               THROW_FMARK))) {
        return 0;
    }

    nu->sid = NULL;
    nu->sidtype = sidtype;
    SET_IFIELD_W(0000, thiz, buf);
    SET_IFIELD_W(0001, thiz, buf);
    SET_IFIELD_J(0003, thiz, P2J(pSid));
    get_home_path(&home[0], MAX_PATH, pSid);
    if (home[0]) {
        SET_IFIELD_W(0004, thiz, home);
    }

    return P2J(nu);
}

SIGHT_EXPORT_DECLARE(jobjectArray, User, who0)(SIGHT_STDARGS)
{

    DWORD resumehandle = 0, total;
    PWKSTA_USER_INFO_0 pb;
    DWORD  res, dwRec, n, i = 0;
    jsize j = 0, nusers = 0;
    jobjectArray users = NULL;

    UNREFERENCED_O;
    do {
        res = NetWkstaUserEnum(NULL, 0, (LPBYTE *)&pb, MAX_PREFERRED_LENGTH,
                                &dwRec, &total, (LPDWORD)&resumehandle );
        if ((res == ERROR_SUCCESS) || (res == ERROR_MORE_DATA)) {
            nusers += dwRec;
            NetApiBufferFree(pb);
        }
    } while (res == ERROR_MORE_DATA);

    if (nusers)
        users = (*_E)->NewObjectArray(_E, nusers, _clazzn.a, NULL);
    if (!users)
        return NULL;
    resumehandle = 0;
    do {
        res = NetWkstaUserEnum(NULL, 0, (LPBYTE *)&pb, MAX_PREFERRED_LENGTH,
                               &dwRec, &total, (LPDWORD)&resumehandle );
        if ((res == ERROR_SUCCESS) || (res == ERROR_MORE_DATA)) {
            for (n = 0; n < dwRec; n++) {
                if (j < nusers) {
                    jobject u;
                    PUSER_INFO_2 pui;
                    win32_user_t *nu;
                    if (!(nu = (win32_user_t *)sight_malloc(_E,
                                               sizeof(win32_user_t),
                                               THROW_FMARK))) {
                        NetApiBufferFree(pb);
                        return NULL;
                    }
                    if (!(u = new_user_class(_E, _O, P2J(nu)))) {
                        NetApiBufferFree(pb);
                        free(nu);
                        return NULL;
                    }
                    if ((*_E)->ExceptionCheck(_E)) {
                        NetApiBufferFree(pb);
                        free(nu);
                        return NULL;
                    }
                    nu->sid = sight_get_sid(pb[n].wkui0_username, &nu->sidtype);
                    SET_IFIELD_W(0000, u, pb[n].wkui0_username);
                    SET_IFIELD_J(0003, u, P2J(nu->sid));
                    res = NetUserGetInfo(NULL, pb[n].wkui0_username, 2, (LPBYTE *)&pui);
                    if (res == ERROR_SUCCESS) {
                        SET_IFIELD_W(0001, u, pui->usri2_full_name);
                        SET_IFIELD_W(0002, u, pui->usri2_comment);
                        SET_IFIELD_W(0004, u, pui->usri2_home_dir);
                        NetApiBufferFree(pui);
                    }
                    if (nu->sid) {
                        /* XXX: Do not set users without SID */
                        (*_E)->SetObjectArrayElement(_E, users, j++, u);
                    }
                    (*_E)->DeleteLocalRef(_E, u);
                }
            }
            NetApiBufferFree(pb);
        }
    } while (res == ERROR_MORE_DATA);

    return users;

}

