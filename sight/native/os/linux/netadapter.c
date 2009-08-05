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

#include <net/if.h>
#include <net/if_arp.h>
#include <net/route.h>
#include <sys/ioctl.h>

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

#define PROC_INET6_FILE     "/proc/net/if_inet6"
#define PROC_NET_DEV        "/proc/net/dev"
#define PROC_NET_ROUTE4     "/proc/net/route"
#define PROC_NET_ROUTE6     "/proc/net/ipv6_route"

typedef struct net_ifc_addr_t {
    char            ip4a[64];
    char            ip6a[64];
    char            mask[64];
    char            badr[64];
    int             virt;
    int             route;
} net_ifc_addr_t;

typedef struct net_ifc_data_t {
    int             type;
    int             index;
    int             flags;
    int             mtu;
    char            maca[64];
    cache_table_t  *addr;
} net_ifc_data_t;

typedef struct net_adapter_enum_t {
    int                   sd;
    int                   idx;
    cache_table_t         *ifc;
} net_adapter_enum_t;


static void c_destroy(const char *key, void *data)
{
    net_ifc_data_t *d = (net_ifc_data_t *)data;
    if (d) {
        cache_free(d->addr, NULL);
        free(d);
    }
}

static char *hex_base = "0123456789abcdefx";

static void make_mac(void *data, char *dest)
{
    int i, j = 0;
    unsigned char buf[IFHWADDRLEN];
    char *sp = dest;

    memcpy(buf, data, IFHWADDRLEN);
    *dest = '\0';
    for (i = 0; i < IFHWADDRLEN; i++) {
        if (buf[i]) {
            j = 1;
            break;
        }
    }
    if (!j)
        return;
    for (i = 0; i < IFHWADDRLEN - 1; i++) {
        *sp++ = hex_base[buf[i] >> 4];
        *sp++ = hex_base[buf[i] & 0x0f];
        *sp++ = ':';
    }
    *sp++ = hex_base[buf[i] >> 4];
    *sp++ = hex_base[buf[i] & 0x0f];
    *sp++ = '\0';
}

static void do_ifrec(net_ifc_data_t *id, net_ifc_addr_t *ca,
                     int sd, const char *name)
{
    struct ifreq        ifi;
    struct sockaddr_in *sa;

    memset(&ifi, 0, sizeof(struct ifreq));
    strcpy(ifi.ifr_name, name);

    if (!ca->virt) {
        if (!ioctl(sd, SIOCGIFINDEX, (char *)&ifi))
            id->index = ifi.ifr_ifindex;
        else
            id->index = -1;
        if (!ioctl(sd, SIOCGIFFLAGS, (char *)&ifi))
            id->flags = ifi.ifr_flags;
        /* Can we have IPV4 without MAC address ? */
        if (!ioctl(sd, SIOCGIFHWADDR, (char *)&ifi))
            make_mac(ifi.ifr_hwaddr.sa_data, id->maca);
        if (!ioctl(sd, SIOCGIFMTU, (char *)&ifi))
            id->mtu = ifi.ifr_mtu;
    }

    if (!ioctl(sd, SIOCGIFNETMASK, (char *)&ifi)) {
        sa = (struct sockaddr_in *)&ifi.ifr_netmask;
        inet_ntop(AF_INET, &sa->sin_addr, ca->mask, 64);
    }

    if ((id->flags & IFF_BROADCAST) &&
        !ioctl(sd, SIOCGIFBRDADDR, (char *)&ifi)) {
        sa = (struct sockaddr_in *)&ifi.ifr_broadaddr;
        inet_ntop(AF_INET, &sa->sin_addr, ca->badr, 64);
    }
}

net_ifc_data_t *new_ifc_addr(JNIEnv *_E, const char *name,
                             net_ifc_addr_t **ca)
{
    net_ifc_data_t *id;
    cache_entry_t  *ie;

    if (!(id = (net_ifc_data_t *)sight_calloc(_E,
                                              sizeof(net_ifc_data_t),
                                              THROW_FMARK))) {
        return NULL;
    }
    if (!(id->addr = cache_new(4))) {
        throwAprMemoryException(_E, THROW_FMARK,
                                apr_get_os_error());
        free(id);
        return NULL;
    }
    ie = cache_add(id->addr, name);
    if (!(*ca = (net_ifc_addr_t *)sight_calloc(_E,
                                               sizeof(net_ifc_addr_t),
                                               THROW_FMARK))) {
        cache_free(id->addr, NULL);
        return NULL;
    }
    ie->data = *ca;
    return id;
}

net_ifc_addr_t *get_ifc_addr(JNIEnv *_E, const char *name,
                             net_ifc_data_t *id)
{
    cache_entry_t  *ie;
    net_ifc_addr_t *ca;

    ie = cache_add(id->addr, name);
    if (ie->data)
        return ie->data;
    if (!(ca = (net_ifc_addr_t *)sight_calloc(_E,
                                              sizeof(net_ifc_addr_t),
                                              THROW_FMARK))) {
        return NULL;
    }
    ie->data = ca;
    return ca;
}

static int get_ipv4_route(JNIEnv *_E, const char *name,
                          net_ifc_data_t *id)
{
    sight_arr_t *i4r;
    net_ifc_addr_t *ca;
    int i;
    
    if (!id)
        return 0; /* No rec, nothing to do */
    if ((i4r = sight_arr_rload(PROC_NET_ROUTE4)) != NULL) {
        for (i = 1; i < i4r->siz; i++) {
            char iface[16], nname[64];
            char gatea[128], neta[128], maska[128];
            struct  in_addr ipg, ipn, ipm;
            int al, iflags, metric, refcnt, use, mss, window, irtt;

            al = sscanf(i4r->arr[i],
                        "%16s %128s %128s %X %d %d %d %128s %d %d %d",
                        iface, neta, gatea, &iflags, &refcnt, &use, &metric,
                        maska, &mss, &window, &irtt);
            if (al < 10 || !(iflags & RTF_GATEWAY))
                continue;
            if (strcmp(iface, name))
                continue;
            sprintf(nname, "%s.r4.%d", name, i);
            /* TODO: Deal with RTF_HOST flags */
            if (!(ca = get_ifc_addr(_E, nname, id))) {
                sight_arr_free(i4r);
                return 1;
            }
            if (ca->route)
                continue;
            ca->route = 1;

            if (sscanf(gatea, "%X", &ipg.s_addr) != 1)
                continue;
            if (sscanf(neta,  "%X", &ipn.s_addr) != 1)
                continue;
            if (sscanf(maska, "%X", &ipm.s_addr) != 1)
                continue;
            inet_ntop(AF_INET, &ipg, ca->ip4a, 64);
            inet_ntop(AF_INET, &ipm, ca->mask, 64);
        }
    }
    sight_arr_free(i4r);
    return 0;
}

static int get_ipv6_route(JNIEnv *_E, const char *name,
                          net_ifc_data_t *id)
{
    sight_arr_t *i6r;
    net_ifc_addr_t *ca;
    int i;
    
    if (!id)
        return 0; /* No rec, nothing to do */
    if ((i6r = sight_arr_rload(PROC_NET_ROUTE6)) != NULL) {
        for (i = 0; i < i6r->siz; i++) {
            char iface[16], nname[64];
            char dsta[128], neta[128], hopa[128];
            struct in6_addr ipd, ipn, iph;

            int al, iflags, metric, refcnt, use, plen, slen;

            al = sscanf(i6r->arr[i],
                        "%64s %02X %64s %02X %64s %08X %08X %08X %08X %s",
                        dsta, &plen, neta, &slen, hopa, &metric, &use,
                        &refcnt, &iflags, iface);
            if (al < 10 || !(iflags & RTF_GATEWAY))
                continue;
            if (strcmp(iface, name))
                continue;
            sprintf(nname, "%s.r6.%d", name, i);
            /* TODO: Deal with RTF_HOST flags */
            if (!(ca = get_ifc_addr(_E, nname, id))) {
                sight_arr_free(i6r);
                return 1;
            }
            if (ca->route)
                continue;
            ca->route = 1;
            /* XXX: Is it source network or next hop ? */
            sight_hex2bin(neta, ipd.s6_addr, 16);
            sight_inet_ntop6(ipd.s6_addr, ca->ip6a, 64);
        }
    }
    sight_arr_free(i6r);
    return 0;
}

SIGHT_EXPORT_DECLARE(jlong, NetworkAdapter, enum0)(SIGHT_STDARGS,
                                                   jlong pool)
{
    net_adapter_enum_t *e;
    int i, j;
    struct ifconf  ifc;
    struct ifreq  *ifr;
    sight_arr_t   *i6a;
    sight_arr_t   *i4a;
    net_ifc_data_t *id;

    UNREFERENCED_O;
    UNREFERENCED(pool);
    if (!(e = (net_adapter_enum_t *)sight_calloc(_E,
                                                 sizeof(net_adapter_enum_t),
                                                 THROW_FMARK))) {
        return 0;
    }
    if (!(e->ifc = cache_new(8))) {
        throwAprMemoryException(_E, THROW_FMARK,
                                apr_get_os_error());
        free(e);
        return 0;
    }
    if ((e->sd = socket(AF_INET, SOCK_DGRAM, 0)) < 0) {
        throwAprException(_E, apr_get_os_error());
        goto cleanup;
    }

    ifc.ifc_buf = NULL;
    if (ioctl(e->sd, SIOCGIFCONF, (char *)&ifc) < 0) {
        throwAprException(_E, apr_get_os_error());
        goto cleanup;
    }
    if (!(ifc.ifc_buf = sight_calloc(_E, ifc.ifc_len, THROW_FMARK))) {
        goto cleanup;
    }
    if (ioctl(e->sd, SIOCGIFCONF, (char *)&ifc) < 0) {
        throwAprException(_E, apr_get_os_error());
        free(ifc.ifc_buf);
        goto cleanup;
    }
    /* Add interfaces to the cache */
    ifr = ifc.ifc_req;
    for (i = 0; i < ifc.ifc_len; i += sizeof(struct ifreq), ifr++) {
        cache_entry_t  *ce;
        char *p, las[128] = "";
        char pname[IF_NAMESIZE];
        struct sockaddr_in *sa;
        int is_virtual = 0;

        sa = (struct sockaddr_in *)&ifr->ifr_addr;
        inet_ntop(AF_INET, &sa->sin_addr, las, 64);
        strcpy(pname, ifr->ifr_name);
        if ((p = strchr(pname, ':'))) {
            /* This is an virtual adapter.
             * Remove everything after colon
             */
            *p = '\0';
            is_virtual = 1;
        }
        ce = cache_add(e->ifc, pname);
        if (!(id = (net_ifc_data_t *)ce->data)) {
            net_ifc_addr_t *ca;

            if (!(id = new_ifc_addr(_E, ifr->ifr_name, &ca))) {
                free(ifc.ifc_buf);
                goto cleanup;
            }
            ce->data = id;
            id->type = AF_INET;
            ca->virt = is_virtual;
            strcpy(ca->ip4a, las);
            do_ifrec(id, ca, e->sd, ifr->ifr_name);
        }
        else {
            net_ifc_addr_t *ca;

            if (!(ca = get_ifc_addr(_E, ifr->ifr_name, id))) {
                free(ifc.ifc_buf);
                goto cleanup;
            }
            ca->virt = is_virtual;
            strcpy(ca->ip4a, las);
            do_ifrec(id, ca, e->sd, ifr->ifr_name);
        }
        get_ipv4_route(_E, ifr->ifr_name, id);
    }

    free(ifc.ifc_buf);
    /* Add missing interfaces from /proc/net/dev */
    if ((i4a = sight_arr_rload(PROC_NET_DEV)) != NULL) {
        for (i = 2; i < i4a->siz; i++) {
            cache_entry_t  *ce;
            char *pname;
            if (!(pname = strchr(i4a->arr[i], ':')))
                continue;
            *pname = '\0';
            ce = cache_add(e->ifc, i4a->arr[i]);
            if (!(id = (net_ifc_data_t *)ce->data)) {
                net_ifc_addr_t *ca;

                if (!(id = new_ifc_addr(_E, i4a->arr[i], &ca))) {
                    sight_arr_free(i4a);
                    goto cleanup;
                }
                ce->data = id;
                id->type = AF_INET;
                do_ifrec(id, ca, e->sd, i4a->arr[i]);
                get_ipv4_route(_E, i4a->arr[i], id);
            }
        }
        sight_arr_free(i4a);
    }
    /* Now add IPV6 entries */
    if ((i6a = sight_arr_rload(PROC_INET6_FILE)) != NULL) {
        for (i = 0; i < i6a->siz; i++) {
            cache_entry_t  *ce;
            struct ifreq ifi;
            int al, plen, scope, dads, ifidx;
            char ias[64];
            char las[128] = "";
            struct in6_addr in6;
            al = sscanf(i6a->arr[i],
                        "%32s %02x %02x %02x %02x %16s",
                        ias, &ifidx, &plen, &scope, &dads, ifi.ifr_name);
            if (al != 6)
                continue;
            sight_hex2bin(ias, in6.s6_addr, 16);
            sight_inet_ntop6(in6.s6_addr, las, 64);
            ce = cache_add(e->ifc, ifi.ifr_name);
            if (plen) {
                char buf[32];
                sprintf(buf, "/%d", plen);
                strcat(las, buf);
            }
            if (!(id = (net_ifc_data_t *)ce->data)) {
                net_ifc_addr_t *ca;

                if (!(id = new_ifc_addr(_E, ifi.ifr_name, &ca))) {
                    sight_arr_free(i6a);
                    goto cleanup;
                }
                ce->data = id;
                strcpy(ca->ip6a, las);

                if (!ioctl(e->sd, SIOCGIFHWADDR, (char *)&ifi)) {
                    make_mac(ifi.ifr_hwaddr.sa_data, las);
                    strcpy(id->maca, las);
                }
                if (!ioctl(e->sd, SIOCGIFMTU, (char *)&ifi))
                    id->mtu = ifi.ifr_mtu;
                id->type  = AF_INET6;
                id->index = ifidx;
                id->flags = IFF_UP;
            }
            else {
                net_ifc_addr_t *ca;

                if (!(ca = get_ifc_addr(_E, ifi.ifr_name, id))) {
                    sight_arr_free(i6a);
                    goto cleanup;
                }
                strcpy(ca->ip6a, las);
            }
            get_ipv6_route(_E, ifi.ifr_name, id);
        }
        sight_arr_free(i6a);
    }
    return P2J(e);

cleanup:
    if (e->sd >= 0)
        close(e->sd);
    cache_free(e->ifc, c_destroy);
    free(e);
    return 0;
}

SIGHT_EXPORT_DECLARE(jint, NetworkAdapter, enum1)(SIGHT_STDARGS,
                                                  jlong handle)
{
    net_adapter_enum_t *e = J2P(handle, net_adapter_enum_t *);

    UNREFERENCED_O;
    if (!e)
        return 0;
    else {
        e->idx = 0;
        return e->ifc->siz;
    }
}

SIGHT_EXPORT_DECLARE(void, NetworkAdapter, enum2)(SIGHT_STDARGS,
                                                  jobject thiz,
                                                  jlong handle)
{
    net_adapter_enum_t *e = J2P(handle, net_adapter_enum_t *);
    net_ifc_data_t *id;
    net_ifc_addr_t *ca;
    jobject addr;
    jobjectArray aarr;
    jint len, idx;
    int i;

    UNREFERENCED_O;
    if (!e || e->idx > e->ifc->siz)
        return;
    id = (net_ifc_data_t *)e->ifc->list[e->idx]->data;
    SET_IFIELD_S(0000, thiz, e->ifc->list[e->idx]->key);
    /* TODO: Figure out the Description */
    SET_IFIELD_S(0001, thiz, e->ifc->list[e->idx]->key);
    SET_IFIELD_S(0002, thiz, e->ifc->list[e->idx]->key);

    if (id->maca[0]) {
        addr = sight_new_netaddr_class(_E, _O);
        if (!addr || (*_E)->ExceptionCheck(_E))
            return;
        sight_netaddr_set_addr(_E, addr, id->maca);
        sight_netaddr_set_family(_E, addr, AF_HARDWARE);
        SET_IFIELD_O(0003, thiz, addr);
        (*_E)->DeleteLocalRef(_E, addr);
    }
    /* Set adapter type */
    if (id->flags & IFF_LOOPBACK) {
        CALL_METHOD1(0000, thiz, IF_TYPE_SOFTWARE_LOOPBACK);
    }
    else if (id->flags & IFF_POINTOPOINT) {
        CALL_METHOD1(0000, thiz, IF_TYPE_PPP);
    }
    else {
        CALL_METHOD1(0000, thiz, IF_TYPE_ETHERNET_CSMACD);
    }
    /* Set adapter status */
    if (id->flags & IFF_UP) {
        if (id->flags & IFF_RUNNING)
            CALL_METHOD1(0001, thiz, SIGHT_IFO_UP);
        else
            CALL_METHOD1(0001, thiz, SIGHT_IFO_NOTPRESENT);
    }
    else {
        CALL_METHOD1(0001, thiz, SIGHT_IFO_DOWN);
    }
    /* Set IP addresses */
    len = 0;
    idx = 0;
    for (i = 0; i < id->addr->siz; i++) {
        ca = (net_ifc_addr_t *)id->addr->list[i]->data;
        if (ca->route)
            continue;
        if (ca->ip4a[0])
            len++;
        if (ca->ip6a[0])
            len++;
    }
    if (len) {
        aarr = sight_new_netaddr_array(_E, _O, len);
        if (!aarr || (*_E)->ExceptionCheck(_E))
            goto cleanup;
        for (i = 0; i < id->addr->siz; i++) {
            ca = (net_ifc_addr_t *)id->addr->list[i]->data;

            if (ca->route)
                continue;
            if (ca->ip4a[0]) {
                addr = sight_new_netaddr_class(_E, _O);
                if (!addr || (*_E)->ExceptionCheck(_E))
                    return;
                sight_netaddr_set_addr(_E, addr, ca->ip4a);
                sight_netaddr_set_family(_E, addr, AF_INET);
                if (ca->mask[0])
                    sight_netaddr_set_mask(_E, addr, ca->mask);
                (*_E)->SetObjectArrayElement(_E, aarr, idx++, addr);
                (*_E)->DeleteLocalRef(_E, addr);
            }
            if (ca->ip6a[0]) {
                addr = sight_new_netaddr_class(_E, _O);
                if (!addr || (*_E)->ExceptionCheck(_E))
                    return;
                sight_netaddr_set_addr(_E, addr, ca->ip6a);
                sight_netaddr_set_family(_E, addr, AF_INET6);
                (*_E)->SetObjectArrayElement(_E, aarr, idx++, addr);
                (*_E)->DeleteLocalRef(_E, addr);
            }
        }
        SET_IFIELD_O(0006, thiz, aarr);
        (*_E)->DeleteLocalRef(_E, aarr);
    }
    /* Set Gateway addresses */
    len = 0;
    idx = 0;
    for (i = 0; i < id->addr->siz; i++) {
        ca = (net_ifc_addr_t *)id->addr->list[i]->data;
        if (!ca->route)
            continue;
        if (ca->ip4a[0])
            len++;
        else if (ca->ip6a[0])
            len++;
    }
    if (len) {
        aarr = sight_new_netaddr_array(_E, _O, len);
        if (!aarr || (*_E)->ExceptionCheck(_E))
            goto cleanup;
        for (i = 0; i < id->addr->siz; i++) {
            ca = (net_ifc_addr_t *)id->addr->list[i]->data;

            if (!ca->route)
                continue;
            addr = sight_new_netaddr_class(_E, _O);
            if (!addr || (*_E)->ExceptionCheck(_E))
                return;
            if (ca->ip4a[0]) {
                sight_netaddr_set_addr(_E, addr, ca->ip4a);
                sight_netaddr_set_family(_E, addr, AF_INET);
                if (ca->mask[0])
                    sight_netaddr_set_mask(_E, addr, ca->mask);
            }
            else if (ca->ip6a[0]) {
                sight_netaddr_set_addr(_E, addr, ca->ip6a);
                sight_netaddr_set_family(_E, addr, AF_INET6);
            }
            (*_E)->SetObjectArrayElement(_E, aarr, idx++, addr);
            (*_E)->DeleteLocalRef(_E, addr);
        }
        SET_IFIELD_O(0008, thiz, aarr);
        (*_E)->DeleteLocalRef(_E, aarr);
    }

    SET_IFIELD_I(0009, thiz, id->mtu);

cleanup:
    e->idx++;
}

SIGHT_EXPORT_DECLARE(void, NetworkAdapter, enum3)(SIGHT_STDARGS,
                                                  jlong handle)
{
    net_adapter_enum_t *e = J2P(handle, net_adapter_enum_t *);

    UNREFERENCED_STDARGS;
    if (e) {
        close(e->sd);
        cache_free(e->ifc, c_destroy);
        free(e);
    }
}
