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
 * Windows Registry implementation
 *
 */
#include "sight.h"
#include "sight_local.h"
#include "sight_types.h"

#define SIGHT_WANT_LATE_DLL
#include "sight_private.h"
#include <shlwapi.h>

#define SAFE_CLOSE_KEY(k)                               \
    if ((k) != NULL && (k) != INVALID_HANDLE_VALUE) {   \
        RegCloseKey((k));                               \
        (k) = NULL;                                     \
    } else (k) = NULL

typedef struct {
    HKEY           root;
    HKEY           key;
} sight_nt_registry_t;


#define SIGHT_HKEY_CLASSES_ROOT       1
#define SIGHT_HKEY_CURRENT_CONFIG     2
#define SIGHT_HKEY_CURRENT_USER       3
#define SIGHT_HKEY_LOCAL_MACHINE      4
#define SIGHT_HKEY_USERS              5

static const struct {
    HKEY k;
} SIGHT_KEYS[] = {
    INVALID_HANDLE_VALUE,
    HKEY_CLASSES_ROOT,
    HKEY_CURRENT_CONFIG,
    HKEY_CURRENT_USER,
    HKEY_LOCAL_MACHINE,
    HKEY_USERS,
    INVALID_HANDLE_VALUE
};

#define SIGHT_KEY_ALL_ACCESS          0x0001
#define SIGHT_KEY_CREATE_LINK         0x0002
#define SIGHT_KEY_CREATE_SUB_KEY      0x0004
#define SIGHT_KEY_ENUMERATE_SUB_KEYS  0x0008
#define SIGHT_KEY_EXECUTE             0x0010
#define SIGHT_KEY_NOTIFY              0x0020
#define SIGHT_KEY_QUERY_VALUE         0x0040
#define SIGHT_KEY_READ                0x0080
#define SIGHT_KEY_SET_VALUE           0x0100
#define SIGHT_KEY_WOW64_64KEY         0x0200
#define SIGHT_KEY_WOW64_32KEY         0x0400
#define SIGHT_KEY_WRITE               0x0800

#define SIGHT_REGSAM(s, x)                      \
        s = 0;                                  \
        if (x & SIGHT_KEY_ALL_ACCESS)           \
            s |= KEY_ALL_ACCESS;                \
        if (x & SIGHT_KEY_CREATE_LINK)          \
            s |= KEY_CREATE_LINK;               \
        if (x & SIGHT_KEY_CREATE_SUB_KEY)       \
            s |= KEY_CREATE_SUB_KEY;            \
        if (x & SIGHT_KEY_ENUMERATE_SUB_KEYS)   \
            s |= KEY_ENUMERATE_SUB_KEYS;        \
        if (x & SIGHT_KEY_EXECUTE)              \
            s |= KEY_EXECUTE;                   \
        if (x & SIGHT_KEY_NOTIFY)               \
            s |= KEY_NOTIFY;                    \
        if (x & SIGHT_KEY_READ)                 \
            s |= KEY_READ;                      \
        if (x & SIGHT_KEY_SET_VALUE)            \
            s |= KEY_SET_VALUE;                 \
        if (x & SIGHT_KEY_WOW64_64KEY)          \
            s |= KEY_WOW64_64KEY;               \
        if (x & SIGHT_KEY_WOW64_32KEY)          \
            s |= KEY_WOW64_32KEY;               \
        if (x & SIGHT_KEY_WRITE)                \
            s |= KEY_WRITE

#define SIGHT_REG_BINARY              1
#define SIGHT_REG_DWORD               2
#define SIGHT_REG_EXPAND_SZ           3
#define SIGHT_REG_MULTI_SZ            4
#define SIGHT_REG_QWORD               5
#define SIGHT_REG_SZ                  6

static const struct {
    DWORD t;
} SIGHT_REGTYPES[] = {
    REG_NONE,
    REG_BINARY,
    REG_DWORD,
    REG_EXPAND_SZ,
    REG_MULTI_SZ,
    REG_QWORD,
    REG_SZ,
    REG_NONE
};

/*
 * NativeObject
 */

J_DECLARE_CLAZZ = {
    NULL,
    NULL,
    SIGHT_PLATFORM_CLASS_PATH "Registry"
};

J_DECLARE_F_ID(0000) = {
    NULL,
    "NHKEY",
    "J"
};

SIGHT_CLASS_LDEF(Registry)
{
    if (sight_load_class(_E, &_clazzn))
        return 1;

    J_LOAD_IFIELD(0000);
    return 0;
}

SIGHT_CLASS_UDEF(Registry)
{
    sight_unload_class(_E, &_clazzn);
}


SIGHT_PLATFORM_DECLARE(jlong, Registry, create0)(SIGHT_STDARGS,
                                                 jint root, jstring name,
                                                 jint sam)
{
    sight_nt_registry_t *reg = NULL;
    SIGHT_ALLOC_WSTRING(name);
    HKEY key, kroot;
    LONG rc;
    REGSAM s;

    UNREFERENCED_O;

    if (root < SIGHT_HKEY_CLASSES_ROOT || root > SIGHT_HKEY_USERS) {
        throwException(_E, "Invalid Registry Root Key");
        goto cleanup;
    }
    if (sam < SIGHT_KEY_ALL_ACCESS || root > SIGHT_KEY_WRITE) {
        throwException(_E, "Invalid Registry Key Security");
        goto cleanup;
    }
    kroot = SIGHT_KEYS[root].k;
    SIGHT_INIT_WSTRING(name);
    SIGHT_REGSAM(s, sam);
    rc = RegCreateKeyExW(kroot, J2C(name), 0, NULL, REG_OPTION_NON_VOLATILE,
                         s, NULL, &key, NULL);
    if (rc != ERROR_SUCCESS) {
        throwAprException(_E, APR_FROM_OS_ERROR(rc));
        goto cleanup;
    }
    reg = (sight_nt_registry_t *)malloc(sizeof(sight_nt_registry_t));
    if (!reg) {
        throwAprMemoryException(_E, THROW_FMARK,
                                apr_get_os_error());
        goto cleanup;
    }
    reg->root = kroot;
    reg->key  = key;

cleanup:
    SIGHT_FREE_WSTRING(name);
    return P2J(reg);
}

SIGHT_PLATFORM_DECLARE(jlong, Registry, open0)(SIGHT_STDARGS,
                                               jint root, jstring name,
                                               jint sam)
{
    sight_nt_registry_t *reg = NULL;
    SIGHT_ALLOC_WSTRING(name);
    HKEY key, kroot;
    LONG rc;
    REGSAM s;

    UNREFERENCED_O;

    if (root < SIGHT_HKEY_CLASSES_ROOT || root > SIGHT_HKEY_USERS) {
        throwException(_E, "Invalid Registry Root Key");
        goto cleanup;
    }
    if (sam < SIGHT_KEY_ALL_ACCESS || root > SIGHT_KEY_WRITE) {
        throwException(_E, "Invalid Registry Key Security");
        goto cleanup;
    }
    kroot = SIGHT_KEYS[root].k;
    SIGHT_INIT_WSTRING(name);
    SIGHT_REGSAM(s, sam);
    rc = RegOpenKeyExW(kroot, J2C(name), 0, s, &key);
    if (rc != ERROR_SUCCESS) {
        throwAprException(_E, APR_FROM_OS_ERROR(rc));
        goto cleanup;
    }

    reg = (sight_nt_registry_t *)malloc(sizeof(sight_nt_registry_t));
    if (!reg) {
        throwAprMemoryException(_E, THROW_FMARK,
                                apr_get_os_error());
        goto cleanup;
    }
    reg->root = kroot;
    reg->key  = key;
cleanup:
    SIGHT_FREE_WSTRING(name);
    return P2J(reg);
}

SIGHT_PLATFORM_DECLARE(void, Registry, close0)(SIGHT_STDARGS,
                                               jlong key)
{
    sight_nt_registry_t *r = J2P(key, sight_nt_registry_t *);

    UNREFERENCED_STDARGS;

    if (r) {
        SAFE_CLOSE_KEY(r->key);
        free(r);
    }
}

SIGHT_PLATFORM_DECLARE(jint, Registry, getType)(SIGHT_STDARGS,
                                                jlong key,
                                                jstring name)
{
    sight_nt_registry_t *k = J2P(key, sight_nt_registry_t *);
    SIGHT_ALLOC_WSTRING(name);
    LONG rc;
    DWORD v;

    UNREFERENCED_O;
    if (!key) {
        throwAprException(_E, APR_EINVAL);
        return -1;
    }
    SIGHT_INIT_WSTRING(name);

    rc = RegQueryValueExW(k->key, J2C(name), NULL, &v, NULL, NULL);
    if (rc != ERROR_SUCCESS) {
        throwAprException(_E, APR_FROM_OS_ERROR(rc));
        v = -rc;
        goto cleanup;
    }
    switch (v) {
        case REG_BINARY:
            v = SIGHT_REG_BINARY;
            break;
        case REG_DWORD:
            v = SIGHT_REG_DWORD;
            break;
        case REG_EXPAND_SZ:
            v = SIGHT_REG_EXPAND_SZ;
            break;
        case REG_MULTI_SZ:
            v = SIGHT_REG_MULTI_SZ;
            break;
        case REG_QWORD:
            v = SIGHT_REG_QWORD;
            break;
        case REG_SZ:
            v = SIGHT_REG_SZ;
            break;
        case REG_DWORD_BIG_ENDIAN:
            v = 0;
            break;
    }
cleanup:
    SIGHT_FREE_WSTRING(name);
    return v;
}

SIGHT_PLATFORM_DECLARE(jint, Registry, getSize)(SIGHT_STDARGS, jlong key,
                                                jstring name)
{
    sight_nt_registry_t *k = J2P(key, sight_nt_registry_t *);
    SIGHT_ALLOC_WSTRING(name);
    LONG rc;
    DWORD v;

    UNREFERENCED_O;
    if (!key) {
        throwAprException(_E, APR_EINVAL);
        return 0;
    }
    SIGHT_INIT_WSTRING(name);
    rc = RegQueryValueExW(k->key, J2C(name), NULL, NULL, NULL, &v);
    if (rc != ERROR_SUCCESS)
        throwAprException(_E, APR_FROM_OS_ERROR(rc));
    SIGHT_FREE_WSTRING(name);
    return v;
}

SIGHT_PLATFORM_DECLARE(jint, Registry, getValueI)(SIGHT_STDARGS, jlong key,
                                                  jstring name)
{
    sight_nt_registry_t *k = J2P(key, sight_nt_registry_t *);
    SIGHT_ALLOC_WSTRING(name);
    LONG rc;
    DWORD t, l;
    DWORD v = 0;

    UNREFERENCED_O;
    if (!key) {
        throwAprException(_E, APR_EINVAL);
        return 0;
    }
    SIGHT_INIT_WSTRING(name);
    rc = RegQueryValueExW(k->key, J2C(name), NULL, &t, NULL, &l);
    if (rc != ERROR_SUCCESS) {
        throwAprException(_E, APR_FROM_OS_ERROR(rc));
        goto cleanup;
    }
    if (t == REG_DWORD) {
        l = sizeof(DWORD);
        rc = RegQueryValueExW(k->key, J2C(name), NULL, NULL, (LPBYTE)&v, &l);
        if (rc != ERROR_SUCCESS) {
            throwAprException(_E, APR_FROM_OS_ERROR(rc));
            goto cleanup;
        }
    }
    else if (t == REG_SZ || t == REG_BINARY ||
             t == REG_MULTI_SZ || t == REG_EXPAND_SZ)
        v = l;
    else {
        v = 0;
        throwException(_E, "Unable to convert the value to integer");
    }
cleanup:
    SIGHT_FREE_WSTRING(name);
    return v;
}

SIGHT_PLATFORM_DECLARE(jlong, Registry, getValueJ)(SIGHT_STDARGS, jlong key,
                                                   jstring name)
{
    sight_nt_registry_t *k = J2P(key, sight_nt_registry_t *);
    SIGHT_ALLOC_WSTRING(name);
    LONG rc;
    DWORD t, l;
    UINT64 v = 0;

    UNREFERENCED_O;
    if (!key) {
        throwAprException(_E, APR_EINVAL);
        return 0;
    }
    SIGHT_INIT_WSTRING(name);
    rc = RegQueryValueExW(k->key, J2C(name), NULL, &t, NULL, &l);
    if (rc != ERROR_SUCCESS) {
        throwAprException(_E, APR_FROM_OS_ERROR(rc));
        goto cleanup;
    }
    if (t == REG_DWORD) {
        DWORD tv;
        l = sizeof(DWORD);
        rc = RegQueryValueExW(k->key, J2C(name), NULL, NULL, (LPBYTE)&tv, &l);
        if (rc != ERROR_SUCCESS) {
            throwAprException(_E, APR_FROM_OS_ERROR(rc));
            goto cleanup;
        }
        v = tv;
    }
    else if (t == REG_QWORD) {
        l = sizeof(UINT64);
        rc = RegQueryValueExW(k->key, J2C(name), NULL, NULL, (LPBYTE)&v, &l);
        if (rc != ERROR_SUCCESS) {
            throwAprException(_E, APR_FROM_OS_ERROR(rc));
            goto cleanup;
        }
    }
    else if (t == REG_SZ || t == REG_BINARY ||
             t == REG_MULTI_SZ || t == REG_EXPAND_SZ)
        v = l;
    else {
        v = 0;
        throwException(_E, "Unable to convert the value to long");
    }
cleanup:
    SIGHT_FREE_WSTRING(name);
    return v;
}

SIGHT_PLATFORM_DECLARE(jstring, Registry, getValueS)(SIGHT_STDARGS,
                                                     jlong key,
                                                     jstring name)
{
    sight_nt_registry_t *k = J2P(key, sight_nt_registry_t *);
    SIGHT_ALLOC_WSTRING(name);
    LONG rc;
    DWORD t, l;
    jstring v = NULL;

    UNREFERENCED_O;
    if (!key) {
        throwAprException(_E, APR_EINVAL);
        goto cleanup;
    }
    SIGHT_INIT_WSTRING(name);
    rc = RegQueryValueExW(k->key, J2C(name), NULL, &t, NULL, &l);
    if (rc != ERROR_SUCCESS) {
        throwAprException(_E, APR_FROM_OS_ERROR(rc));
        goto cleanup;
    }
    if (t == REG_SZ || t == REG_EXPAND_SZ) {
        jchar *vw = (jchar *)malloc(l);
        if (!vw) {
            throwAprMemoryException(_E, THROW_FMARK,
                                    apr_get_os_error());
            goto cleanup;
        }

        rc = RegQueryValueExW(k->key, J2C(name), NULL, NULL, (LPBYTE)vw, &l);
        if (rc != ERROR_SUCCESS) {
            throwAprException(_E, APR_FROM_OS_ERROR(rc));
            free(vw);
            goto cleanup;
        }
        v = WSTR_TO_JSTRING(vw);
        free(vw);
    }
cleanup:
    SIGHT_FREE_WSTRING(name);
    return v;
}

SIGHT_PLATFORM_DECLARE(jbyteArray, Registry, getValueB)(SIGHT_STDARGS,
                                                        jlong key,
                                                        jstring name)
{
    sight_nt_registry_t *k = J2P(key, sight_nt_registry_t *);
    SIGHT_ALLOC_WSTRING(name);
    LONG rc;
    DWORD t, l;
    jbyteArray v = NULL;

    UNREFERENCED_O;
    if (!key) {
        throwAprException(_E, APR_EINVAL);
        goto cleanup;
    }
    SIGHT_INIT_WSTRING(name);
    rc = RegQueryValueExW(k->key, J2C(name), NULL, &t, NULL, &l);
    if (rc != ERROR_SUCCESS) {
        throwAprException(_E, APR_FROM_OS_ERROR(rc));
        goto cleanup;
    }
    if (t == REG_BINARY) {
        BYTE *b = (BYTE *)malloc(l);
        if (!b) {
            throwAprMemoryException(_E, THROW_FMARK,
                                    apr_get_os_error());
            goto cleanup;
        }

        rc = RegQueryValueExW(k->key, J2C(name), NULL, NULL, b, &l);
        if (rc != ERROR_SUCCESS) {
            throwAprException(_E, APR_FROM_OS_ERROR(rc));
            free(b);
            goto cleanup;
        }
        v = sight_new_byte_array(_E, b, l);
        free(b);
    }
cleanup:
    SIGHT_FREE_WSTRING(name);
    return v;
}

static jsize get_multi_sz_count(LPCWSTR str)
{
    LPCWSTR p = str;
    jsize   cnt = 0;
    for ( ; p && *p; p++) {
        cnt++;
        while (*p)
            p++;
    }
    return cnt;
}

SIGHT_PLATFORM_DECLARE(jobjectArray, Registry, getValueA)(SIGHT_STDARGS,
                                                          jlong key,
                                                          jstring name)
{
    sight_nt_registry_t *k = J2P(key, sight_nt_registry_t *);
    SIGHT_ALLOC_WSTRING(name);
    LONG rc;
    DWORD t, l;
    jobjectArray v = NULL;

    UNREFERENCED_O;
    if (!key) {
        rc = EINVAL;
        throwAprException(_E, APR_EINVAL);
        goto cleanup;
    }
    SIGHT_INIT_WSTRING(name);
    rc = RegQueryValueExW(k->key, J2C(name), NULL, &t, NULL, &l);
    if (rc != ERROR_SUCCESS) {
        throwAprException(_E, APR_FROM_OS_ERROR(rc));
        goto cleanup;
    }
    if (t == REG_MULTI_SZ) {
        jsize cnt = 0;
        jchar *p;
        jchar *vw = (jchar *)malloc(l);
        if (!vw) {
            throwAprMemoryException(_E, THROW_FMARK,
                                    apr_get_os_error());
            goto cleanup;
        }
        rc = RegQueryValueExW(k->key, J2C(name), NULL, NULL, (LPBYTE)vw, &l);
        if (rc != ERROR_SUCCESS) {
            throwAprException(_E, APR_FROM_OS_ERROR(rc));
            free(vw);
            goto cleanup;
        }
        cnt = get_multi_sz_count(vw);
        if (cnt) {
            jsize idx = 0;
            v = sight_new_cc_array(_E, SIGHT_CC_STRING, cnt);
            if (!v || (*_E)->ExceptionCheck(_E)) {
                free(vw);
                v = NULL;
                goto cleanup;
            }
            for (p = vw ; p && *p; p++) {
                jstring s;
                jchar *b = p;
                while (*p)
                    p++;
                s = ZSTR_TO_JSTRING(b, (p - b));
                if (s)
                    (*_E)->SetObjectArrayElement(_E, v, idx++, s);
                else
                    break;
                if ((*_E)->ExceptionCheck(_E)) {
                    v = NULL;
                    break;
                }
                (*_E)->DeleteLocalRef(_E, s);
            }
        }
        free(vw);
    }
cleanup:
    SIGHT_FREE_WSTRING(name);
    return v;
}

SIGHT_PLATFORM_DECLARE(jint, Registry, setValueI)(SIGHT_STDARGS,
                                                  jlong key,
                                                  jstring name,
                                                  jint val)
{
    sight_nt_registry_t *k = J2P(key, sight_nt_registry_t *);
    SIGHT_ALLOC_WSTRING(name);
    LONG rc = 0;
    DWORD v = (DWORD)val;

    UNREFERENCED_O;
    if (!key) {
        rc = EINVAL;
        throwAprException(_E, APR_EINVAL);
        goto cleanup;
    }
    SIGHT_INIT_WSTRING(name);
    rc = RegSetValueExW(k->key, J2C(name), 0, REG_DWORD, (CONST BYTE *)&v, sizeof(DWORD));
cleanup:
    SIGHT_FREE_WSTRING(name);
    return APR_FROM_OS_ERROR(rc);
}

SIGHT_PLATFORM_DECLARE(jint, Registry, setValueJ)(SIGHT_STDARGS,
                                                  jlong key,
                                                  jstring name,
                                                  jlong val)
{
    sight_nt_registry_t *k = J2P(key, sight_nt_registry_t *);
    SIGHT_ALLOC_WSTRING(name);
    LONG rc = 0;
    UINT64 v = (UINT64)val;

    UNREFERENCED_O;
    if (!key) {
        rc = EINVAL;
        throwAprException(_E, APR_EINVAL);
        goto cleanup;
    }
    SIGHT_INIT_WSTRING(name);
    rc = RegSetValueExW(k->key, J2C(name), 0, REG_QWORD, (CONST BYTE *)&v, sizeof(UINT64));
cleanup:
    SIGHT_FREE_WSTRING(name);
    return APR_FROM_OS_ERROR(rc);
}

SIGHT_PLATFORM_DECLARE(jint, Registry, setValueS)(SIGHT_STDARGS, jlong key,
                                              jstring name, jstring val)
{
    sight_nt_registry_t *k = J2P(key, sight_nt_registry_t *);
    SIGHT_ALLOC_WSTRING(name);
    SIGHT_ALLOC_WSTRING(val);
    LONG rc = 0;
    DWORD len;

    UNREFERENCED_O;
    if (!key) {
        rc = EINVAL;
        throwAprException(_E, APR_EINVAL);
        goto cleanup;
    }
    SIGHT_INIT_WSTRING(name);
    SIGHT_INIT_WSTRING(val);
    len = (DWORD)JWL(val);
    rc = RegSetValueExW(k->key, J2C(name), 0, REG_SZ,
                        (CONST BYTE *)J2C(val), (len + 1) * 2);
cleanup:
    SIGHT_FREE_WSTRING(name);
    SIGHT_FREE_WSTRING(val);
    return APR_FROM_OS_ERROR(rc);
}

SIGHT_PLATFORM_DECLARE(jint, Registry, setValueE)(SIGHT_STDARGS,
                                                  jlong key,
                                                  jstring name,
                                                  jstring val)
{
    sight_nt_registry_t *k = J2P(key, sight_nt_registry_t *);
    SIGHT_ALLOC_WSTRING(name);
    SIGHT_ALLOC_WSTRING(val);
    LONG rc = 0;
    DWORD len;

    UNREFERENCED_O;
    if (!key) {
        rc = EINVAL;
        throwAprException(_E, APR_EINVAL);
        goto cleanup;
    }
    SIGHT_INIT_WSTRING(name);
    SIGHT_INIT_WSTRING(val);
    len = (DWORD)JWL(val);
    rc = RegSetValueExW(k->key, J2C(name), 0, REG_EXPAND_SZ,
                        (CONST BYTE *)J2C(val), (len + 1) * 2);
cleanup:
    SIGHT_FREE_WSTRING(name);
    SIGHT_FREE_WSTRING(val);
    return APR_FROM_OS_ERROR(rc);
}

SIGHT_PLATFORM_DECLARE(jint, Registry, setValueA)(SIGHT_STDARGS,
                                                  jlong key,
                                                  jstring name,
                                                  jobjectArray vals)
{
    sight_nt_registry_t *k = J2P(key, sight_nt_registry_t *);
    SIGHT_ALLOC_WSTRING(name);
    LONG rc = 0;
    jsize i, len;
    jsize sl = 0;
    jchar *msz, *p;

    UNREFERENCED_O;
    if (!key) {
        rc = EINVAL;
        throwAprException(_E, APR_EINVAL);
        goto cleanup;
    }
    SIGHT_INIT_WSTRING(name);
    len = (*_E)->GetArrayLength(_E, vals);
    for (i = 0; i < len; i++) {
        jstring s = (jstring)(*_E)->GetObjectArrayElement(_E, vals, i);
        if (!s) {
            rc = EINVAL;
            throwAprException(_E, APR_EINVAL);
            goto cleanup;
        }
        sl += (*_E)->GetStringLength(_E, s) + 1;
    }
    sl = (sl + 1) * 2;
    p = msz = (jchar *)calloc(1, sl);
    if (!msz) {
        rc = apr_get_os_error();
        throwAprMemoryException(_E, THROW_FMARK, rc);
        goto cleanup;
    }

    for (i = 0; i < len; i++) {
        jsize   l;
        jstring s = (jstring)(*_E)->GetObjectArrayElement(_E, vals, i);
        l = (*_E)->GetStringLength(_E, s);
        wcsncpy(p, (*_E)->GetStringChars(_E, s, 0), l);
        p += l + 1;
    }
    rc = RegSetValueExW(k->key, J2C(name), 0, REG_MULTI_SZ,
                        (CONST BYTE *)msz, sl);
cleanup:
    SIGHT_FREE_WSTRING(name);
    free(msz);
    return APR_FROM_OS_ERROR(rc);
}

SIGHT_PLATFORM_DECLARE(jint, Registry, setValueB)(SIGHT_STDARGS,
                                                  jlong key,
                                                  jstring name,
                                                  jbyteArray val,
                                                  jint off, jint len)
{
    sight_nt_registry_t *k = J2P(key, sight_nt_registry_t *);
    SIGHT_ALLOC_WSTRING(name);
    jbyte *bytes = (*_E)->GetByteArrayElements(_E, val, NULL);
    LONG rc = 0;

    UNREFERENCED_O;
    if (!key || !bytes) {
        rc = EINVAL;
        throwAprException(_E, APR_EINVAL);
        goto cleanup;
    }
    SIGHT_INIT_WSTRING(name);
    rc = RegSetValueExW(k->key, J2C(name), 0, REG_BINARY,
                        bytes + off, (DWORD)len);
cleanup:
    (*_E)->ReleaseByteArrayElements(_E, val, bytes, JNI_ABORT);
    SIGHT_FREE_WSTRING(name);
    return APR_FROM_OS_ERROR(rc);
}

#define MAX_VALUE_NAME 4096

SIGHT_PLATFORM_DECLARE(jobjectArray, Registry, enumKeys)(SIGHT_STDARGS,
                                                         jlong key)
{
    sight_nt_registry_t *k = J2P(key, sight_nt_registry_t *);
    LONG rc;
    jobjectArray v = NULL;
    jsize cnt = 0;

    WCHAR    achKey[MAX_PATH];
    WCHAR    achClass[MAX_PATH] = L"";
    DWORD    cchClassName = MAX_PATH;
    DWORD    cSubKeys;
    DWORD    cbMaxSubKey;
    DWORD    cchMaxClass;
    DWORD    cValues;
    DWORD    cchMaxValue;
    DWORD    cbMaxValueData;
    DWORD    cbSecurityDescriptor;
    FILETIME ftLastWriteTime;

    DWORD cchValue = MAX_VALUE_NAME;

    UNREFERENCED_O;
    if (!key) {
        throwAprException(_E, APR_EINVAL);
        return 0;
    }
    rc = RegQueryInfoKeyW(k->key,
                          achClass,
                          &cchClassName,
                          NULL,
                          &cSubKeys,
                          &cbMaxSubKey,
                          &cchMaxClass,
                          &cValues,
                          &cchMaxValue,
                          &cbMaxValueData,
                          &cbSecurityDescriptor,
                          &ftLastWriteTime);
    if (rc != ERROR_SUCCESS) {
        throwAprException(_E, APR_FROM_OS_ERROR(rc));
        goto cleanup;
    }
    cnt = cSubKeys;
    if (cnt) {
        jsize idx = 0;
        v = sight_new_cc_array(_E, SIGHT_CC_STRING, cnt);
        if (!v || (*_E)->ExceptionCheck(_E))
            return NULL;
        for (idx = 0; idx < cnt; idx++) {
            jstring s;
            DWORD achKeyLen = MAX_PATH;
            rc = RegEnumKeyExW(k->key,
                               idx,
                               achKey,
                               &achKeyLen,
                               NULL,
                               NULL,
                               NULL,
                               &ftLastWriteTime);
            if (rc == (DWORD)ERROR_SUCCESS) {
                s = WSTR_TO_JSTRING(achKey);
                if (s) {
                    (*_E)->SetObjectArrayElement(_E, v, idx, s);
                    if ((*_E)->ExceptionCheck(_E))
                        return NULL;
                    (*_E)->DeleteLocalRef(_E, s);
                }
                else
                    return NULL;
            }
        }
    }
cleanup:
    return v;
}

SIGHT_PLATFORM_DECLARE(jobjectArray, Registry, enumValues)(SIGHT_STDARGS,
                                                           jlong key)
{
    sight_nt_registry_t *k = J2P(key, sight_nt_registry_t *);
    LONG rc;
    jobjectArray v = NULL;
    jsize cnt = 0;

    WCHAR    achClass[MAX_PATH] = L"";
    DWORD    cchClassName = MAX_PATH;
    DWORD    cSubKeys;
    DWORD    cbMaxSubKey;
    DWORD    cchMaxClass;
    DWORD    cValues;
    DWORD    cchMaxValue;
    DWORD    cbMaxValueData;
    DWORD    cbSecurityDescriptor;
    FILETIME ftLastWriteTime;

    WCHAR  achValue[MAX_VALUE_NAME];
    DWORD  cchValue = MAX_VALUE_NAME;

    UNREFERENCED_O;
    if (!key) {
        throwAprException(_E, APR_EINVAL);
        return NULL;
    }
    /* Get the class name and the value count. */
    rc = RegQueryInfoKeyW(k->key,
                          achClass,
                          &cchClassName,
                          NULL,
                          &cSubKeys,
                          &cbMaxSubKey,
                          &cchMaxClass,
                          &cValues,
                          &cchMaxValue,
                          &cbMaxValueData,
                          &cbSecurityDescriptor,
                          &ftLastWriteTime);
    if (rc != ERROR_SUCCESS) {
        throwAprException(_E, APR_FROM_OS_ERROR(rc));
        goto cleanup;
    }
    cnt = cValues;
    if (cnt) {
        jsize idx = 0;
        v = sight_new_cc_array(_E, SIGHT_CC_STRING, cnt);
        if (!v || (*_E)->ExceptionCheck(_E))
            return NULL;
        for (idx = 0; idx < cnt; idx++) {
            jstring s;
            cchValue = MAX_VALUE_NAME;
            achValue[0] = L'\0';
            rc = RegEnumValueW(k->key, idx,
                               achValue,
                               &cchValue,
                               NULL,
                               NULL,    // &dwType,
                               NULL,    // &bData,
                               NULL);   // &bcData
            if (rc == (DWORD)ERROR_SUCCESS) {
                s = WSTR_TO_JSTRING(achValue);
                if (s) {
                    (*_E)->SetObjectArrayElement(_E, v, idx, s);
                    if ((*_E)->ExceptionCheck(_E))
                        return NULL;
                    (*_E)->DeleteLocalRef(_E, s);
                }
                else
                    return NULL;
            }
        }
    }
cleanup:
    return v;
}

SIGHT_PLATFORM_DECLARE(jint, Registry, deleteKey0)(SIGHT_STDARGS,
                                                   jint root,
                                                   jstring name,
                                                   jboolean only_if_empty)
{
    DWORD rv;
    SIGHT_ALLOC_WSTRING(name);

    UNREFERENCED_O;
    if (root < SIGHT_HKEY_CLASSES_ROOT || root > SIGHT_HKEY_USERS) {
        rv = EBADF;
        goto cleanup;
    }
    SIGHT_INIT_WSTRING(name);
    if (only_if_empty)
        rv = SHDeleteEmptyKeyW(SIGHT_KEYS[root].k, J2C(name));
    else
        rv = SHDeleteKeyW(SIGHT_KEYS[root].k, J2C(name));
cleanup:
    SIGHT_FREE_WSTRING(name);
    return APR_FROM_OS_ERROR(rv);
}

SIGHT_PLATFORM_DECLARE(jint, Registry, deleteValue)(SIGHT_STDARGS,
                                                    jlong key,
                                                    jstring name)
{
    LONG rv = 0;
    SIGHT_ALLOC_WSTRING(name);
    sight_nt_registry_t *k = J2P(key, sight_nt_registry_t *);

    UNREFERENCED_O;
    if (!key) {
        rv = EINVAL;
        throwAprException(_E, APR_EINVAL);
        goto cleanup;
    }
    SIGHT_INIT_WSTRING(name);
    rv = RegDeleteValueW(k->key, J2C(name));
cleanup:
    SIGHT_FREE_WSTRING(name);
    return APR_FROM_OS_ERROR(rv);
}
