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

#define SIGHT_WANT_LATE_DLL
#include "sight_private.h"

/*
 * Network implementation
 */

J_DECLARE_CLAZZ = {
    NULL,
    NULL,
    SIGHT_CLASS_PATH "Network"
};

J_DECLARE_F_ID(0000) = {
    NULL,
    "HostName",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0001) = {
    NULL,
    "DomainName",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0002) = {
    NULL,
    "DnsServerAddresses",
    "[L" SIGHT_CLASS_PATH "NetworkAddress;"
};

SIGHT_CLASS_LDEF(Network)
{
    if (sight_load_class(_E, &_clazzn))
        return 1;
    J_LOAD_IFIELD(0000);
    J_LOAD_IFIELD(0001);
    J_LOAD_IFIELD(0002);

    return 0;
}

SIGHT_CLASS_UDEF(Network)
{
    sight_unload_class(_E, &_clazzn);
}

SIGHT_EXPORT_DECLARE(void, Network, info0)(SIGHT_STDARGS,
                                           jobject thiz,
                                           jlong pool)

{
    PFIXED_INFO pInfo = NULL;
    DWORD rc;
    ULONG outBufLen = 0;
    PIP_ADDR_STRING da;
    jsize len, idx;

    UNREFERENCED_O;


    // Make an initial call to GetAdaptersAddresses to get the
    // size needed into the outBufLen variable
    rc = GetNetworkParams(NULL, &outBufLen);
    if (rc != ERROR_BUFFER_OVERFLOW) {
        throwAprException(_E, APR_FROM_OS_ERROR(rc));
        goto cleanup;
    }
    if (!(pInfo = (PFIXED_INFO)sight_malloc(_E, outBufLen,
                                            THROW_FMARK))) {
        goto cleanup;
    }
    // Make a second call to GetAdapters Addresses to get the
    // actual data we want
    rc = GetNetworkParams(pInfo, &outBufLen);
    if (rc != ERROR_SUCCESS) {
        throwAprException(_E, APR_FROM_OS_ERROR(rc));
        goto cleanup;
    }
    SET_IFIELD_S(0000, thiz, pInfo->HostName);
    if (*pInfo->DomainName) {
        SET_IFIELD_S(0001, thiz, pInfo->DomainName);
    }
    da = &pInfo->DnsServerList;
    len = 0;
    while (da) {
        if (*da->IpAddress.String)
            len++;
        da = da->Next;
    }
    if (len) {
        jobject addr;
        jobjectArray aaddr;
        aaddr = sight_new_netaddr_array(_E, _O, len);
        if (!aaddr || (*_E)->ExceptionCheck(_E)) {
            goto cleanup;
        }
        da = &pInfo->DnsServerList;
        idx = 0;
        while (da) {
            addr = sight_new_netaddr_class(_E, _O);
            if (!addr || (*_E)->ExceptionCheck(_E)) {
                goto cleanup;
            }
            sight_netaddr_set_addr(_E, addr, da->IpAddress.String);
            sight_netaddr_set_family(_E, addr, AF_INET);
            if (*da->IpMask.String)
                sight_netaddr_set_mask(_E, addr, da->IpMask.String);
            (*_E)->SetObjectArrayElement(_E, aaddr, idx++, addr);
            (*_E)->DeleteLocalRef(_E, addr);
            da = da->Next;
        }
        SET_IFIELD_O(0002, thiz, aaddr);
        (*_E)->DeleteLocalRef(_E, aaddr);
    }

cleanup:
    SIGHT_FREE(pInfo);
}

