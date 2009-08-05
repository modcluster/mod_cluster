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

LPWSTR sight_trimw(LPWSTR s)
{
    size_t i;

    /* check for empty strings */
    if (!s || !*s)
        return NULL;
    i = lstrlenW(s) - 1;
    while (i >= 0 && (iswspace(s[i]) || (s[i] == L'\r') || (s[i] == L'\n')))
        s[i--] = L'\0';
    while (*s && iswspace(*s))
        s++;

    return s;
}

void sight_trimwc(LPWSTR d, LPCWSTR s)
{
    LPWSTR p = sight_trimw((LPWSTR)s);
    if (p && d)
        lstrcpyW(d, p);
}

int sight_iswlast(LPCWSTR s, int chr)
{
    size_t i;
    /* check for empty strings */
    if (!(i = lstrlenW(s)))
        return 0;
    if (s[i-1] == chr)
        return -1;
    else
        return 0;
}


char *sight_registry_get_lpstr(HKEY hkey, const char *tag,
                               char *b, apr_size_t len)
{
    DWORD type = 0;
    LONG lrc;
    DWORD sz = (DWORD)len;

    b[0] = '\0';
    if ((lrc = RegQueryValueExA(hkey, tag, NULL, &type,
                                (LPBYTE)b, &sz)) != ERROR_SUCCESS)
        return NULL;
    if (type != REG_SZ)
        return NULL;
    b[sz] = '\0';
    return b;
}

#if 0
char *sight_registry_pstrdup(HKEY hkey, const char *tag,
                             apr_pool_t *ctx)
{
    DWORD type = 0;
    LONG lrc;
    DWORD sz = 0;
    char *rv;

    if ((lrc = RegQueryValueExA(hkey, tag, NULL, &type,
                                NULL, &sz)) != ERROR_SUCCESS)
        return NULL;
    if (type != REG_SZ)
        return NULL;
    if (!(rv = (char *)apr_palloc(ctx, sz)))
        return NULL;
    if ((lrc = RegQueryValueExA(hkey, tag, NULL, &type,
                                (LPBYTE)rv, &sz)) != ERROR_SUCCESS)
        return NULL;
    return (char *)rv;
}

sight_str_t sight_registry_pstrdupw(HKEY hkey, LPCWSTR tag,
                                    apr_pool_t *ctx)
{
    DWORD type = 0;
    LONG lrc;
    DWORD sz = 0;
    WCHAR *rv;
    sight_str_t str;

    str.len = 0;
    str.w   = NULL;
    if ((lrc = RegQueryValueExW(hkey, tag, NULL, &type,
                                NULL, &sz)) != ERROR_SUCCESS)
        return str;
    if (type == REG_SZ) {
        if (!(rv = (WCHAR *)apr_palloc(ctx, sz)))
            return str;
    }
    else
        return str;
    if ((lrc = RegQueryValueExW(hkey, tag, NULL, &type,
                                (LPBYTE)rv, &sz)) != ERROR_SUCCESS)
        return str;
    str.len = lstrlenW(rv);
    str.w   = rv;
    return str;
}

sight_arr_t sight_registry_get_arrw(HKEY hkey, LPCWSTR tag,
                                    apr_pool_t *ctx)
{
    DWORD type = 0;
    LONG lrc;
    DWORD sz = 0;
    WCHAR *rv, *p;
    sight_arr_t arr;
    jsize n = 0;

    arr.len = 0;
    arr.wa  = NULL;
    if ((lrc = RegQueryValueExW(hkey, tag, NULL, &type,
                                NULL, &sz)) != ERROR_SUCCESS)
        return arr;
    if (type == REG_MULTI_SZ) {
        if (!(rv = (WCHAR *)apr_palloc(ctx, sz)))
            return arr;
    }
    else
        return arr;
    if ((lrc = RegQueryValueExW(hkey, tag, NULL, &type,
                                (LPBYTE)rv, &sz)) != ERROR_SUCCESS)
        return arr;
    arr.len = sight_wcsparts(rv);
    arr.wa  = (jchar **)apr_palloc(ctx, arr.len * sizeof(jchar *));
    for (p = rv; p && *p; p++) {
        arr.wa[n++] = p;
        while (*p)
            p++;
    }
    arr.len |= SIGHT_ARR_WCHAR;
    return arr;
}

char *sight_registry_strdup(HKEY hkey, const char *tag)
{
    DWORD type = 0;
    LONG lrc;
    DWORD sz = 0;
    char *rv;

    if ((lrc = RegQueryValueExA(hkey, tag, NULL, &type,
                                NULL, &sz)) != ERROR_SUCCESS)
        return NULL;
    if (type!= REG_SZ)
        return NULL;
    if (!(rv = (char *)malloc(sz)))
        return NULL;
    if ((lrc = RegQueryValueExA(hkey, tag, NULL, &type,
                                (LPBYTE)rv, &sz)) != ERROR_SUCCESS)
        return NULL;
    return (char *)rv;
}
#endif

apr_status_t sight_registry_get_int32(HKEY hkey, const char *tag,
                                      apr_int32_t *rv)
{
    DWORD type = 0;
    LONG lrc;
    DWORD tv;
    DWORD sz;

    *rv = 0;
    sz = sizeof(DWORD);
    if ((lrc = RegQueryValueExA(hkey, tag, NULL, &type,
                                (LPBYTE)&tv, &sz)) != ERROR_SUCCESS)
        return APR_FROM_OS_ERROR(lrc);
    if (type!= REG_DWORD)
        return APR_EGENERAL;
    *rv = tv;
    return APR_SUCCESS;
}

apr_status_t sight_registry_get_int64(HKEY hkey, const char *tag,
                                      apr_int64_t *rv)
{
    DWORD type = 0;
    LONG lrc;
    DWORD sz;

    *rv = 0;
    if ((lrc = RegQueryValueExA(hkey, tag, NULL, &type,
                                NULL, NULL)) != ERROR_SUCCESS)
        return APR_FROM_OS_ERROR(lrc);
    if (type == REG_DWORD) {
        DWORD tv;
        sz = sizeof(DWORD);
        if ((lrc = RegQueryValueExA(hkey, tag, NULL, &type,
                                    (LPBYTE)&tv, &sz)) != ERROR_SUCCESS)
            return APR_FROM_OS_ERROR(lrc);
        *rv = tv;
    }
    else if (type == REG_DWORD) {
        UINT64 tv;
        sz = sizeof(UINT64);
        if ((lrc = RegQueryValueExA(hkey, tag, NULL, &type,
                                    (LPBYTE)&tv, &sz)) != ERROR_SUCCESS)
            return APR_FROM_OS_ERROR(lrc);
        *rv = tv;
    }
    else
        return APR_EGENERAL;

    return APR_SUCCESS;
}

/* Number of mili-seconds between the beginning of the Windows epoch
 * (Jan. 1, 1601) and the Unix epoch (Jan. 1, 1970)
 */
#define SIGHT_DELTA_EPOCH_IN_MSEC   APR_TIME_C(11644473600000)

jlong filetime_to_ms(FILETIME *ft)
{
    ULARGE_INTEGER i;

    i.HighPart = ft->dwHighDateTime;
    i.LowPart  = ft->dwLowDateTime;
    return (jlong)(i.QuadPart / 10000);
}

jlong winftime_to_ms(FILETIME *ft)
{
    ULARGE_INTEGER i;

    i.HighPart = ft->dwHighDateTime;
    i.LowPart  = ft->dwLowDateTime;
    if (i.QuadPart)
        return (jlong)((i.QuadPart / 10000) - SIGHT_DELTA_EPOCH_IN_MSEC);
    else
        return 0;
}

jlong largeint_to_ms(LARGE_INTEGER *li)
{
    return (jlong)(li->QuadPart / 10000);
}

jlong litime_to_ms(LARGE_INTEGER *li)
{
    if (li->QuadPart)
        return (jlong)((li->QuadPart / 10000) - SIGHT_DELTA_EPOCH_IN_MSEC);
    else
        return 0;
}

static PSID duplicateSid(PSID sid, apr_pool_t *p)
{
    PSID  pSid = NULL;
    DWORD cbSid;
    if (IsValidSid(sid)) {
        cbSid = GetLengthSid(sid);
        if (p)
            pSid = apr_palloc(p, cbSid);
        else
            pSid = malloc(cbSid);
        if (pSid) {
            if (!CopySid(cbSid, pSid, sid)) {
                return NULL;
            }
        }
    }
    return pSid;
}


/* Borrowed from apr/win32/user.c
 * and make process dependent
 */
apr_status_t sight_uid_get(HANDLE hProcess,
                           apr_uid_t *uid,
                           apr_gid_t *gid,
                           apr_pool_t *p)
{
    HANDLE hToken;
    DWORD needed;
    TOKEN_USER *usr;
    TOKEN_PRIMARY_GROUP *grp;

    if(!OpenProcessToken(hProcess,
                         TOKEN_QUERY | TOKEN_QUERY_SOURCE,
                         &hToken)) {
        if(!OpenProcessToken(hProcess,
                             TOKEN_QUERY,
                             &hToken)) {
            DWORD rc;
            PSID  psidOwner;
            PSID  psidGroup;
            PSECURITY_DESCRIPTOR pSecurityDescriptor = NULL;
            rc = GetSecurityInfo(hProcess,
                                 SE_KERNEL_OBJECT,
                                 OWNER_SECURITY_INFORMATION |
                                 GROUP_SECURITY_INFORMATION,
                                 &psidOwner,
                                 &psidGroup,
                                 NULL,
                                 NULL,
                                 &pSecurityDescriptor);
            if (rc != ERROR_SUCCESS) {
                return APR_FROM_OS_ERROR(rc);
            }
            *uid = duplicateSid(psidOwner, p);
            *gid = duplicateSid(psidGroup, p);
            LocalFree(pSecurityDescriptor);
            return APR_SUCCESS;
        }
    }

    if (!GetTokenInformation(hToken, TokenUser, NULL, 0, &needed)
        && (GetLastError() == ERROR_INSUFFICIENT_BUFFER)
        && (usr = apr_palloc(p, needed))
        && GetTokenInformation(hToken, TokenUser, usr, needed, &needed))
        *uid = usr->User.Sid;
    else
        *uid = NULL;

    if (!GetTokenInformation(hToken, TokenPrimaryGroup, NULL, 0, &needed)
        && (GetLastError() == ERROR_INSUFFICIENT_BUFFER)
        && (grp = apr_palloc(p, needed))
        && GetTokenInformation(hToken, TokenPrimaryGroup, grp, needed, &needed))
        *gid = grp->PrimaryGroup;
    else
        *gid = NULL;
    CloseHandle(hToken);
    return APR_SUCCESS;
}

PSID sight_get_sid(LPCWSTR name, PSID_NAME_USE sidtype)
{
    WCHAR domain[MAX_PATH];
    DWORD domlen = MAX_PATH;
    DWORD sidlen = 0;
    PSID  sid = NULL;

    *sidtype = -1;
    LookupAccountNameW(NULL, name, NULL, &sidlen,
                       NULL, &domlen, sidtype);
    if (sidlen) {
        /* Give it back on the second pass with proper sidlen
         */
        if (!(sid = malloc(sidlen)))
            return NULL;
        domlen = MAX_PATH;
        if (!LookupAccountNameW(NULL, name, sid, &sidlen,
                                domain, &domlen, sidtype)) {
            free(sid);
            sid = NULL;
        }
    }
    return sid;
}

SID_NAME_USE get_sid_name(LPWSTR buf, size_t blen, PSID psid)
{
    WCHAR domain[MAX_PATH];
    DWORD domlen = MAX_PATH;
    DWORD acclen = (DWORD)blen;
    SID_NAME_USE sidtype;

    if (IsWellKnownSid(psid, WinBuiltinAdministratorsSid)) {
        lstrcpyW(buf, L"SYSTEM");
        return SidTypeWellKnownGroup;
    }
    if (!LookupAccountSidW(NULL, psid,
                           buf, &acclen,
                           domain, &domlen,
                           &sidtype)) {

        sidtype = -1;
        buf[0]  = L'\0';
    }
    return sidtype;
}
