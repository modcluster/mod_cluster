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
 * Service windows implementation
 *
 */

#include "sight.h"
#include "sight_local.h"
#include "sight_types.h"

#define SIGHT_WANT_LATE_DLL
#include "sight_private.h"

typedef struct windows_svc_t {
    HANDLE  handle;
} windows_svc_t;


J_DECLARE_CLAZZ = {
    NULL,
    NULL,
    SIGHT_CLASS_PATH "Service"
};

J_DECLARE_F_ID(0000) = {
    NULL,
    "Name",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0001) = {
    NULL,
    "BinaryPathName",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0002) = {
    NULL,
    "Dependencies",
    "[Ljava/lang/String;"
};

J_DECLARE_F_ID(0003) = {
    NULL,
    "ServiceStartName",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0004) = {
    NULL,
    "DisplayName",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0005) = {
    NULL,
    "Description",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0006) = {
    NULL,
    "LoadOrderGroup",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0007) = {
    NULL,
    "ExitCode",
    "I"
};

J_DECLARE_F_ID(0008) = {
    NULL,
    "ServiceSpecificExitCode",
    "I"
};

J_DECLARE_F_ID(0009) = {
    NULL,
    "ProcessId",
    "I"
};

J_DECLARE_M_ID(0000) = {
    NULL,
    "setState",
    "(I)V"
};

SIGHT_CLASS_LDEF(Service)
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
    J_LOAD_METHOD(0000);

    return 0;
}

SIGHT_CLASS_UDEF(Service)
{
    sight_unload_class(_E, &_clazzn);
}

static void svc_cleanup(int mode, sight_object_t *no)
{
    if (no && !IS_INVALID_HANDLE(no->native)) {
        CloseServiceHandle(no->native);
        no->native = NULL;
    }
}

SIGHT_EXPORT_DECLARE(jint, Service, open0)(SIGHT_STDARGS,
                                           jobject thiz,
                                           jlong instance,
                                           jlong scm,
                                           jstring name,
                                           jint access)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    sight_object_t *ns = J2P(scm, sight_object_t *);
    SC_HANDLE hscm;
    SC_HANDLE hsvc;
    DWORD rc = ERROR_SUCCESS;
    LPQUERY_SERVICE_CONFIGW lpServiceConfig = NULL;
    DWORD cbBufSize;
    DWORD cbBytesNeeded;
    LPSERVICE_STATUS_PROCESS lpStatus;
    SIGHT_ALLOC_CSTRING(name);
    BYTE buf[1024];
    UNREFERENCED_O;

    if (!name) {
        throwArgumentException(_E, THROW_FMARK, NULL);
        return APR_EINVAL;
    }
    if (!no || !no->pool) {
        SIGHT_FREE_CSTRING(name);
        throwArgumentException(_E, THROW_FMARK, NULL);
        return APR_EINVAL;
    }
    if (!ns || !ns->native) {
        SIGHT_FREE_CSTRING(name);
        throwArgumentException(_E, THROW_FMARK, NULL);
        return APR_EINVAL;
    }
    SIGHT_LOCAL_TRY(ns) {
    SIGHT_LOCAL_TRY(no) {
        hscm = ns->native;
        if (no->native) {
            CloseServiceHandle(no->native);
            no->native = NULL;
        }
        hsvc = OpenServiceA(hscm, J2S(name), (DWORD)access);
        if (IS_INVALID_HANDLE(hsvc)) {
            rc = GetLastError();
            SIGHT_LOCAL_BRK(no);
            SIGHT_LOCAL_BRK(ns);
            goto cleanup;
        }

        if (!QueryServiceConfigW(hsvc, NULL,
                                 0, &cbBytesNeeded)) {
            rc = GetLastError();
            if (rc == ERROR_INSUFFICIENT_BUFFER) {
                cbBufSize = cbBytesNeeded;
                lpServiceConfig = (LPQUERY_SERVICE_CONFIGW)malloc(cbBufSize);
                if (!lpServiceConfig) {
                    rc = ERROR_OUTOFMEMORY;
                    throwAprMemoryException(_E, THROW_FMARK,
                                            apr_get_os_error());
                    SIGHT_LOCAL_BRK(no);
                    SIGHT_LOCAL_BRK(ns);
                    goto cleanup;
                }
                if (!QueryServiceConfigW(hsvc, lpServiceConfig,
                                         cbBufSize, &cbBytesNeeded)) {
                    rc = GetLastError();
                    SIGHT_LOCAL_BRK(no);
                    SIGHT_LOCAL_BRK(ns);
                    goto cleanup;
                }
            }
            else {
                SIGHT_LOCAL_BRK(no);
                SIGHT_LOCAL_BRK(ns);
                goto cleanup;
            }
        }
        SET_IFIELD_W(0001, thiz, lpServiceConfig->lpBinaryPathName);
        SET_IFIELD_O(0002, thiz, sight_mw_to_sa(_E, lpServiceConfig->lpDependencies));
        SET_IFIELD_W(0003, thiz, lpServiceConfig->lpServiceStartName);
        SET_IFIELD_W(0004, thiz, lpServiceConfig->lpDisplayName);
        SET_IFIELD_W(0006, thiz, lpServiceConfig->lpLoadOrderGroup);

        if (!QueryServiceConfig2W(hsvc, SERVICE_CONFIG_DESCRIPTION,
                                  NULL, 0, &cbBytesNeeded)) {
            BYTE  *lpBuffer;
            if (rc != ERROR_INSUFFICIENT_BUFFER) {
                SIGHT_LOCAL_BRK(no);
                SIGHT_LOCAL_BRK(ns);
                goto cleanup;
            }
            cbBufSize = cbBytesNeeded;
            lpBuffer = (BYTE *)malloc(cbBufSize);
            if (!lpServiceConfig) {
                rc = ERROR_OUTOFMEMORY;
                throwAprMemoryException(_E, THROW_FMARK,
                                        apr_get_os_error());
                SIGHT_LOCAL_BRK(no);
                SIGHT_LOCAL_BRK(ns);
                goto cleanup;
            }
            if (!QueryServiceConfig2W(hsvc, SERVICE_CONFIG_DESCRIPTION,
                                      lpBuffer, cbBufSize, &cbBytesNeeded)) {
                rc = GetLastError();
                free(lpBuffer);
                SIGHT_LOCAL_BRK(no);
                SIGHT_LOCAL_BRK(ns);
                goto cleanup;
            }
            else {
                LPSERVICE_DESCRIPTIONW lpDesc = (LPSERVICE_DESCRIPTIONW)lpBuffer;
                SET_IFIELD_W(0005, thiz, lpDesc->lpDescription);
            }
            free(lpBuffer);
        }
        if (!QueryServiceStatusEx(hsvc, SC_STATUS_PROCESS_INFO,
                                  buf, sizeof(buf), &cbBytesNeeded)) {
            rc = GetLastError();
        }
        else {
            lpStatus = (LPSERVICE_STATUS_PROCESS)&buf[0];
            SET_IFIELD_I(0007, thiz, lpStatus->dwWin32ExitCode);
            SET_IFIELD_I(0008, thiz, lpStatus->dwServiceSpecificExitCode);
            SET_IFIELD_I(0009, thiz, lpStatus->dwProcessId);
            CALL_METHOD1(0000, thiz, lpStatus->dwCurrentState);

            no->native = hsvc;
            no->clean  = svc_cleanup;
            rc = ERROR_SUCCESS;
        }
    } SIGHT_LOCAL_END(no);
    } SIGHT_LOCAL_END(ns);
cleanup:
    if (lpServiceConfig)
        free(lpServiceConfig);
    SIGHT_FREE_CSTRING(name);
    return APR_FROM_OS_ERROR(rc);
}

SIGHT_EXPORT_DECLARE(jint, Service, ctrl0)(SIGHT_STDARGS,
                                           jlong instance,
                                           jint cmd)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    SC_HANDLE hsvc;
    DWORD rc = ERROR_SUCCESS;
    SERVICE_STATUS stStatus;
    DWORD sc = cmd;

    UNREFERENCED_O;
    if (!no || !no->native || cmd < 0) {
        return APR_EINVAL;
    }
    SIGHT_LOCAL_TRY(no) {
        hsvc = no->native;

        switch (cmd) {
            case 1:
                sc = SERVICE_CONTROL_STOP;
            break;
            case 2:
                sc = SERVICE_CONTROL_PAUSE;
            break;
            case 3:
                sc = SERVICE_CONTROL_CONTINUE;
            break;
            case 4:
                sc = SERVICE_CONTROL_INTERROGATE;
            break;
            case 5:
                SIGHT_LOCAL_BRK(no);
                return APR_EINVAL;
            break;
            case 6:
                sc = SERVICE_CONTROL_PARAMCHANGE;
            break;
        }
        if (cmd == 0) {
            if (!StartService(hsvc, 0, NULL))
                rc = GetLastError();
        }
        else {
            if (!ControlService(hsvc, sc, &stStatus))
               rc = GetLastError();
        }
    } SIGHT_LOCAL_END(no);
    return APR_FROM_OS_ERROR(rc);
}

SIGHT_EXPORT_DECLARE(jint, Service, stats0)(SIGHT_STDARGS,
                                            jobject thiz,
                                            jlong instance)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    SC_HANDLE hsvc;
    DWORD rc = ERROR_SUCCESS;
    DWORD cbBytesNeeded;
    LPSERVICE_STATUS_PROCESS lpStatus;
    BYTE buf[1024];

    UNREFERENCED_O;
    if (!no || !no->native) {
        return APR_EINVAL;
    }
    SIGHT_LOCAL_TRY(no) {
        hsvc = no->native;

        if (!QueryServiceStatusEx(hsvc, SC_STATUS_PROCESS_INFO,
                                  buf, sizeof(buf), &cbBytesNeeded)) {
            rc = GetLastError();
        }
        else {
            lpStatus = (LPSERVICE_STATUS_PROCESS)&buf[0];
            SET_IFIELD_I(0007, thiz, lpStatus->dwWin32ExitCode);
            SET_IFIELD_I(0008, thiz, lpStatus->dwServiceSpecificExitCode);
            SET_IFIELD_I(0009, thiz, lpStatus->dwProcessId);
            CALL_METHOD1(0000, thiz, lpStatus->dwCurrentState);
        }
    } SIGHT_LOCAL_END(no);
    return APR_FROM_OS_ERROR(rc);
}

SIGHT_EXPORT_DECLARE(jint, Service, wait0)(SIGHT_STDARGS,
                                            jobject thiz,
                                            jlong instance,
                                            jlong timeout,
                                            jint state)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    SC_HANDLE hsvc;
    DWORD rc = ERROR_SUCCESS;
    DWORD cbBytesNeeded;
    LPSERVICE_STATUS_PROCESS lpStatus;
    BYTE buf[1024];
    apr_interval_time_t timeout_value, timeout_interval;

    UNREFERENCED_O;
    if (!no || !no->native) {
        return APR_EINVAL;
    }
    SIGHT_LOCAL_TRY(no) {
        hsvc = no->native;
        if (!QueryServiceStatusEx(hsvc, SC_STATUS_PROCESS_INFO,
                                  buf, sizeof(buf), &cbBytesNeeded)) {
            rc = GetLastError();
            SIGHT_LOCAL_BRK(no);
            goto cleanup;
        }
        lpStatus = (LPSERVICE_STATUS_PROCESS)&buf[0];
        if (lpStatus->dwCurrentState == (DWORD)state)
            goto setup;
        if (timeout > 0) {
            timeout_value = timeout * 1000L;
            timeout_interval = timeout_value / 64;
        }
        else {
            /* Defaults to 100 ms */
            timeout_interval = 100000L;
        }
        do {
            if (SIGHT_LOCAL_IRQ(no)) {
                rc = APR_TO_OS_ERROR(APR_TIMEUP);
                break;
            }
            apr_sleep(timeout_interval);
            if (!QueryServiceStatusEx(hsvc, SC_STATUS_PROCESS_INFO,
                                      buf, sizeof(buf), &cbBytesNeeded)) {
                rc = GetLastError();
                SIGHT_LOCAL_BRK(no);
                goto cleanup;
            }
            lpStatus = (LPSERVICE_STATUS_PROCESS)&buf[0];
            if (timeout > 0) {
                if (timeout_interval >= timeout_value) {
                    rc = APR_TO_OS_ERROR(APR_TIMEUP);
                    break;
                }
                timeout_interval *= 2;
            }

        } while (lpStatus->dwCurrentState != (DWORD)state);
setup:
        lpStatus = (LPSERVICE_STATUS_PROCESS)&buf[0];
        SET_IFIELD_I(0007, thiz, lpStatus->dwWin32ExitCode);
        SET_IFIELD_I(0008, thiz, lpStatus->dwServiceSpecificExitCode);
        SET_IFIELD_I(0009, thiz, lpStatus->dwProcessId);
        CALL_METHOD1(0000, thiz, lpStatus->dwCurrentState);
    } SIGHT_LOCAL_END(no);
cleanup:
    return APR_FROM_OS_ERROR(rc);
}

SIGHT_EXPORT_DECLARE(jint, Service, wait1)(SIGHT_STDARGS,
                                            jobject thiz,
                                            jlong instance,
                                            jobject progress,
                                            jlong timeout,
                                            jint state)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    SC_HANDLE hsvc;
    DWORD rc = ERROR_SUCCESS;
    DWORD cbBytesNeeded;
    LPSERVICE_STATUS_PROCESS lpStatus;
    BYTE buf[1024];
    apr_interval_time_t timeout_value, timeout_interval;
    jclass c;
    jint   cres;
    jint   tick = 0;
    sight_callback_t cb;

    UNREFERENCED_O;
    if (!no || !no->native || !progress) {
        return APR_EINVAL;
    }
    SIGHT_LOCAL_TRY(no) {
        hsvc = no->native;

        if (!QueryServiceStatusEx(hsvc, SC_STATUS_PROCESS_INFO,
                                  buf, sizeof(buf), &cbBytesNeeded)) {
            rc = GetLastError();
            SIGHT_LOCAL_BRK(no);
            goto cleanup;
        }
        lpStatus = (LPSERVICE_STATUS_PROCESS)&buf[0];
        if (lpStatus->dwCurrentState == (DWORD)state)
            goto setup;
        c = (*_E)->GetObjectClass(_E, progress);
        cb.name   = "progress";
        cb.msig   = "(I)I";
        cb.object = progress;
        cb.method = (*_E)->GetMethodID(_E, c, cb.name, cb.msig);
        if (!cb.method || (*_E)->ExceptionCheck(_E)) {
            rc = EINVAL;
            SIGHT_LOCAL_BRK(no);
            goto cleanup;
        }
        cres = (*_E)->CallIntMethod(_E, cb.object, cb.method, tick, NULL);
        if ((*_E)->ExceptionCheck(_E)) {
            rc = EINTR;
            SIGHT_LOCAL_BRK(no);
            goto cleanup;
        }
        if (timeout > 0) {
            timeout_value = timeout * 1000L;
            timeout_interval = timeout_value / 100;
        }
        else {
            /* Defaults to 100 ms */
            timeout_interval = 100000L;
        }
        do {
            if (cres < 0) {
                /* Broken by the callback */
                break;
            }
            else if (cres > 0) {
                timeout_interval = cres * 1000L;
            }
            if (SIGHT_LOCAL_IRQ(no)) {
                rc = APR_TO_OS_ERROR(APR_TIMEUP);
                break;
            }
            apr_sleep(timeout_interval);
            if (!QueryServiceStatusEx(hsvc, SC_STATUS_PROCESS_INFO,
                                      buf, sizeof(buf), &cbBytesNeeded)) {
                rc = GetLastError();
                SIGHT_LOCAL_BRK(no);
                goto cleanup;
            }
            lpStatus = (LPSERVICE_STATUS_PROCESS)&buf[0];
            if (timeout > 0) {
                if (timeout_interval >= timeout_value) {
                    rc = APR_TO_OS_ERROR(APR_TIMEUP);
                    break;
                }
                timeout_value -= timeout_interval;
            }
            cres = (*_E)->CallIntMethod(_E, cb.object, cb.method, tick++, NULL);
            if ((*_E)->ExceptionCheck(_E)) {
                rc = EINTR;
                SIGHT_LOCAL_BRK(no);
                goto cleanup;
            }
        } while (lpStatus->dwCurrentState != (DWORD)state);
setup:
        lpStatus = (LPSERVICE_STATUS_PROCESS)&buf[0];
        SET_IFIELD_I(0007, thiz, lpStatus->dwWin32ExitCode);
        SET_IFIELD_I(0008, thiz, lpStatus->dwServiceSpecificExitCode);
        SET_IFIELD_I(0009, thiz, lpStatus->dwProcessId);
        CALL_METHOD1(0000, thiz, lpStatus->dwCurrentState);
    } SIGHT_LOCAL_END(no);
cleanup:
    return APR_FROM_OS_ERROR(rc);
}
