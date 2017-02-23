/*
 *  Advertise (test for multicast)
 *
 *  Copyright(c) 2009 Red Hat Middleware, LLC,
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
 * @author Jean-Frederic Clere
 * @author Michal Karm Babacek
 */

#if !defined(PATH_MAX)
#define PATH_MAX 4096
#endif

#include <stdlib.h>
#include "apr_getopt.h"
#include "apr_network_io.h"
#include "apr_strings.h"
#include "apr_thread_proc.h"

/**
 * Multicast Time to Live (ttl) for the advertise transmission.
 */
#define MA_ADVERTISE_HOPS        10

#define INITIAL_APR_THREAD_SLEEP 100000
#define APR_SLEEP_AFTER_DETACH   150000

#define MA_MGROUP_SA_HOSTNAME_DESCRIPTION "UDP Multicast address to send datagrams to. Value: "
#define MA_MGROUP_SA_HOSTNAME "224.0.1.105"
#define MA_MGROUP_SA_PORT_DESCRIPTION "UDP Multicast port. Value: "
#define MA_MGROUP_SA_PORT 23364
#define MA_MGROUP_SA_INTERFACE_DESCRIPTION "IP address of the NIC to bound to. Value: "
#define MA_MGROUP_SA_INTERFACE "NULL"

#define STR(X) #X
#define CONCAT_STR_STR(first, second) first second
#define CONCAT_STR_INT(first, second) first STR(second)

static apr_sockaddr_t *ma_mgroup_sa;
static apr_socket_t *ma_mgroup_socket;

static const apr_getopt_option_t opt_option[] = {
        {"udpaddress", 'a', TRUE, CONCAT_STR_STR(MA_MGROUP_SA_HOSTNAME_DESCRIPTION, MA_MGROUP_SA_HOSTNAME)},
        {"udpport",    'p', TRUE, CONCAT_STR_INT(MA_MGROUP_SA_PORT_DESCRIPTION, MA_MGROUP_SA_PORT)},
        {"nicaddress", 'n', TRUE, CONCAT_STR_STR(MA_MGROUP_SA_INTERFACE_DESCRIPTION, MA_MGROUP_SA_INTERFACE)},
        {"help",       'h', FALSE, "show help"},
        {NULL,         0, 0,      NULL},
};

static void *APR_THREAD_FUNC parent_thread(apr_thread_t *thd, void *data) {
    char buf[20];
    apr_size_t n;
    char *s;
    apr_status_t rv;
    apr_pool_t *pool = data;

    apr_sleep(APR_TIME_C(INITIAL_APR_THREAD_SLEEP));
    s = apr_psprintf(pool, "to %pI", ma_mgroup_sa);
    printf("apr_socket_sendto %s\n", s);

    char rbuf[APR_RFC822_DATE_LEN + 1];
    apr_rfc822_date(rbuf, apr_time_now());

    n = (apr_size_t) apr_snprintf(buf, (apr_size_t) 20 + APR_RFC822_DATE_LEN + 1, "Advertize !!! %s\n", rbuf);
    rv = apr_socket_sendto(ma_mgroup_socket, ma_mgroup_sa, 0, buf, &n);
    if (rv != APR_SUCCESS) {
        printf("apr_socket_sendto failed %d %s\n", (int) n, apr_strerror(rv, buf, 20));
        return 0;
    }
    return 0;
}

static void print_usage() {
    printf("UDP Multicast Advertize Usage and Defaults:\n");
    for (int i = 0; i < sizeof(opt_option) / sizeof(apr_getopt_option_t) - 1; i++) {
        printf("\t--%s\t-%c\t%s\n", opt_option[i].name, opt_option[i].optch, opt_option[i].description);
    }
}

int main(int argc, const char **argv) {
    apr_pool_t *pool;
    apr_status_t rv;
    apr_sockaddr_t *ma_listen_sa;
    apr_sockaddr_t *ma_niface_sa;
    apr_thread_t *tp;
    apr_getopt_t *opt;
    int optch;
    const char *optarg;

    const char *ma_mgroup_sa_hostname = MA_MGROUP_SA_HOSTNAME;
    apr_port_t ma_mgroup_sa_port = MA_MGROUP_SA_PORT;
    const char *ma_mgroup_sa_interface = MA_MGROUP_SA_INTERFACE;

    apr_initialize();
    atexit(apr_terminate);

    apr_pool_create(&pool, NULL);

    apr_getopt_init(&opt, pool, argc, argv);

    while ((rv = apr_getopt_long(opt, opt_option, &optch, &optarg)) == APR_SUCCESS) {
        switch (optch) {
            case 'a':
                ma_mgroup_sa_hostname = optarg;
                break;
            case 'p':
                ma_mgroup_sa_port = (apr_port_t) atoi(optarg);
                break;
            case 'n':
                ma_mgroup_sa_interface = optarg;
                break;
            case 'h':
                print_usage();
                return 0;
            default:
                print_usage();
                return 1;
        }
    }
    if (rv != APR_EOF) {
        printf("Wrong arguments.\n");
        print_usage();
        return 1;
    }
    printf(CONCAT_STR_STR(MA_MGROUP_SA_HOSTNAME_DESCRIPTION, "%s\n"), ma_mgroup_sa_hostname);
    printf(CONCAT_STR_STR(MA_MGROUP_SA_PORT_DESCRIPTION, "%d\n"), ma_mgroup_sa_port);
    printf(CONCAT_STR_STR(MA_MGROUP_SA_INTERFACE_DESCRIPTION, "%s\n"), ma_mgroup_sa_interface);

    /**
     * mod_advertise code uses NULL as its default value that supposedly translates into: 0.0.0.0:0
     */
    if (strcmp("NULL", ma_mgroup_sa_interface) == 0) {
        ma_mgroup_sa_interface = NULL;
    }

    rv = apr_sockaddr_info_get(&ma_mgroup_sa, ma_mgroup_sa_hostname, APR_INET, ma_mgroup_sa_port, APR_UNSPEC, pool);
    if (rv != APR_SUCCESS) {
        printf("apr_sockaddr_info_get failed %d\n", rv);
        return 1;
    }

    rv = apr_sockaddr_info_get(&ma_listen_sa, ma_mgroup_sa_interface, ma_mgroup_sa->family, 0, APR_UNSPEC, pool);
    if (rv != APR_SUCCESS) {
        printf("apr_sockaddr_info_get(NULL) failed %d\n", rv);
        return 1;
    }

    rv = apr_sockaddr_info_get(&ma_niface_sa, ma_mgroup_sa_interface, ma_mgroup_sa->family, 0, APR_UNSPEC, pool);
    if (rv != APR_SUCCESS) {
        printf("apr_sockaddr_info_get(NULL) failed %d\n", rv);
        return 1;
    }

    rv = apr_socket_create(&ma_mgroup_socket, ma_mgroup_sa->family, SOCK_DGRAM, APR_PROTO_UDP, pool);
    if (rv != APR_SUCCESS) {
        printf("apr_socket_create failed %d\n", rv);
        return 1;
    }

    rv = apr_socket_opt_set(ma_mgroup_socket, APR_SO_REUSEADDR, 1);
    if (rv != APR_SUCCESS) {
        printf("apr_socket_opt_set failed %d\n", rv);
        return rv;
    }

    rv = apr_socket_bind(ma_mgroup_socket, ma_listen_sa);
    if (rv != APR_SUCCESS) {
        printf("apr_socket_bind failed %d\n", rv);
        return 1;
    }


    char *s;
    s = apr_psprintf(pool, "on %pI", ma_listen_sa);
    printf("apr_socket_bind %s\n", s);
    s = apr_psprintf(pool, "on %pI", ma_niface_sa);
    printf("apr_mcast_join %s\n", s);

    rv = apr_mcast_join(ma_mgroup_socket, ma_mgroup_sa, ma_niface_sa, NULL);
    if (rv != APR_SUCCESS) {
        printf("apr_mcast_join failed %d\n", rv);
        return 1;
    }

    /*  Expected on lo
    rv = apr_mcast_loopback(ma_mgroup_socket, 1);
    if (rv != APR_SUCCESS) {
        printf("apr_mcast_loopback failed %d\n", rv);
        return 1;
    }
    */

    rv = apr_mcast_hops(ma_mgroup_socket, MA_ADVERTISE_HOPS);
    if (rv != APR_SUCCESS) {
        printf("apr_mcast_hops failed %d\n", rv);
        apr_mcast_leave(ma_mgroup_socket, ma_mgroup_sa, NULL, NULL);
        apr_socket_close(ma_mgroup_socket);
        return 1;
    }

    rv = apr_thread_create(&tp, NULL, parent_thread, pool, pool);
    if (rv != APR_SUCCESS) {
        printf("apr_thread_create failed %d\n", rv);
        apr_mcast_leave(ma_mgroup_socket, ma_mgroup_sa, NULL, NULL);
        apr_socket_close(ma_mgroup_socket);
        return 1;
    }

    apr_thread_detach(tp);
    apr_sleep(APR_TIME_C(APR_SLEEP_AFTER_DETACH));
    apr_mcast_leave(ma_mgroup_socket, ma_mgroup_sa, NULL, NULL);
    apr_socket_close(ma_mgroup_socket);
    return 0;
}
