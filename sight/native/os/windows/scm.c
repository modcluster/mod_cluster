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
 * Service Control Manager windows implementation
 *
 */

#include "sight.h"
#include "sight_local.h"
#include "sight_types.h"

#define SIGHT_WANT_LATE_DLL
#include "sight_private.h"

typedef struct svc_enum_t {
    void   *next;
    DWORD   size;
    BYTE    data[1];
} svc_enum_t;

static void free_enum_list(svc_enum_t *head)
{
    svc_enum_t *next;
    while (head) {
        next = head->next;
        free(head);
        head = next;
    }
}

static void scm_cleanup(int mode, sight_object_t *no)
{
    if (no && no->native) {
        CloseServiceHandle(no->native);
        no->native = NULL;
    }
}


SIGHT_EXPORT_DECLARE(jint, ServiceControlManager, open0)(SIGHT_STDARGS,
                                                         jlong instance,
                                                         jstring database,
                                                         jint mode)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    DWORD rc = ERROR_SUCCESS;
    SIGHT_ALLOC_WSTRING(database);

    UNREFERENCED_O;
    SIGHT_INIT_WSTRING(database);

    if (!no || !no->pool) {
        SIGHT_FREE_WSTRING(database);
        return APR_EINVAL;
    }
    SIGHT_LOCAL_TRY(no) {
        if (no->native)
            CloseServiceHandle(no->native);
        no->native = OpenSCManagerW(NULL, J2C(database), mode);
        if (!no->native)
            rc = GetLastError();
        else
            no->clean = scm_cleanup;
    } SIGHT_LOCAL_END(no);
    SIGHT_FREE_WSTRING(database);
    return APR_FROM_OS_ERROR(rc);
}

SIGHT_EXPORT_DECLARE(void, ServiceControlManager, close0)(SIGHT_STDARGS,
                                                          jlong instance)
{
    sight_object_t *no = J2P(instance, sight_object_t *);

    UNREFERENCED_STDARGS;


    if (no && no->native) {
        SIGHT_LOCAL_TRY(no) {
            CloseServiceHandle(no->native);
            no->native = NULL;
            no->clean  = NULL;
        } SIGHT_LOCAL_END(no);
    }
}

SIGHT_EXPORT_DECLARE(jobjectArray, ServiceControlManager,
                     enum0)(SIGHT_STDARGS, jlong instance,
                            jint drivers, jint what)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    jobjectArray ea = NULL;
    jsize cnt = 0;
    jint  idx = 0;
    DWORD rc;
    LPENUM_SERVICE_STATUS_PROCESSA lpService;
    DWORD dwSize = 0;
    DWORD dwNumServices = 0;
    DWORD dwResumeHandle = 0;
    DWORD dwServiceType;
    DWORD dwServiceState = SERVICE_STATE_ALL;
    BOOL  rt;
    void *lpBuffer = NULL;
    svc_enum_t *head = NULL;
    svc_enum_t *list = NULL;

    if (!no || !no->pool || !no->native) {
        return NULL;
    }

    if (drivers)
        dwServiceType   = SERVICE_DRIVER;
    else
        dwServiceType   = SERVICE_WIN32;
    if (what == 1)
        dwServiceState  = SERVICE_ACTIVE;
    else if (what == 2)
        dwServiceState  = SERVICE_INACTIVE;
    SIGHT_LOCAL_TRY(no) {
        do {
            if (SIGHT_LOCAL_IRQ(no)) {
                ea = NULL;
                SIGHT_LOCAL_BRK(no);
                goto cleanup;
            }
            rt = EnumServicesStatusExA(no->native,
                                       SC_ENUM_PROCESS_INFO,
                                       dwServiceType,
                                       dwServiceState,
                                       (LPBYTE)lpBuffer,
                                       dwSize,
                                       &dwSize,
                                       &dwNumServices,
                                       &dwResumeHandle,
                                       NULL);
            if (!rt && ((rc = GetLastError()) != ERROR_MORE_DATA)) {
                throwAprException(_E, APR_FROM_OS_ERROR(rc));
                ea = NULL;
                SIGHT_LOCAL_BRK(no);
                goto cleanup;
            }
            if (dwNumServices > 0) {


            }
            if (!rt) {
                if (!(list = (svc_enum_t *)sight_malloc(_E,
                                                dwSize + sizeof(svc_enum_t),
                                                THROW_FMARK))) {
                    ea = NULL;
                    SIGHT_LOCAL_BRK(no);
                    goto cleanup;
                }
                list->next = head;
                lpBuffer = &list->data[0];
                /* TODO: Make list ordered as its populated */
                head = list;
            }
            list->size = dwNumServices;
            cnt += list->size;
        } while (!rt);

        ea = sight_new_cc_array(_E, SIGHT_CC_STRING, cnt);
        if (!ea || (*_E)->ExceptionCheck(_E)) {
            ea = NULL;
            SIGHT_LOCAL_BRK(no);
            goto cleanup;
        }
        while (head) {
            DWORD i;
            lpService = (LPENUM_SERVICE_STATUS_PROCESSA)&head->data[0];
            for (i = 0; i < head->size; i++) {
                jstring s = CSTR_TO_JSTRING(lpService[i].lpServiceName);
                if (s)
                    (*_E)->SetObjectArrayElement(_E, ea, idx++, s);
                else
                    break;
                if ((*_E)->ExceptionCheck(_E)) {
                    ea = NULL;
                    break;
                }
                (*_E)->DeleteLocalRef(_E, s);
            }
            head = head->next;
        }
    } SIGHT_LOCAL_END(no);
cleanup:
    free_enum_list(head);
    return ea;
}
