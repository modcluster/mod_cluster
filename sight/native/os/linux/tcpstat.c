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

#define PROC_NET_FS "/proc/net/"
static const char *eiftype = "Unsupported NetworkAddressFamily type";

static int tcp2sstate[] = {
    0,
    SIGHT_TCP_ESTABLISHED,
    SIGHT_TCP_SYN_SENT,
    SIGHT_TCP_SYN_RCVD,
    SIGHT_TCP_FIN_WAIT1,
    SIGHT_TCP_FIN_WAIT2,
    SIGHT_TCP_TIME_WAIT,
    SIGHT_TCP_CLOSED,
    SIGHT_TCP_CLOSE_WAIT,
    SIGHT_TCP_LAST_ACK,
    SIGHT_TCP_LISTENING,
    SIGHT_TCP_CLOSING,
    0,
    0,
    0,
    0
};

/* Initialize volume enumeration */
SIGHT_EXPORT_DECLARE(void, TcpStatistics, info0)(SIGHT_STDARGS,
                                                 jobject thiz,
                                                 jint iftype,
                                                 jlong pool)
{
    apr_status_t rc = APR_ENOTIMPL;
    sight_arr_t *tnet;
    int i;
    int RtoAlgorithm, RtoMin, RtoMax, MaxConn, ActiveOpens, PassiveOpens;
    int AttemptFails, EstabResets, CurrEstab, InSegs, OutSegs, RetransSegs;
    int InErrs, OutRsts;
    int NumCons = 0;

    UNREFERENCED_O;
    if (iftype == 1) {
        if (!(tnet = sight_arr_rload(PROC_NET_FS "sockstat"))) {
            throwAprException(_E, apr_get_os_error());
            return;
        }
        for (i = 0; i < tnet->siz; i++) {
            if (memcmp(tnet->arr[i], "TCP: inuse ", 11) == 0) {
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
            if (memcmp(tnet->arr[i], "TCP6: inuse ", 12) == 0) {
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

    if (!(tnet = sight_arr_rload(PROC_NET_FS "snmp"))) {
        throwAprException(_E, apr_get_os_error());
        return;
    }

    /* Get the information corresponding to the second entry Tcp: */
    for (i = 0; i < tnet->siz; i++) {
        if (memcmp(tnet->arr[i], "Tcp:", 4) == 0) {
            if (memcmp(tnet->arr[i], "Tcp: RtoAlgorithm", 17) != 0) {
                sscanf(tnet->arr[i], "Tcp: %d %d %d %d %d %d %d %d %d %d %d %d %d %d",
                       &RtoAlgorithm, &RtoMin, &RtoMax, &MaxConn, &ActiveOpens,
                       &PassiveOpens, &AttemptFails, &EstabResets, &CurrEstab,
                       &InSegs, &OutSegs, &RetransSegs, &InErrs, &OutRsts);
                break;
            }
        }
    }
    sight_arr_free(tnet);

    /* Fill the info (RtoAlgorithm not used) */
    SET_IFIELD_I(0000, thiz, RtoMin);
    SET_IFIELD_I(0001, thiz, RtoMax);
    SET_IFIELD_I(0002, thiz, MaxConn);
    SET_IFIELD_I(0003, thiz, ActiveOpens);
    SET_IFIELD_I(0004, thiz, PassiveOpens);
    SET_IFIELD_I(0005, thiz, AttemptFails);
    SET_IFIELD_I(0006, thiz, EstabResets);
    SET_IFIELD_I(0007, thiz, CurrEstab);
    SET_IFIELD_I(0008, thiz, InSegs);
    SET_IFIELD_I(0009, thiz, OutSegs);
    SET_IFIELD_I(0010, thiz, RetransSegs);
    SET_IFIELD_I(0011, thiz, InErrs);
    SET_IFIELD_I(0012, thiz, OutRsts);
    SET_IFIELD_I(0013, thiz, NumCons);
}

typedef struct tcpconn_enum_t {
    int             type;
    int             idx;
    sight_arr_t    *tnet;
} tcpconn_enum_t;


/* Initialize TCP conn enumeration */
SIGHT_EXPORT_DECLARE(jlong, TcpStatistics, enum0)(SIGHT_STDARGS,
                                                  jint iftype,
                                                  jlong pool)
{
    tcpconn_enum_t *e;

    UNREFERENCED_O;
    if (iftype < 1 || iftype > 2) {
        throwOSException(_E, eiftype);
        return 0;
    }
    if (!(e = (tcpconn_enum_t *)calloc(1, sizeof(tcpconn_enum_t)))) {
        throwAprMemoryException(_E, THROW_FMARK,
                                apr_get_os_error());
        return 0;
    }
    e->type = iftype;
    if (iftype == 1) {
        if (!(e->tnet = sight_arr_rload(PROC_NET_FS "tcp"))) {
            throwAprException(_E, apr_get_os_error());
            goto cleanup;
        }
    }
    else {
        if (!(e->tnet = sight_arr_rload(PROC_NET_FS "tcp6"))) {
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
SIGHT_EXPORT_DECLARE(jint, TcpStatistics, enum1)(SIGHT_STDARGS,
                                                 jlong handle)
{
    tcpconn_enum_t *e = J2P(handle, tcpconn_enum_t *);

    UNREFERENCED_STDARGS;
    if (e && e->tnet) {
        e->idx = 1;
        return e->tnet->siz - 1; /* Skip description field */
    }
    else
        return 0;
}

SIGHT_EXPORT_DECLARE(void, TcpStatistics, enum2)(SIGHT_STDARGS,
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
    struct stat sb;

    tcpconn_enum_t *e = J2P(handle, tcpconn_enum_t *);

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
    }
    /* TODO: See what's the actual value of timelen */
    tmo = timelen;
    sight_tcpconn_set_tmo(_E, conn, tmo);
    if (!sight_stat_by_inode(inode, uid, &sb, &pid)) {
        apr_time_t ctime;
        apr_time_ansi_put(&ctime, sb.st_ctime);
        sight_tcpconn_set_cts(_E, conn, apr_time_as_msec(ctime));
    }
    sight_tcpconn_set_pid(_E, conn, pid);
    sight_tcpconn_set_state(_E, conn, tcp2sstate[state & 0xFF]);

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
    /* Increment the index counter */
    e->idx++;
}

/* Close TCP conn enumeration */
SIGHT_EXPORT_DECLARE(void, TcpStatistics, enum3)(SIGHT_STDARGS,
                                                 jlong handle)
{
    tcpconn_enum_t *e = J2P(handle, tcpconn_enum_t *);

    UNREFERENCED_STDARGS;
    if (e) {
        sight_arr_free(e->tnet);
        free(e);
    }
}
