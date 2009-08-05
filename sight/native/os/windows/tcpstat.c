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
 * TCP statistics implementation
 */

J_DECLARE_CLAZZ = {
    NULL,
    NULL,
    SIGHT_CLASS_PATH "TcpStatistics"
};

J_DECLARE_F_ID(0000) = {
    NULL,
    "RtoMin",
    "I"
};

J_DECLARE_F_ID(0001) = {
    NULL,
    "RtoMax",
    "I"
};

J_DECLARE_F_ID(0002) = {
    NULL,
    "MaxConn",
    "I"
};

J_DECLARE_F_ID(0003) = {
    NULL,
    "ActiveOpens",
    "I"
};

J_DECLARE_F_ID(0004) = {
    NULL,
    "PassiveOpens",
    "I"
};

J_DECLARE_F_ID(0005) = {
    NULL,
    "AttemptFails",
    "I"
};

J_DECLARE_F_ID(0006) = {
    NULL,
    "EstabResets",
    "I"
};

J_DECLARE_F_ID(0007) = {
    NULL,
    "CurrEstab",
    "I"
};

J_DECLARE_F_ID(0008) = {
    NULL,
    "InSegs",
    "I"
};

J_DECLARE_F_ID(0009) = {
    NULL,
    "OutSegs",
    "I"
};

J_DECLARE_F_ID(0010) = {
    NULL,
    "RetransSegs",
    "I"
};

J_DECLARE_F_ID(0011) = {
    NULL,
    "InErrs",
    "I"
};

J_DECLARE_F_ID(0012) = {
    NULL,
    "OutRsts",
    "I"
};

J_DECLARE_F_ID(0013) = {
    NULL,
    "NumConns",
    "I"
};

SIGHT_CLASS_LDEF(TcpStatistics)
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
    J_LOAD_IFIELD(0010);
    J_LOAD_IFIELD(0011);
    J_LOAD_IFIELD(0012);
    J_LOAD_IFIELD(0013);

    return 0;
}

SIGHT_CLASS_UDEF(TcpStatistics)
{
    sight_unload_class(_E, &_clazzn);
}

static const char *eiftype = "Unsupported NetworkAddressFamily type";

/* Initialize volume enumeration */
SIGHT_EXPORT_DECLARE(void, TcpStatistics, info0)(SIGHT_STDARGS,
                                                 jobject thiz,
                                                 jint iftype,
                                                 jlong pool)
{
    MIB_TCPSTATS s;
    DWORD rc = APR_EINVAL;

    UNREFERENCED_O;
    if (sight_osver->dwMajorVersion == 5 &&
        sight_osver->dwMinorVersion == 0) {
        if (iftype == 1)
            rc = GetTcpStatistics(&s);
        else {
            throwOSException(_E, eiftype);
            return;
        }

    }
    else {
        if (iftype == 1)
            rc = GetTcpStatisticsEx(&s, AF_INET);
        else if (iftype == 2)
            rc = GetTcpStatisticsEx(&s, AF_INET6);
        else {
            throwOSException(_E, eiftype);
            return;
        }
    }
    if (rc != NO_ERROR) {
        throwAprException(_E, APR_FROM_OS_ERROR(rc));
        return;
    }

    SET_IFIELD_I(0000, thiz, s.dwRtoMin);
    SET_IFIELD_I(0001, thiz, s.dwRtoMax);
    SET_IFIELD_I(0002, thiz, s.dwMaxConn);
    SET_IFIELD_I(0003, thiz, s.dwActiveOpens);
    SET_IFIELD_I(0004, thiz, s.dwPassiveOpens);
    SET_IFIELD_I(0005, thiz, s.dwAttemptFails);
    SET_IFIELD_I(0006, thiz, s.dwEstabResets);
    SET_IFIELD_I(0007, thiz, s.dwCurrEstab);
    SET_IFIELD_I(0008, thiz, s.dwInSegs);
    SET_IFIELD_I(0009, thiz, s.dwOutSegs);
    SET_IFIELD_I(0010, thiz, s.dwRetransSegs);
    SET_IFIELD_I(0011, thiz, s.dwInErrs);
    SET_IFIELD_I(0012, thiz, s.dwOutRsts);
    SET_IFIELD_I(0013, thiz, s.dwNumConns);

}

typedef struct tcpconn_enum_t {
    int                   type;
    union {
        void                       *p;
        PMIB_TCPTABLE               p0;
        PMIB_TCPTABLE_OWNER_MODULE  p4;
        PMIB_TCP6TABLE_OWNER_MODULE p6;
    };

} tcpconn_enum_t;

/* Initialize TCP conn enumeration */
SIGHT_EXPORT_DECLARE(jlong, TcpStatistics, enum0)(SIGHT_STDARGS,
                                                  jint iftype,
                                                  jlong pool)
{
    tcpconn_enum_t *e;
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


    if (!(e = (tcpconn_enum_t *)sight_calloc(_E, sizeof(tcpconn_enum_t),
                                             THROW_FMARK))) {
        return 0;
    }
    e->type = type;

    if (type == 0) {
        DWORD dwSize = 0;
        if (!(e->p = sight_calloc(_E, sizeof(MIB_TCPTABLE), THROW_FMARK)))
            goto cleanup;
        rc = GetTcpTable(e->p, &dwSize, TRUE);
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
        rc = GetTcpTable(e->p, &dwSize, TRUE);
        if (rc != NO_ERROR) {
            throwAprException(_E, APR_FROM_OS_ERROR(rc));
            goto cleanup;
        }
    }
    else if (type == 1) {
        DWORD dwSize = 0;
        if (!(e->p = sight_calloc(_E, sizeof(MIB_TCPTABLE_OWNER_MODULE),
                                  THROW_FMARK)))
            goto cleanup;
        rc = GetExtendedTcpTable(e->p, &dwSize, TRUE, AF_INET,
                                 TCP_TABLE_OWNER_MODULE_ALL, 0);
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
        rc = GetExtendedTcpTable(e->p, &dwSize, TRUE, AF_INET,
                                 TCP_TABLE_OWNER_MODULE_ALL, 0);
        if (rc != NO_ERROR) {
            throwAprException(_E, APR_FROM_OS_ERROR(rc));
            goto cleanup;
        }
    }
    else if (type == 2) {
        DWORD dwSize = 0;
        if (!(e->p = sight_calloc(_E, sizeof(MIB_TCP6TABLE_OWNER_MODULE),
                                  THROW_FMARK)))
            goto cleanup;
        rc = GetExtendedTcpTable(e->p, &dwSize, TRUE, AF_INET6,
                                 TCP_TABLE_OWNER_MODULE_ALL, 0);
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
        rc = GetExtendedTcpTable(e->p, &dwSize, TRUE, AF_INET6,
                                 TCP_TABLE_OWNER_MODULE_ALL, 0);
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
SIGHT_EXPORT_DECLARE(jint, TcpStatistics, enum1)(SIGHT_STDARGS,
                                                 jlong handle)
{
    tcpconn_enum_t *e = J2P(handle, tcpconn_enum_t *);

    UNREFERENCED_STDARGS;
    if (e && e->p) {
        switch (e->type) {
            case 0: return e->p0->dwNumEntries; break;
            case 1: return e->p4->dwNumEntries; break;
            case 2: return e->p6->dwNumEntries; break;
        }
    }
    return 0;
}

SIGHT_EXPORT_DECLARE(void, TcpStatistics, enum2)(SIGHT_STDARGS,
                                                 jobject conn,
                                                 jint index,
                                                 jlong handle)
{
    jint pid   = -1;
    jint state = 0;
    jint lp = 0, rp = 0;
    jobject la, ra;
    char las[128] = "";
    char ras[128] = "";
    char ssi[32] = "";
    struct  in_addr ial, iar;
    jlong cts = 0;
    tcpconn_enum_t *e = J2P(handle, tcpconn_enum_t *);

    if (!e || !e->p)
        return;

    if (e->type == 0) {
        ial.s_addr = e->p0->table[index].dwLocalAddr;
        iar.s_addr = e->p0->table[index].dwRemoteAddr;
        strcpy(las, inet_ntoa(ial));
        strcpy(ras, inet_ntoa(iar));
        state = e->p0->table[index].dwState;
        lp    = ntohs((unsigned short)(0x0000FFFF & e->p0->table[index].dwLocalPort));
        rp    = ntohs((unsigned short)(0x0000FFFF & e->p0->table[index].dwRemotePort));
    }
    else if (e->type == 1) {
        ial.s_addr = e->p4->table[index].dwLocalAddr;
        iar.s_addr = e->p4->table[index].dwRemoteAddr;
        strcpy(las, inet_ntoa(ial));
        strcpy(ras, inet_ntoa(iar));
        lp    = ntohs((unsigned short)(0x0000FFFF & e->p4->table[index].dwLocalPort));
        rp    = ntohs((unsigned short)(0x0000FFFF & e->p4->table[index].dwRemotePort));
        state = e->p4->table[index].dwState;
        pid   = e->p4->table[index].dwOwningPid;
        cts   = litime_to_ms(&(e->p4->table[index].liCreateTimestamp));
        if (state == MIB_TCP_STATE_LISTEN && e->p4->table[index].dwRemoteAddr == 0)
            rp = 0;
    }
    else if (e->type == 2) {
        sight_inet_ntop6(e->p6->table[index].ucLocalAddr, las, 64);
        if (e->p6->table[index].dwLocalScopeId) {
            sprintf(ssi, "%%%d", e->p6->table[index].dwLocalScopeId);
            strcat(las, ssi);
        }
        sight_inet_ntop6(e->p6->table[index].ucRemoteAddr, ras, 64);
        if (e->p6->table[index].dwRemoteScopeId) {
            sprintf(ssi, "%%%d", e->p6->table[index].dwRemoteScopeId);
            strcat(ras, ssi);
        }
        lp    = ntohs((unsigned short)(0x0000FFFF & e->p6->table[index].dwLocalPort));
        rp    = ntohs((unsigned short)(0x0000FFFF & e->p6->table[index].dwRemotePort));
        state = e->p6->table[index].dwState;
        pid   = e->p6->table[index].dwOwningPid;
        cts   = litime_to_ms(&(e->p6->table[index].liCreateTimestamp));
        if (state == MIB_TCP_STATE_LISTEN)
            rp = 0;
    }
    sight_tcpconn_set_pid(_E, conn, pid);
    sight_tcpconn_set_cts(_E, conn, cts);
    sight_tcpconn_set_state(_E, conn, state);

    la = sight_new_netaddr_class(_E, _O);
    sight_netaddr_set_addr(_E, la, las);
    sight_netaddr_set_port(_E, la, lp);
    sight_tcpconn_set_local(_E, conn, la);
    (*_E)->DeleteLocalRef(_E, la);

    ra = sight_new_netaddr_class(_E, _O);
    sight_netaddr_set_addr(_E, ra, ras);
    sight_netaddr_set_port(_E, ra, rp);
    sight_tcpconn_set_remote(_E, conn, ra);
    (*_E)->DeleteLocalRef(_E, ra);

}

/* Close TCP conn enumeration */
SIGHT_EXPORT_DECLARE(void, TcpStatistics, enum3)(SIGHT_STDARGS,
                                                 jlong handle)
{
    tcpconn_enum_t *e = J2P(handle, tcpconn_enum_t *);

    UNREFERENCED_STDARGS;
    if (e) {
        if (e->p)
            free(e->p);
        free(e);
    }
}
