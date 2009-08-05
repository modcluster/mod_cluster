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
 * WMI windows utilities
 *
 */

#include "sight.h"
#include "sight_local.h"
#include "sight_types.h"
#include "sight_private.h"

#include <comdef.h>
#include <wbemidl.h>
#pragma comment(lib, "wbemuuid.lib")

typedef struct {
    IWbemLocator            *pLoc;
    IWbemServices           *pSvc;
    IEnumWbemClassObject    *pEnumerator;
    IWbemClassObject        *pclsObj;
    IWbemClassObject        *pclsInstance;
    IWbemClassObject        *pInParams;
    IWbemClassObject        *pOutParams;
    ULONG                    dwCount;
} wmi_data_t;

#define DEFAULT_WMI_NS      L"ROOT\\CIMV2"

static int cosec_inited = 0;

extern "C"
void *wmi_intialize(JNIEnv *_E, const jchar *ns, jsize nsl)
{
    wmi_data_t *wd;
    HRESULT hres;

    if (!ns) {
        ns = DEFAULT_WMI_NS;
        nsl = (sizeof(DEFAULT_WMI_NS) - 1) / sizeof(WCHAR);
    }
    // Step 1: --------------------------------------------------
    // Initialize COM. ------------------------------------------

    hres =  CoInitializeEx(0, COINIT_MULTITHREADED);
    if (FAILED(hres)) {
        throwAprException(_E, APR_FROM_OS_ERROR(hres));
        return NULL;
    }

    // Step 2: --------------------------------------------------
    // Set general COM security levels --------------------------
    // Note: If you are using Windows 2000, you need to specify -
    // the default authentication credentials for a user by using
    // a SOLE_AUTHENTICATION_LIST structure in the pAuthList ----
    // parameter of CoInitializeSecurity ------------------------
    if (!cosec_inited) {
        hres =  CoInitializeSecurity(
            NULL,
            -1,                          // COM authentication
            NULL,                        // Authentication services
            NULL,                        // Reserved
            RPC_C_AUTHN_LEVEL_DEFAULT,   // Default authentication
            RPC_C_IMP_LEVEL_IMPERSONATE, // Default Impersonation
            NULL,                        // Authentication info
            EOAC_NONE,                   // Additional capabilities
            NULL                         // Reserved
            );
        if (FAILED(hres)) {
            throwAprException(_E, APR_FROM_OS_ERROR(hres));
            CoUninitialize();
            return NULL;                 // Program has failed.
        }
        cosec_inited = 1;
    }

    if (!(wd = (wmi_data_t *)sight_calloc(_E, sizeof(wmi_data_t),
                                          THROW_FMARK))) {
        return NULL;
    }
    hres = CoCreateInstance(
                CLSID_WbemLocator,
                0,
                CLSCTX_INPROC_SERVER,
                IID_IWbemLocator, (LPVOID *)&wd->pLoc);

    if (FAILED(hres)) {
        throwAprException(_E, APR_FROM_OS_ERROR(hres));
        wd->pLoc = NULL;
        free(wd);
        return NULL;
    }

    BSTR bns = SysAllocStringLen(ns, nsl);
    // Connect to the root\cimv2 namespace with the
    // current user and obtain pointer pSvc
    // to make IWbemServices calls.

    hres = wd->pLoc->ConnectServer(
        bns,                        // WMI namespace
        NULL,                       // User name
        NULL,                       // User password
        0,                          // Locale
        NULL,                       // Security flags
        0,                          // Authority
        0,                          // Context object
        &wd->pSvc                   // IWbemServices proxy
        );
    SysFreeString(bns);
    if (FAILED(hres)) {
        throwAprException(_E, APR_FROM_OS_ERROR(hres));
        wd->pLoc->Release();
        CoUninitialize();
        wd->pLoc = NULL;
        wd->pSvc = NULL;
        goto cleanup;
    }

    // Set the IWbemServices proxy so that impersonation
    // of the user (client) occurs.
    hres = CoSetProxyBlanket(
        wd->pSvc,                   // the proxy to set
        RPC_C_AUTHN_WINNT,          // authentication service
        RPC_C_AUTHZ_NONE,           // authorization service
        NULL,                       // Server principal name
        RPC_C_AUTHN_LEVEL_CALL,     // authentication level
        RPC_C_IMP_LEVEL_IMPERSONATE,// impersonation level
        NULL,                       // client identity
        EOAC_NONE                   // proxy capabilities
    );

    if (FAILED(hres)) {
        throwAprException(_E, APR_FROM_OS_ERROR(hres));
        wd->pSvc->Release();
        wd->pLoc->Release();
        CoUninitialize();
        wd->pLoc = NULL;
        wd->pSvc = NULL;
        goto cleanup;
    }

    return wd;
cleanup:
    free(wd);
    return NULL;
}

extern "C"
void wmi_terminate(void *wmi)
{
    wmi_data_t *wd = (wmi_data_t *)wmi;

    if (!wd)
        return;
    if (wd->pEnumerator)
        wd->pEnumerator->Release();
    if (wd->pInParams)
        wd->pInParams->Release();
    if (wd->pOutParams)
        wd->pOutParams->Release();
    if (wd->pclsObj)
        wd->pclsObj->Release();
    if (wd->pSvc)
        wd->pSvc->Release();
    if (wd->pLoc)
        wd->pLoc->Release();
    CoUninitialize();
    free(wd);
}

extern "C"
int wmi_query(void *wmi, const jchar *lang, jsize langl,
              const jchar *query, jsize queryl)
{
    HRESULT hres;
    wmi_data_t *wd = (wmi_data_t *)wmi;

    if (!wd)
        return APR_EINVAL;
    if (wd->pEnumerator)
        wd->pEnumerator->Release();
    BSTR blang  = SysAllocStringLen(lang, langl);
    BSTR bquery = SysAllocStringLen(query, queryl);
    hres = wd->pSvc->ExecQuery(
        blang,
        bquery,
        WBEM_FLAG_FORWARD_ONLY | WBEM_FLAG_RETURN_IMMEDIATELY,
        NULL,
        &wd->pEnumerator);
    SysFreeString(blang);
    SysFreeString(bquery);
    if (FAILED(hres)) {
        wd->pEnumerator = NULL;
        return APR_FROM_OS_ERROR(hres);
    }

    return APR_SUCCESS;
}

extern "C"
int wmi_query_next(void *wmi)
{
    HRESULT hres;
    wmi_data_t *wd = (wmi_data_t *)wmi;

    if (!wd || !wd->pEnumerator)
        return APR_EINVAL;

    hres = wd->pEnumerator->Next(
        WBEM_INFINITE,
        1,
        &wd->pclsObj,
        &wd->dwCount);

    if (SUCCEEDED(hres)) {
        if (wd->dwCount)
            return APR_SUCCESS;
        else
            return APR_EOF;
    }
    else
        return APR_EOF;
}

extern "C"
int wmi_query_skip(void *wmi, int count)
{
    HRESULT hres;
    wmi_data_t *wd = (wmi_data_t *)wmi;

    if (!wd || !wd->pEnumerator)
        return APR_EINVAL;

    hres = wd->pEnumerator->Skip(
        WBEM_INFINITE,
        count);

    if (SUCCEEDED(hres)) {
        return APR_SUCCESS;
    }
    else
        return APR_EOF;
}

extern "C"
int wmi_query_reset(void *wmi)
{
    HRESULT hres;
    wmi_data_t *wd = (wmi_data_t *)wmi;

    if (!wd || !wd->pEnumerator)
        return APR_EINVAL;

    hres = wd->pEnumerator->Reset();

    if (SUCCEEDED(hres)) {
        return APR_SUCCESS;
    }
    else
        return APR_EOF;
}

extern "C"
void *wmi_query_get(JNIEnv *_E, void *wmi, const jchar *prop, jsize propl)
{
    HRESULT hres;
    wmi_data_t *wd = (wmi_data_t *)wmi;

    if (!wd || !wd->pclsObj)
        return NULL;

    CIMTYPE type;
    VARIANT *v = (VARIANT *)alloc_variant();
    if (v) {
        VariantInit(v);
        BSTR bprop = SysAllocStringLen(prop, propl);
        hres = wd->pclsObj->Get(bprop, 0, v, &type, NULL);
        SysFreeString(bprop);
        if (SUCCEEDED(hres)) {
            return v;
        }
    }
    free_variant(v);
    return NULL;
}
