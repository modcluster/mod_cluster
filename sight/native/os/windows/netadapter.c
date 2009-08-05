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
 * Network adapter implementation
 */

J_DECLARE_CLAZZ = {
    NULL,
    NULL,
    SIGHT_CLASS_PATH "NetworkAdapter"
};

J_DECLARE_F_ID(0000) = {
    NULL,
    "Name",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0001) = {
    NULL,
    "Description",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0002) = {
    NULL,
    "FriendlyName",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0003) = {
    NULL,
    "MacAddress",
    "L" SIGHT_CLASS_PATH "NetworkAddress;"
};

J_DECLARE_F_ID(0004) = {
    NULL,
    "DhcpEnabled",
    "Z"
};

J_DECLARE_F_ID(0005) = {
    NULL,
    "DhcpServer",
    "L" SIGHT_CLASS_PATH "NetworkAddress;"
};

J_DECLARE_F_ID(0006) = {
    NULL,
    "UnicastAddresses",
    "[L" SIGHT_CLASS_PATH "NetworkAddress;"
};

J_DECLARE_F_ID(0007) = {
    NULL,
    "MulticastAddresses",
    "[L" SIGHT_CLASS_PATH "NetworkAddress;"
};

J_DECLARE_F_ID(0008) = {
    NULL,
    "DnsServerAddresses",
    "[L" SIGHT_CLASS_PATH "NetworkAddress;"
};

J_DECLARE_F_ID(0009) = {
    NULL,
    "Mtu",
    "I"
};

J_DECLARE_M_ID(0000) = {
    NULL,
    "setType",
    "(I)V"
};

J_DECLARE_M_ID(0001) = {
    NULL,
    "setStat",
    "(I)V"
};

SIGHT_CLASS_LDEF(NetworkAdapter)
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
    J_LOAD_METHOD(0001);

    return 0;
}

SIGHT_CLASS_UDEF(NetworkAdapter)
{
    sight_unload_class(_E, &_clazzn);
}

typedef struct net_adapter_enum_t {
    int                   isInfo;
    PIP_ADAPTER_ADDRESSES pAddresses;
    PIP_ADAPTER_ADDRESSES pCurrent;
    PIP_ADAPTER_INFO      pInfo;
    PIP_ADAPTER_INFO      pCurInfo;
    int                   devCounters[8];
    PMIB_IFTABLE          pTable;
} net_adapter_enum_t;

static int if_enum(SIGHT_STDARGS, net_adapter_enum_t *e)
{
    DWORD i, rc;
    DWORD dwSize;

    dwSize = sizeof(MIB_IFTABLE);
    if (!(e->pTable = sight_calloc(_E, dwSize, THROW_FMARK)))
        return errno;

    rc = GetIfTable(e->pTable, &dwSize, TRUE);
    if (rc == ERROR_INSUFFICIENT_BUFFER || rc == ERROR_BUFFER_OVERFLOW) {
        free(e->pTable);
        if (!(e->pTable = sight_calloc(_E, dwSize, THROW_FMARK)))
            return errno;
        rc = GetIfTable(e->pTable, &dwSize, TRUE);
    }
    for (i = 0; i < e->pTable->dwNumEntries; i++) {
        switch (e->pTable->table[i].dwType) {
            case MIB_IF_TYPE_ETHERNET:
                swprintf(e->pTable->table[i].wszName, L"eth%d",  e->devCounters[0]++);
            break;
            case MIB_IF_TYPE_TOKENRING:
                swprintf(e->pTable->table[i].wszName, L"tr%d",   e->devCounters[1]++);
            break;
            case MIB_IF_TYPE_FDDI:
                swprintf(e->pTable->table[i].wszName, L"fddi%d", e->devCounters[2]++);
            break;
            case MIB_IF_TYPE_LOOPBACK:
                /* There should only be only IPv4 loopback address */
                if (e->devCounters[3]++ == 0)
                    lstrcpyW(e->pTable->table[i].wszName, L"lo");
                else
                    continue;
                break;
            case MIB_IF_TYPE_PPP:
                swprintf(e->pTable->table[i].wszName, L"ppp%d",  e->devCounters[3]++);
            break;
            case MIB_IF_TYPE_SLIP:
                swprintf(e->pTable->table[i].wszName, L"sl%d",   e->devCounters[4]++);
            break;
            default:
                swprintf(e->pTable->table[i].wszName, L"net%d",  e->devCounters[5]++);
            break;
        }
    }
    return rc;
}

static int if_name(net_adapter_enum_t *e, PIP_ADAPTER_ADDRESSES a, LPWSTR dst)
{
    DWORD i;

    for (i = 0; i < e->pTable->dwNumEntries; i++) {
        if (e->pTable->table[i].dwIndex == a->IfIndex) {
            lstrcpyW(dst, e->pTable->table[i].wszName);
            return 1;
        }
    }
    return 0;
}

static jlong xp_enum0(SIGHT_STDARGS, jlong pool)
{
    DWORD rc;
    net_adapter_enum_t *e;
    ULONG outBufLen = 0;

    UNREFERENCED_O;
    if (!(e = (net_adapter_enum_t *)calloc(1, sizeof(net_adapter_enum_t)))) {
        throwAprMemoryException(_E, THROW_FMARK,
                                apr_get_os_error());
        return 0;
    }
    // Make an initial call to GetAdaptersAddresses to get the
    // size needed into the outBufLen variable
    rc = GetAdaptersAddresses(AF_UNSPEC,
                              0,
                              NULL,
                              NULL,
                              &outBufLen);
    if (rc != ERROR_BUFFER_OVERFLOW) {
        throwAprException(_E, APR_FROM_OS_ERROR(rc));
        goto cleanup;
    }
    e->pAddresses = (PIP_ADAPTER_ADDRESSES)malloc(outBufLen);
    if (!e->pAddresses) {
        throwAprMemoryException(_E, THROW_FMARK,
                                apr_get_os_error());
        goto cleanup;
    }
    // Make a second call to GetAdapters Addresses to get the
    // actual data we want
    rc = GetAdaptersAddresses(AF_UNSPEC,
                              0,
                              NULL,
                              e->pAddresses,
                              &outBufLen);
    if (rc != ERROR_SUCCESS) {
        throwAprException(_E, APR_FROM_OS_ERROR(rc));
        goto cleanup;
    }
    if (if_enum(_E, _O, e) != ERROR_SUCCESS)
        goto cleanup;
    e->pCurrent = e->pAddresses;
    return P2J(e);
cleanup:
    if (e) {
        SIGHT_FREE(e->pAddresses);
        SIGHT_FREE(e->pTable);
        free(e);
    }
    return 0;
}

static jlong si_enum0(SIGHT_STDARGS, jlong pool)
{
    DWORD rc;
    net_adapter_enum_t *e;
    ULONG outBufLen = 0;

    UNREFERENCED_O;
    if (!(e = (net_adapter_enum_t *)sight_calloc(_E, sizeof(net_adapter_enum_t),
                                                 THROW_FMARK))) {
        return 0;
    }

    // Make an initial call to GetAdaptersAddresses to get the
    // size needed into the outBufLen variable
    rc = GetAdaptersInfo(NULL, &outBufLen);
    if (rc != ERROR_BUFFER_OVERFLOW) {
        throwAprException(_E, APR_FROM_OS_ERROR(rc));
        goto cleanup;
    }
    if (!(e->pInfo = (PIP_ADAPTER_INFO)sight_malloc(_E, outBufLen, THROW_FMARK))) {
        goto cleanup;
    }
    // Make a second call to GetAdapters Addresses to get the
    // actual data we want
    rc = GetAdaptersInfo(e->pInfo, &outBufLen);
    if (rc != ERROR_SUCCESS) {
        throwAprException(_E, APR_FROM_OS_ERROR(rc));
        goto cleanup;
    }
    if (if_enum(_E, _O, e) != ERROR_SUCCESS)
        goto cleanup;

    e->pCurInfo = e->pInfo;
    e->isInfo   = 1;
    return P2J(e);
cleanup:
    if (e) {
        SIGHT_FREE(e->pInfo);
        SIGHT_FREE(e->pTable);
        free(e);
    }
    return 0;
}

#define TCPIPPI "SYSTEM\\CurrentControlSet\\Services\\Tcpip\\Parameters\\Interfaces\\"

static char *get_tcpip_param(const char *iface, const char *param)
{
    char   subkey[MAX_PATH];
    HKEY   hKey;
    DWORD  dwType;
    DWORD  dwSize;
    LPBYTE value;

    if (*iface != '{')
        return NULL;
    strcpy(subkey, TCPIPPI);
    strcat(subkey, iface);
    if (RegOpenKeyEx(HKEY_LOCAL_MACHINE,
                     subkey, 0, KEY_QUERY_VALUE,
                     &hKey) != ERROR_SUCCESS)
        return NULL;
    if (RegQueryValueEx(hKey, param, NULL,
                        &dwType, NULL, &dwSize) != ERROR_SUCCESS)
        goto cleanup;
    if (dwType != REG_SZ && dwType != REG_MULTI_SZ)
        goto cleanup;
    if (!(value = malloc(dwSize)))
        goto cleanup;
    if (RegQueryValueEx(hKey, param, NULL,
                        &dwType, value, &dwSize) != ERROR_SUCCESS) {
        free(value);
        value = NULL;
    }
cleanup:
    RegCloseKey(hKey);
    return value;
}

static char *hex_base = "0123456789abcdefx";

static void xp_enum2(SIGHT_STDARGS, jobject thiz, jlong handle)
{
    net_adapter_enum_t *e = J2P(handle, net_adapter_enum_t *);
    PIP_ADAPTER_UNICAST_ADDRESS     ua;
    PIP_ADAPTER_MULTICAST_ADDRESS   ma;
    PIP_ADAPTER_DNS_SERVER_ADDRESS  da;
    jsize len, idx, i;
    char buf[MAX_PATH] = {0};
    WCHAR nname[32];
    char *sp;
    DWORD blen;
    UNREFERENCED_O;
    if (!e || !e->pCurrent)
        return;

    if (if_name(e, e->pCurrent, nname))
        SET_IFIELD_W(0000, thiz, nname);
    else {
        if (e->pCurrent->IfType == IF_TYPE_TUNNEL) {
            sprintf(buf, "tun%d", e->devCounters[6]++);
            SET_IFIELD_S(0000, thiz, buf);
        }
        else if (!e->pCurrent->IfIndex) {
            sprintf(buf, "net%d", e->devCounters[5]++);
            SET_IFIELD_S(0000, thiz, buf);
        }
        else
            SET_IFIELD_S(0000, thiz, e->pCurrent->AdapterName);
    }
    SET_IFIELD_W(0001, thiz, e->pCurrent->Description);
    SET_IFIELD_W(0002, thiz, e->pCurrent->FriendlyName);
    CALL_METHOD1(0000, thiz, e->pCurrent->IfType);
    CALL_METHOD1(0001, thiz, e->pCurrent->OperStatus);
    if (e->pCurrent->PhysicalAddressLength) {
        jobject addr;
        len = (jsize)e->pCurrent->PhysicalAddressLength;
        sp = &buf[0];
        for (i = 0; i < len - 1; i++) {
            *sp++ = hex_base[e->pCurrent->PhysicalAddress[i] >> 4];
            *sp++ = hex_base[e->pCurrent->PhysicalAddress[i] & 0x0f];
            *sp++ = ':';
        }
        *sp++ = hex_base[e->pCurrent->PhysicalAddress[i] >> 4];
        *sp++ = hex_base[e->pCurrent->PhysicalAddress[i] & 0x0f];
        *sp++ = '\0';
        addr = sight_new_netaddr_class(_E, _O);
        if (!addr || (*_E)->ExceptionCheck(_E))
            return;
        sight_netaddr_set_addr(_E, addr, buf);
        sight_netaddr_set_family(_E, addr, AF_HARDWARE);
        SET_IFIELD_O(0003, thiz, addr);
        (*_E)->DeleteLocalRef(_E, addr);
    }
    if (e->pCurrent->Flags & IP_ADAPTER_DHCP_ENABLED) {
        jobject addr;
        SET_IFIELD_Z(0004, thiz, JNI_TRUE);
        addr = sight_new_netaddr_class(_E, _O);
        if (!addr || (*_E)->ExceptionCheck(_E))
            return;
        if ((sp = get_tcpip_param(e->pCurrent->AdapterName,
                                  "DhcpServer"))) {
            sight_netaddr_set_addr(_E, addr, sp);
            sight_netaddr_set_family(_E, addr, AF_INET);
            free(sp);
        }
        SET_IFIELD_O(0005, thiz, addr);
        (*_E)->DeleteLocalRef(_E, addr);

    }
    ua = e->pCurrent->FirstUnicastAddress;
    len = 0;
    while (ua) {
        len++;
        ua = ua->Next;
    }
    if (len) {
        jobject addr;
        jobjectArray aaddr = sight_new_netaddr_array(_E, _O, len);
        if (!aaddr || (*_E)->ExceptionCheck(_E))
            return;
        ua = e->pCurrent->FirstUnicastAddress;
        idx = 0;
        while (ua) {
            blen = MAX_PATH;
            if (WSAAddressToStringA(ua->Address.lpSockaddr,
                                    ua->Address.iSockaddrLength,
                                    NULL,
                                    buf,
                                    &blen)) {
                DWORD rc = WSAGetLastError();
                throwAprException(_E, APR_FROM_OS_ERROR(rc));
                return;
            }
            addr = sight_new_netaddr_class(_E, _O);
            if (!addr || (*_E)->ExceptionCheck(_E))
                return;
            sight_netaddr_set_addr(_E, addr, buf);
            sight_netaddr_set_family(_E, addr,
                                     ua->Address.lpSockaddr->sa_family);
            if (ua->Address.lpSockaddr->sa_family != AF_INET6) {
                if (e->pCurrent->IfType == IF_TYPE_SOFTWARE_LOOPBACK)
                    sight_netaddr_set_mask(_E, addr, "255.0.0.0");
                else if (e->pCurrent->Flags & IP_ADAPTER_DHCP_ENABLED) {
                    if ((sp = get_tcpip_param(e->pCurrent->AdapterName,
                                              "DhcpSubnetMask"))) {
                        sight_netaddr_set_mask(_E, addr, sp);
                        free(sp);
                    }
                }
                else {
                    if ((sp = get_tcpip_param(e->pCurrent->AdapterName,
                                              "SubnetMask"))) {
                        sight_netaddr_set_mask(_E, addr, sp);
                        free(sp);
                    }
                }
            }
            if (ua->LeaseLifetime != ULONG_MAX)
                sight_netaddr_set_llt(_E, addr, ua->LeaseLifetime);
            (*_E)->SetObjectArrayElement(_E, aaddr, idx++, addr);
            (*_E)->DeleteLocalRef(_E, addr);
            ua = ua->Next;
        }
        SET_IFIELD_O(0006, thiz, aaddr);
        (*_E)->DeleteLocalRef(_E, aaddr);
    }
    ma = e->pCurrent->FirstMulticastAddress;
    len = 0;
    while (ma) {
        len++;
        ma = ma->Next;
    }
    if (len) {
        jobject addr;
        jobjectArray aaddr = sight_new_netaddr_array(_E, _O, len);
        if (!aaddr || (*_E)->ExceptionCheck(_E))
            return;
        ma = e->pCurrent->FirstMulticastAddress;
        idx = 0;
        while (ma) {
            blen = MAX_PATH;
            if (WSAAddressToStringA(ma->Address.lpSockaddr,
                                    ma->Address.iSockaddrLength,
                                    NULL,
                                    buf,
                                    &blen)) {
                DWORD rc = WSAGetLastError();
                throwAprException(_E, APR_FROM_OS_ERROR(rc));
                return;
            }
            addr = sight_new_netaddr_class(_E, _O);
            if (!addr || (*_E)->ExceptionCheck(_E))
                return;
            sight_netaddr_set_addr(_E, addr, buf);
            sight_netaddr_set_family(_E, addr,
                                     ma->Address.lpSockaddr->sa_family);

            (*_E)->SetObjectArrayElement(_E, aaddr, idx++, addr);
            (*_E)->DeleteLocalRef(_E, addr);
            ma = ma->Next;
        }
        SET_IFIELD_O(0007, thiz, aaddr);
        (*_E)->DeleteLocalRef(_E, aaddr);
    }

    da = e->pCurrent->FirstDnsServerAddress;
    len = 0;
    while (da) {
        len++;
        da = da->Next;
    }
    if (len) {
        jobject addr;
        jobjectArray aaddr = sight_new_netaddr_array(_E, _O, len);
        if (!aaddr || (*_E)->ExceptionCheck(_E))
            return;
        da = e->pCurrent->FirstDnsServerAddress;
        idx = 0;
        while (da) {
            blen = MAX_PATH;
            if (WSAAddressToStringA(da->Address.lpSockaddr,
                                    da->Address.iSockaddrLength,
                                    NULL,
                                    buf,
                                    &blen)) {
                DWORD rc = WSAGetLastError();
                throwAprException(_E, APR_FROM_OS_ERROR(rc));
                return;
            }
            addr = sight_new_netaddr_class(_E, _O);
            if (!addr || (*_E)->ExceptionCheck(_E))
                return;
            sight_netaddr_set_addr(_E, addr, buf);
            sight_netaddr_set_family(_E, addr,
                                     da->Address.lpSockaddr->sa_family);
            (*_E)->SetObjectArrayElement(_E, aaddr, idx++, addr);
            (*_E)->DeleteLocalRef(_E, addr);
            da = da->Next;
        }
        SET_IFIELD_O(0008, thiz, aaddr);
        (*_E)->DeleteLocalRef(_E, aaddr);
    }
    SET_IFIELD_I(0009, thiz, e->pCurrent->Mtu);


    e->pCurrent = e->pCurrent->Next;
}

static void si_enum2(SIGHT_STDARGS, jobject thiz, jlong handle)
{
    net_adapter_enum_t *e = J2P(handle, net_adapter_enum_t *);
    PIP_ADDR_STRING ua;
    PIP_ADDR_STRING da;
    char buf[MAX_PATH];
    WCHAR nname[32];
    char *sp;
    jsize len, idx, i;
    UNREFERENCED_O;
    if (!e || !e->pCurInfo)
        return;

    if (if_name(e, e->pCurrent, nname))
        SET_IFIELD_W(0000, thiz, nname);
    else
        SET_IFIELD_S(0000, thiz, e->pCurrent->AdapterName);
    SET_IFIELD_S(0001, thiz, e->pCurInfo->Description);
    SET_IFIELD_S(0002, thiz, e->pCurInfo->AdapterName);
    CALL_METHOD1(0000, thiz, e->pCurInfo->Type);
    CALL_METHOD1(0001, thiz, 1);

    if (e->pCurInfo->AddressLength) {
        jobject addr;
        len = (jsize)e->pCurInfo->AddressLength;
        sp = &buf[0];
        for (i = 0; i < len - 1; i++) {
            *sp++ = hex_base[e->pCurInfo->Address[i] >> 4];
            *sp++ = hex_base[e->pCurInfo->Address[i] & 0x0f];
            *sp++ = ':';
        }
        *sp++ = hex_base[e->pCurInfo->Address[i] >> 4];
        *sp++ = hex_base[e->pCurInfo->Address[i] & 0x0f];
        *sp++ = '\0';
        addr = sight_new_netaddr_class(_E, _O);
        if (!addr || (*_E)->ExceptionCheck(_E))
            return;
        sight_netaddr_set_addr(_E, addr, buf);
        sight_netaddr_set_family(_E, addr, AF_HARDWARE);
        SET_IFIELD_O(0003, thiz, addr);
        (*_E)->DeleteLocalRef(_E, addr);
    }
    if (e->pCurInfo->DhcpEnabled) {
        jobject addr;
        SET_IFIELD_Z(0004, thiz, JNI_TRUE);
        addr = sight_new_netaddr_class(_E, _O);
        if (!addr || (*_E)->ExceptionCheck(_E))
            return;
        sight_netaddr_set_addr(_E, addr, e->pCurInfo->DhcpServer.IpAddress.String);
        sight_netaddr_set_family(_E, addr, AF_INET);
        SET_IFIELD_O(0005, thiz, addr);
        (*_E)->DeleteLocalRef(_E, addr);
    }

    ua = &e->pCurInfo->IpAddressList;
    len = 0;
    while (ua) {
        if (*ua->IpAddress.String)
            len++;
        ua = ua->Next;
    }
    if (len) {
        jobject addr;
        jobjectArray aaddr = sight_new_netaddr_array(_E, _O, len);
        if (!aaddr || (*_E)->ExceptionCheck(_E))
            return;
        ua = &e->pCurInfo->IpAddressList;
        idx = 0;
        while (ua) {
            addr = sight_new_netaddr_class(_E, _O);
            if (!addr || (*_E)->ExceptionCheck(_E))
                return;
            sight_netaddr_set_addr(_E, addr, ua->IpAddress.String);
            sight_netaddr_set_family(_E, addr, AF_INET);

            if (*ua->IpMask.String)
                sight_netaddr_set_mask(_E, addr, ua->IpMask.String);

            if (e->pCurInfo->DhcpEnabled) {
                jlong dt = (jlong)difftime(e->pCurInfo->LeaseExpires,
                                           time(NULL));
                sight_netaddr_set_llt(_E, addr, dt);
            }
            (*_E)->SetObjectArrayElement(_E, aaddr, idx++, addr);
            (*_E)->DeleteLocalRef(_E, addr);
            ua = ua->Next;
        }
        SET_IFIELD_O(0006, thiz, aaddr);
        (*_E)->DeleteLocalRef(_E, aaddr);
    }

    da = &e->pCurInfo->GatewayList;
    len = 0;
    while (da) {
        if (*da->IpAddress.String)
            len++;
        da = da->Next;
    }
    if (len) {
        jobject addr;
        jobjectArray aaddr = sight_new_netaddr_array(_E, _O, len);
        if (!aaddr || (*_E)->ExceptionCheck(_E))
            return;
        da = &e->pCurInfo->GatewayList;
        idx = 0;
        while (da) {
            addr = sight_new_netaddr_class(_E, _O);
            if (!addr || (*_E)->ExceptionCheck(_E))
                return;
            sight_netaddr_set_addr(_E, addr, da->IpAddress.String);
            if (*da->IpMask.String)
                sight_netaddr_set_mask(_E, addr, da->IpMask.String);
            sight_netaddr_set_family(_E, addr, AF_INET);

            (*_E)->SetObjectArrayElement(_E, aaddr, idx++, addr);
            (*_E)->DeleteLocalRef(_E, addr);
            da = da->Next;
        }
        SET_IFIELD_O(0008, thiz, aaddr);
        (*_E)->DeleteLocalRef(_E, aaddr);
    }

    e->pCurInfo = e->pCurInfo->Next;
}

SIGHT_EXPORT_DECLARE(jlong, NetworkAdapter, enum0)(SIGHT_STDARGS,
                                                   jlong pool)
{
    if (sight_osver->dwMajorVersion == 5 &&
        sight_osver->dwMinorVersion == 0)
        return si_enum0(_E, _O, pool);
    else
        return xp_enum0(_E, _O, pool);

}

SIGHT_EXPORT_DECLARE(jint, NetworkAdapter, enum1)(SIGHT_STDARGS,
                                                  jlong handle)
{
    jint cnt = 0;
    net_adapter_enum_t *e = J2P(handle, net_adapter_enum_t *);

    UNREFERENCED_O;
    if (!e)
        return 0;
    if (e->isInfo) {
        PIP_ADAPTER_INFO i = e->pInfo;
        while(i) {
            cnt++;
            i = i->Next;
        }
    }
    else {
        PIP_ADAPTER_ADDRESSES p = e->pAddresses;
        while(p) {
            cnt++;
            p = p->Next;
        }
    }
    return cnt;
}

SIGHT_EXPORT_DECLARE(void, NetworkAdapter, enum2)(SIGHT_STDARGS,
                                                  jobject thiz,
                                                  jlong handle)
{
    net_adapter_enum_t *e = J2P(handle, net_adapter_enum_t *);
    if (!e)
        return;
    if (!e->isInfo)
        xp_enum2(_E, _O, thiz, handle);
    else
        si_enum2(_E, _O, thiz, handle);
}

SIGHT_EXPORT_DECLARE(void, NetworkAdapter, enum3)(SIGHT_STDARGS,
                                                  jlong handle)
{
    net_adapter_enum_t *e = J2P(handle, net_adapter_enum_t *);

    UNREFERENCED_STDARGS;
    if (e) {
        SIGHT_FREE(e->pAddresses);
        SIGHT_FREE(e->pInfo);
        SIGHT_FREE(e->pTable);
        free(e);
    }
}
