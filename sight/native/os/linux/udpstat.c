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

extern int sight_stat_by_inode(ino_t, uid_t, struct stat *, pid_t *);

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

#define PROC_NET_FS "/proc/net/"
static const char *eiftype = "Unsupported NetworkAddressFamily type";

/* Initialize volume enumeration */
SIGHT_EXPORT_DECLARE(void, UdpStatistics, info0)(SIGHT_STDARGS,
                                                 jobject thiz,
                                                 jint iftype,
                                                 jlong pool)
{
    int i;
    sight_arr_t *tnet;
    int InDatagrams, NoPorts, InErrors, OutDatagrams, RcvbufErrors, SndbufErrors;
    int NumCons = 0;

    UNREFERENCED_O;

    if (iftype == 1) {
        if (!(tnet = sight_arr_rload(PROC_NET_FS "sockstat"))) {
            throwAprException(_E, apr_get_os_error());
            return;
        }
        for (i = 0; i < tnet->siz; i++) {
            if (memcmp(tnet->arr[i], "UDP: inuse ", 11) == 0) {
                NumCons = atoi(tnet->arr[i] + 11);
                break;
            }
        }
    }
    else if (iftype == 2) {
        if (!(tnet = sight_arr_rload(PROC_NET_FS "sockstat6"))) {
            throwAprException(_E, apr_get_os_error());
            return;
        }
        for (i = 0; i < tnet->siz; i++) {
            if (memcmp(tnet->arr[i], "UDP6: inuse ", 12) == 0) {
                NumCons = atoi(tnet->arr[i] + 12);
                break;
            }
        }
    }
    else {
        throwOSException(_E, eiftype);
        return;
    }
    sight_arr_free(tnet);

    if (!(tnet = sight_arr_rload("/proc/net/snmp"))) {
        throwAprException(_E, apr_get_os_error());
        return;
    }

    /* Get the information corresponding to the second entry Tcp: */
    for (i = 0; i < tnet->siz; i++) {
        if (memcmp(tnet->arr[i], "Udp:", 4) == 0) {
            if (memcmp(tnet->arr[i], "Udp: InDatagrams", 16) != 0) {
                sscanf(tnet->arr[i], "Udp: %d %d %d %d %d %d",
                       &InDatagrams, &NoPorts, &InErrors, &OutDatagrams,
                       &RcvbufErrors, &SndbufErrors);
                break;
            }
        }
    }
    sight_arr_free(tnet);

    SET_IFIELD_I(0000, thiz, InDatagrams);
    SET_IFIELD_I(0001, thiz, NoPorts);
    SET_IFIELD_I(0002, thiz, InErrors);
    SET_IFIELD_I(0003, thiz, OutDatagrams);
    SET_IFIELD_I(0004, thiz, NumCons);

}

typedef struct udpconn_enum_t {
    int             type;
    int             idx;
    sight_arr_t    *tnet;
} udpconn_enum_t;


SIGHT_EXPORT_DECLARE(jlong, UdpStatistics, enum0)(SIGHT_STDARGS,
                                                  jint iftype,
                                                  jlong pool)
{
    udpconn_enum_t *e;

    UNREFERENCED_O;
    if (iftype < 1 || iftype > 2) {
        throwOSException(_E, eiftype);
        return 0;
    }
    if (!(e = (udpconn_enum_t *)calloc(1, sizeof(udpconn_enum_t)))) {
        throwAprMemoryException(_E, THROW_FMARK,
                                apr_get_os_error());
        return 0;
    }
    e->type = iftype;
    if (iftype == 1) {
        if (!(e->tnet = sight_arr_rload(PROC_NET_FS "udp"))) {
            throwAprException(_E, apr_get_os_error());
            goto cleanup;
        }
    }
    else {
        if (!(e->tnet = sight_arr_rload(PROC_NET_FS "udp6"))) {
            throwAprException(_E, apr_get_os_error());
            goto cleanup;
        }
    }

    return P2J(e);
cleanup:
    sight_arr_free(e->tnet);
    free(e);
    return 0;
}

/* Get the number of entries */
SIGHT_EXPORT_DECLARE(jint, UdpStatistics, enum1)(SIGHT_STDARGS,
                                                 jlong handle)
{
    udpconn_enum_t *e = J2P(handle, udpconn_enum_t *);

    UNREFERENCED_STDARGS;
    if (e && e->tnet) {
        e->idx = 1;
        return e->tnet->siz - 1; /* Skip description field */
    }
    else
        return 0;
}

SIGHT_EXPORT_DECLARE(void, UdpStatistics, enum2)(SIGHT_STDARGS,
                                                 jobject conn,
                                                 jint index,
                                                 jlong handle)
{
    jint pid   = -1;
    jint state = 0;
    jint lp    = 0, rp = 0;
    jobject la, ra;
    char las[128] = "";
    char ras[128] = "";
    char ssi[32]  = "";
    struct  in_addr ial, iar;
    jint tmo = 0;
    unsigned long rxq, txq, timelen, retr, inode;
    int num, d, uid, timer_run, timeout;
    char more[512];
    int laddr = 0;
    struct stat sb;

    udpconn_enum_t *e = J2P(handle, udpconn_enum_t *);

    if (!e || !e->tnet)
        return;
    if (e->idx > e->tnet->siz) {
        /* TODO: Throw overflow */
        return;
    }
    num = sscanf(e->tnet->arr[e->idx],
    "%d: %64[0-9A-Fa-f]:%X %64[0-9A-Fa-f]:%X %X %lX:%lX %X:%lX %lX %d %d %ld %512s",
            &d, las, &lp, ras, &rp, &state,
            &txq, &rxq, &timer_run, &timelen, &retr,
            &uid, &timeout, &inode, more);

    if (e->type == 1) {
        /* IPV4 entries */
        int al;
        struct in_addr in4;

        al = sscanf(las, "%X", &in4.s_addr);
        if (al == 1)
            inet_ntop(AF_INET, &in4, las, 64);
        else
            las[0] = '\0';

        al = sscanf(ras, "%X", &in4.s_addr);
        if (al == 1)
            inet_ntop(AF_INET, &in4, ras, 64);
        else
            ras[0] = '\0';
        if (in4.s_addr == 0 && rp == 0);
            laddr = 1;
    }
    else {
        /* IPV6 entries */
        int al;
        struct in6_addr in6;

        al = sscanf(las, "%08X%08X%08X%08X",
                    &in6.s6_addr32[0], &in6.s6_addr32[1],
                    &in6.s6_addr32[2], &in6.s6_addr32[3]);

        if (al == 4)
            sight_inet_ntop6(in6.s6_addr, las, 64);
        else
            las[0] = '\0';
        al = sscanf(ras, "%08X%08X%08X%08X",
                    &in6.s6_addr32[0], &in6.s6_addr32[1],
                    &in6.s6_addr32[2], &in6.s6_addr32[3]);

        if (al == 4)
            sight_inet_ntop6(in6.s6_addr, ras, 64);
        else
            ras[0] = '\0';
        if (in6.s6_addr32[0] == 0 && in6.s6_addr32[1] == 0 &&
            in6.s6_addr32[2] == 0 && in6.s6_addr32[3] == 0 &&
            rp == 0)
            laddr = 1;
    }
    /* TODO: See what's the actual value of timelen */
    tmo = timelen;
    sight_udpconn_set_tmo(_E, conn, tmo);
    if (!sight_stat_by_inode(inode, uid, &sb, &pid)) {
        apr_time_t ctime;
        apr_time_ansi_put(&ctime, sb.st_ctime);
        sight_udpconn_set_cts(_E, conn, apr_time_as_msec(ctime));
    }
    sight_udpconn_set_pid(_E, conn, pid);

    switch (state) {
        case TCP_ESTABLISHED:
            sight_tcpconn_set_state(_E, conn, 2);
        break;
        case TCP_CLOSE:
            sight_tcpconn_set_state(_E, conn, 3);
        break;
        default:
            sight_tcpconn_set_state(_E, conn, laddr);
        break;
    }

    la = sight_new_netaddr_class(_E, _O);
    sight_netaddr_set_addr(_E, la, las);
    sight_netaddr_set_port(_E, la, lp);
    sight_udpconn_set_local(_E, conn, la);
    (*_E)->DeleteLocalRef(_E, la);

    ra = sight_new_netaddr_class(_E, _O);
    sight_netaddr_set_addr(_E, ra, ras);
    sight_netaddr_set_port(_E, ra, rp);
    sight_udpconn_set_remote(_E, conn, ra);
    (*_E)->DeleteLocalRef(_E, ra);
    /* Increment the index counter */
    e->idx++;

}

/* Close UDP conn enumeration */
SIGHT_EXPORT_DECLARE(void, UdpStatistics, enum3)(SIGHT_STDARGS,
                                                 jlong handle)
{
    udpconn_enum_t *e = J2P(handle, udpconn_enum_t *);

    UNREFERENCED_STDARGS;
    if (e) {
        sight_arr_free(e->tnet);
        free(e);
    }
}
