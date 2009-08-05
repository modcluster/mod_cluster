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
 * UDP statistics implementation
 */

J_DECLARE_CLAZZ = {
    NULL,
    NULL,
    SIGHT_CLASS_PATH "UdpStatistics"
};

J_DECLARE_F_ID(0000) = {
    NULL,
    "InDatagrams",
    "I"
};

J_DECLARE_F_ID(0001) = {
    NULL,
    "NoPorts",
    "I"
};

J_DECLARE_F_ID(0002) = {
    NULL,
    "InErrors",
    "I"
};

J_DECLARE_F_ID(0003) = {
    NULL,
    "OutDatagrams",
    "I"
};

J_DECLARE_F_ID(0004) = {
    NULL,
    "NumAddrs",
    "I"
};


SIGHT_CLASS_LDEF(UdpStatistics)
{
    if (sight_load_class(_E, &_clazzn))
        return 1;
    J_LOAD_IFIELD(0000);
    J_LOAD_IFIELD(0001);
    J_LOAD_IFIELD(0002);
    J_LOAD_IFIELD(0003);
    J_LOAD_IFIELD(0004);

    return 0;
}

SIGHT_CLASS_UDEF(UdpStatistics)
{
    sight_unload_class(_E, &_clazzn);
}

static const char *eiftype = "Unsupported NetworkAddressFamily type";

/* Initialize volume enumeration */
SIGHT_EXPORT_DECLARE(void, UdpStatistics, info0)(SIGHT_STDARGS,
                                                 jobject thiz,
                                                 jint iftype,
                                                 jlong pool)
{
    MIB_UDPSTATS s;
    DWORD rc = APR_EINVAL;

    UNREFERENCED_O;
    if (sight_osver->dwMajorVersion == 5 &&
        sight_osver->dwMinorVersion == 0) {
        if (iftype == 1)
            rc = GetUdpStatistics(&s);
        else {
            throwOSException(_E, eiftype);
            return;
        }

    }
    else {
        if (iftype == 1)
            rc = GetUdpStatisticsEx(&s, AF_INET);
        else if (iftype == 2)
            rc = GetUdpStatisticsEx(&s, AF_INET6);
        else {
            throwOSException(_E, eiftype);
            return;
        }
    }
    if (rc != NO_ERROR) {
        throwAprException(_E, APR_FROM_OS_ERROR(rc));
        return;
    }

    SET_IFIELD_I(0000, thiz, s.dwInDatagrams);
    SET_IFIELD_I(0001, thiz, s.dwNoPorts);
    SET_IFIELD_I(0002, thiz, s.dwInErrors);
    SET_IFIELD_I(0003, thiz, s.dwOutDatagrams);
    SET_IFIELD_I(0004, thiz, s.dwNumAddrs);

}

typedef struct udpconn_enum_t {
    int                   type;
    union {
        void                       *p;
        PMIB_UDPTABLE               p0;
        PMIB_UDPTABLE_OWNER_MODULE  p4;
        PMIB_UDP6TABLE_OWNER_MODULE p6;
    };
} udpconn_enum_t;

/* Initialize UDP conn enumeration */
SIGHT_EXPORT_DECLARE(jlong, UdpStatistics, enum0)(SIGHT_STDARGS,
                                                  jint iftype,
                                                  jlong pool)
{
    udpconn_enum_t *e;
    int type = iftype;
    DWORD rc;

    UNREFERENCED_O;

    if (iftype < 1 || iftype > 2) {
        throwOSException(_E, eiftype);
         return 0;
    }
    if (sight_osver->dwMajorVersion == 5 &&
        sight_osver->dwMinorVersion == 0) {
        if (iftype != 1) {
            throwOSException(_E, eiftype);
            return 0;
        }
        type = 0;
    }

    if (!(e = (udpconn_enum_t *)sight_calloc(_E, sizeof(udpconn_enum_t),
                                             THROW_FMARK))) {
        return 0;
    }
    e->type = type;

    if (type == 0) {
        DWORD dwSize = 0;

        if (!(e->p = sight_calloc(_E, sizeof(MIB_UDPTABLE),
                                  THROW_FMARK)))
            goto cleanup;
        rc = GetUdpTable(e->p, &dwSize, TRUE);
        if (rc == ERROR_INSUFFICIENT_BUFFER) {
            free(e->p);
            if (!(e->p = sight_calloc(_E, dwSize, THROW_FMARK)))
                goto cleanup;
        }
        else if (rc != NO_ERROR) {
            throwAprException(_E, APR_FROM_OS_ERROR(rc));
            goto cleanup;
        }
        /* Do a real table query */
        rc = GetUdpTable(e->p, &dwSize, TRUE);
        if (rc != NO_ERROR) {
            throwAprException(_E, APR_FROM_OS_ERROR(rc));
            goto cleanup;
        }
    }
    else if (type == 1) {
        DWORD dwSize = 0;
        if (!(e->p = sight_calloc(_E, sizeof(MIB_UDPTABLE_OWNER_MODULE),
                                  THROW_FMARK)))
            goto cleanup;
        rc = GetExtendedUdpTable(e->p, &dwSize, TRUE, AF_INET,
                                 UDP_TABLE_OWNER_MODULE, 0);
        if (rc == ERROR_INSUFFICIENT_BUFFER) {
            free(e->p);
            if (!(e->p = sight_calloc(_E, dwSize, THROW_FMARK)))
                goto cleanup;
        }
        else if (rc != NO_ERROR) {
            throwAprException(_E, APR_FROM_OS_ERROR(rc));
            goto cleanup;
        }
        /* Do a real table query */
        rc = GetExtendedUdpTable(e->p, &dwSize, TRUE, AF_INET,
                                 UDP_TABLE_OWNER_MODULE, 0);
        if (rc != NO_ERROR) {
            throwAprException(_E, APR_FROM_OS_ERROR(rc));
            goto cleanup;
        }
    }
    else if (type == 2) {
        DWORD dwSize = 0;
        if (!(e->p = sight_calloc(_E, sizeof(MIB_UDP6TABLE_OWNER_MODULE),
                                  THROW_FMARK)))
            goto cleanup;

        rc = GetExtendedUdpTable(e->p, &dwSize, TRUE, AF_INET6,
                                 UDP_TABLE_OWNER_MODULE, 0);
        dwSize = dwSize << 2;
        if (rc == ERROR_INSUFFICIENT_BUFFER) {
            free(e->p);
            if (!(e->p = sight_calloc(_E, dwSize, THROW_FMARK)))
                goto cleanup;
        }
        else if (rc != NO_ERROR) {
            throwAprException(_E, APR_FROM_OS_ERROR(rc));
            goto cleanup;
        }
        /* Do a real table query */
        rc = GetExtendedUdpTable(e->p, &dwSize, TRUE, AF_INET6,
                                 UDP_TABLE_OWNER_MODULE, 0);
        if (rc != NO_ERROR) {
            throwAprException(_E, APR_FROM_OS_ERROR(rc));
            goto cleanup;
        }
    }

    return P2J(e);
cleanup:
    if (e) {
        if (e->p)
            free(e->p);
        free(e);
    }
    return 0;
}

/* Get the number of entries */
SIGHT_EXPORT_DECLARE(jint, UdpStatistics, enum1)(SIGHT_STDARGS,
                                                 jlong handle)
{
    udpconn_enum_t *e = J2P(handle, udpconn_enum_t *);

    UNREFERENCED_STDARGS;
    if (e) {
        switch (e->type) {
            case 0: return e->p0->dwNumEntries; break;
            case 1: return e->p4->dwNumEntries; break;
            case 2: return e->p6->dwNumEntries; break;
        }
    }
    return 0;
}

SIGHT_EXPORT_DECLARE(void, UdpStatistics, enum2)(SIGHT_STDARGS,
                                                 jobject conn,
                                                 jint index,
                                                 jlong handle)
{
    jint pid   = -1;
    jint state = 0;
    jint lp = 0;
    jobject la;
    char las[96];
    char ssi[32];
    struct  in_addr ial;
    jlong cts = 0;
    udpconn_enum_t *e = J2P(handle, udpconn_enum_t *);

    if (!e || !e->p)
        return;

    if (e->type == 0) {
        ial.s_addr = e->p0->table[index].dwLocalAddr;
        strcpy(las, inet_ntoa(ial));
        lp    = ntohs((unsigned short)(0x0000FFFF & e->p0->table[index].dwLocalPort));
    }
    else if (e->type == 1) {
        ial.s_addr = e->p4->table[index].dwLocalAddr;
        strcpy(las, inet_ntoa(ial));
        lp    = ntohs((unsigned short)(0x0000FFFF & e->p4->table[index].dwLocalPort));
        pid   = e->p4->table[index].dwOwningPid;
        cts   = litime_to_ms(&(e->p4->table[index].liCreateTimestamp));
    }
    else if (e->type == 2) {
        sight_inet_ntop6(e->p6->table[index].ucLocalAddr, las, 64);
        if (e->p6->table[index].dwLocalScopeId) {
            sprintf(ssi, "%%%d", e->p6->table[index].dwLocalScopeId);
            strcat(las, ssi);
        }
        lp    = ntohs((unsigned short)(0x0000FFFF & e->p6->table[index].dwLocalPort));
        pid   = e->p6->table[index].dwOwningPid;
        cts   = litime_to_ms(&(e->p6->table[index].liCreateTimestamp));
    }
    sight_udpconn_set_pid(_E, conn, pid);
    sight_udpconn_set_cts(_E, conn, cts);
    sight_udpconn_set_state(_E, conn, 0);

    la = sight_new_netaddr_class(_E, _O);
    sight_netaddr_set_addr(_E, la, las);
    sight_netaddr_set_port(_E, la, lp);
    sight_udpconn_set_local(_E, conn, la);
    (*_E)->DeleteLocalRef(_E, la);

}

/* Close TCP conn enumeration */
SIGHT_EXPORT_DECLARE(void, UdpStatistics, enum3)(SIGHT_STDARGS,
                                                 jlong handle)
{
    udpconn_enum_t *e = J2P(handle, udpconn_enum_t *);

    UNREFERENCED_STDARGS;
    if (e) {
        if (e->p)
            free(e->p);
        free(e);
    }
}
